package fr.univrennes.istic.l2gen.application.core.table;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import org.duckdb.DuckDBConnection;

import fr.univrennes.istic.l2gen.application.core.config.Config;
import fr.univrennes.istic.l2gen.application.core.config.Log;
import fr.univrennes.istic.l2gen.application.core.filter.Filter;
import fr.univrennes.istic.l2gen.application.core.filter.FilterBuilder;
import fr.univrennes.istic.l2gen.application.core.services.FileService;
import fr.univrennes.istic.l2gen.application.core.services.ObjectService;

public final class DataTable {
    private static final int BLOCK_SIZE = 10_000;
    private static final int MAX_CACHED_BLOCKS = 8;

    private final LinkedHashMap<Integer, Object[][]> blockCache;
    private ExecutorService prefetchExecutor;

    private DuckDBConnection connection;

    private String tableName;
    private File tablePath;
    private String alias;

    private final List<String> columnNames;
    private final List<DataType> columnTypes;

    private long rowCount;
    private final long columnCount;
    private int blockCount;

    private final ArrayList<Filter> filters;

    public static DataTable of(ResultSet resultSet, File file, String alias) throws Exception {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();

        List<String> columnNames = new ArrayList<>(columnCount);
        List<DataType> columnTypes = new ArrayList<>(columnCount);

        for (int i = 1; i <= columnCount; i++) {
            columnNames.add(metaData.getColumnLabel(i));
            columnTypes.add(DataType.fromSQL(metaData.getColumnTypeName(i)));
        }

        long rowCount = 0;
        while (resultSet.next()) {
            rowCount++;
        }

        return new DataTable(file, alias, columnNames, columnTypes, rowCount, columnCount);
    }

    public static DataTable of(File file) throws IOException {
        if (!file.canRead() || !file.isFile() || file.length() == 0
                || !FileService.getExtension(file).equals("parquet")) {
            return null;
        }

        String tablePath = file.getAbsolutePath().replace("\\", "/");
        try (DuckDBConnection connection = (DuckDBConnection) DriverManager.getConnection("jdbc:duckdb:")) {
            try (Statement statement = connection.createStatement()) {

                ResultSet rowCountResult = statement.executeQuery(
                        String.format("SELECT COUNT(*) FROM '%s'", tablePath));
                rowCountResult.next();
                long rowCount = rowCountResult.getLong(1);

                ResultSet columnResult = statement.executeQuery(
                        String.format("DESCRIBE SELECT * FROM '%s'", tablePath));

                List<String> columnNames = new ArrayList<>();
                List<DataType> columnTypes = new ArrayList<>();

                long columnCount = 0;
                while (columnResult.next()) {
                    columnCount++;
                    columnNames.add(columnResult.getString("column_name"));
                    columnTypes.add(DataType.fromSQL(columnResult.getString("column_type")));
                }

                String alias = FileService.getBaseName(file);
                return new DataTable(file, alias, columnNames, columnTypes, rowCount, columnCount);
            }
        } catch (Exception e) {
            Log.debug("Failed to load table from file: " + file.getAbsolutePath(), e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public DataTable(File file, String alias, List<String> columnNames, List<DataType> columnTypes, long rowCount,
            long columnCount)
            throws IOException {
        this.tablePath = file;
        this.tableName = file.getAbsolutePath().replace("\\", "/");
        this.alias = alias;

        this.columnNames = columnNames;
        this.columnTypes = columnTypes;
        this.columnCount = columnCount;

        this.filters = Config.getBoolean("settings.table.filters.save_with_table",
                true) ? ObjectService
                        .load(new File(FileService.getAppDataDir(), "filters_" + alias + ".ser"), ArrayList.class)
                        .orElse(new ArrayList<>()) : new ArrayList<>();

        this.rowCount = rowCount;
        this.blockCount = (int) Math.ceil((double) rowCount / BLOCK_SIZE);
        this.blockCache = new LinkedHashMap<>(MAX_CACHED_BLOCKS, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, Object[][]> eldest) {
                return size() > MAX_CACHED_BLOCKS;
            }
        };

        this.prefetchExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "DataTable-Prefetch");
            thread.setDaemon(true);
            return thread;
        });

        try {
            this.connection = (DuckDBConnection) DriverManager.getConnection("jdbc:duckdb:");
        } catch (Exception e) {
            throw new IOException("Failed to open DuckDB connection", e);
        }

        for (int i = 0; i < columnNames.size(); i++) {
            if (columnTypes.get(i) != DataType.STRING && columnTypes.get(i) != DataType.EMPTY) {
                continue;
            }

            String query = String.format("SELECT COUNT(*) FROM '%s' WHERE %s IS NOT NULL",
                    tableName, getSQLColumnName(i));

            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);
                resultSet.next();
                long nonNullCount = resultSet.getLong(1);
                if (nonNullCount == 0) {
                    columnTypes.set(i, DataType.EMPTY);
                }
            } catch (Exception e) {
                Log.debug("Failed to check if column is empty for column: " + columnNames.get(i), e);
            }
        }

        refreshRowCount();
    }

    public File getPath() {
        return tablePath;
    }

    public String getSQLName() {
        return "\'" + tableName.replace("'", "''") + "\'";
    }

    public String getSQLColumnName(int columnIndex) {
        if (columnIndex < 0 || columnIndex >= columnNames.size()) {
            return "";
        }
        return "\"" + getColumnName(columnIndex).replace("\"", "\"\"") + "\"";
    }

    public String getColumnName(int columnIndex) {
        if (columnIndex < 0 || columnIndex >= columnNames.size()) {
            return "";
        }
        return columnNames.get(columnIndex);
    }

    public List<String> getColumnNames() {
        return columnNames;
    }

    public DataType getColumnType(int columnIndex) {
        if (columnIndex < 0 || columnIndex >= columnTypes.size()) {
            return DataType.STRING;
        }
        return columnTypes.get(columnIndex);
    }

    public boolean setColumnType(int columnIndex, DataType newType) {
        if (columnIndex < 0 || columnIndex >= columnTypes.size()) {
            return false;
        }

        String castType = newType.toSQL();

        File tempFile = new File(tablePath.getAbsolutePath() + ".tmp.parquet");
        String tempName = tempFile.getAbsolutePath().replace("\\", "/");

        StringBuilder select = new StringBuilder("SELECT ");
        for (int i = 0; i < columnNames.size(); i++) {
            String name = getSQLColumnName(i);

            if (i == columnIndex) {
                select.append(String.format("TRY_CAST(%s AS %s) AS %s", name, castType, name));
            } else {
                select.append(name);
            }

            if (i < columnNames.size() - 1) {
                select.append(", ");
            }
        }

        String query = String.format(
                "COPY (%s FROM '%s') TO '%s' (FORMAT PARQUET, CODEC 'SNAPPY')",
                select.toString(),
                tableName,
                tempName);

        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:");
                Statement stmt = conn.createStatement()) {

            stmt.execute(query);

            tablePath.delete();
            tempFile.renameTo(tablePath);

            columnTypes.set(columnIndex, newType);

            synchronized (blockCache) {
                blockCache.clear();
            }

            return true;
        } catch (Exception e) {
            Log.debug("Failed to change column type, reverting to original", e);
            tempFile.delete();
            return false;
        }
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
        File newPath = new File(tablePath.getParentFile(), alias + ".parquet");
        try {
            if (tablePath.renameTo(newPath)) {
                tablePath = newPath;
                tableName = newPath.getAbsolutePath().replace("\\", "/");
            } else {
                Log.debug("Failed to rename file for alias change");
            }
        } catch (Exception e) {
            Log.debug("Failed to rename file for alias change", e);
        }
    }

    public long getRowCount() {
        return rowCount;
    }

    public long getColumnCount() {
        return columnCount;
    }

    public int getBlockCount() {
        return blockCount;
    }

    public long getBlockStartRow(int blockIndex) {
        return (long) blockIndex * BLOCK_SIZE;
    }

    public long getBlockRowCount(int blockIndex) {
        long start = getBlockStartRow(blockIndex);
        long remaining = getRowCount() - start;
        return Math.min(remaining, BLOCK_SIZE);
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= getRowCount()) {
            return null;
        }
        int blockIndex = rowIndex / BLOCK_SIZE;
        int rowWithinBlock = rowIndex % BLOCK_SIZE;

        Object[][] blockData = getBlock(blockIndex);
        if (blockData == null || rowWithinBlock >= blockData.length) {
            return null;
        }
        if (columnIndex < 0 || columnIndex >= blockData[rowWithinBlock].length) {
            return null;
        }
        return blockData[rowWithinBlock][columnIndex];
    }

    public boolean setValueAt(int rowIndex, int columnIndex, Object value) {
        if (rowIndex < 0 || rowIndex >= getRowCount()) {
            return false;
        }
        if (columnIndex < 0 || columnIndex >= columnNames.size()) {
            return false;
        }
        if (isClosed() && !open()) {
            return false;
        }

        long targetRowId;
        String baseQuery = "SELECT row_number() OVER () AS __row_id, * FROM " + getSQLName();
        String filterClause = buildFilterClause(true);
        String rowIdQuery = "SELECT __row_id FROM (" + baseQuery + ") base" + filterClause
                + " LIMIT 1 OFFSET " + rowIndex;

        synchronized (connection) {
            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(rowIdQuery);
                if (!resultSet.next()) {
                    return false;
                }
                targetRowId = resultSet.getLong(1);
            } catch (Exception e) {
                Log.debug("Failed to resolve target row for update", e);
                return false;
            }
        }

        String valueExpression = buildValueExpression(columnTypes.get(columnIndex), value);
        StringBuilder select = new StringBuilder("SELECT ");
        for (int i = 0; i < columnNames.size(); i++) {
            String columnName = getSQLColumnName(i);
            if (i == columnIndex) {
                select.append("CASE WHEN __row_id = ").append(targetRowId)
                        .append(" THEN ").append(valueExpression)
                        .append(" ELSE ").append(columnName)
                        .append(" END AS ").append(columnName);
            } else {
                select.append(columnName);
            }

            if (i < columnNames.size() - 1) {
                select.append(", ");
            }
        }
        select.append(" FROM (").append(baseQuery).append(") base");

        File tempFile = new File(tablePath.getAbsolutePath() + ".tmp.parquet");
        String tempName = tempFile.getAbsolutePath().replace("\\", "/");

        String query = String.format(
                "COPY (%s) TO '%s' (FORMAT PARQUET, CODEC 'SNAPPY')",
                select.toString(),
                tempName);

        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:");
                Statement stmt = conn.createStatement()) {

            stmt.execute(query);

            tablePath.delete();
            tempFile.renameTo(tablePath);

            invalidateCache();
            return true;
        } catch (Exception e) {
            Log.debug("Failed to update parquet file", e);
            tempFile.delete();
            return false;
        }
    }

    private String buildFilterClause(boolean orderBy) {
        String query = FilterBuilder.base("SELECT * FROM", this, orderBy).toString();
        String prefix = "SELECT * FROM " + getSQLName();
        if (query.startsWith(prefix)) {
            return query.substring(prefix.length());
        }
        return "";
    }

    private String buildValueExpression(DataType type, Object value) {
        if (value == null) {
            return "NULL";
        }

        String rawValue = value.toString();
        if ("(null)".equals(rawValue)) {
            return "NULL";
        }

        String escaped = rawValue.replace("'", "''");
        String literal = "'" + escaped + "'";

        return switch (type) {
            case INTEGER -> {
                if (rawValue.isBlank()) {
                    yield "NULL";
                }
                yield "TRY_CAST(" + literal + " AS INTEGER)";
            }
            case DOUBLE -> {
                if (rawValue.isBlank()) {
                    yield "NULL";
                }
                yield "TRY_CAST(REPLACE(" + literal + ", ',', '.') AS DOUBLE)";
            }
            case BOOLEAN -> {
                if (rawValue.isBlank()) {
                    yield "NULL";
                }
                yield "TRY_CAST(" + literal + " AS BOOLEAN)";
            }
            case DATE -> {
                if (value instanceof java.util.Date dateValue) {
                    String dateLiteral = "'" + new java.sql.Date(dateValue.getTime()) + "'";
                    yield "TRY_CAST(" + dateLiteral + " AS DATE)";
                }
                if (rawValue.isBlank()) {
                    yield "NULL";
                }
                yield "TRY_CAST(" + literal + " AS DATE)";
            }
            case STRING, EMPTY -> literal;
        };
    }

    public void prefetch(int startRow, int endRow) {
        if (prefetchExecutor.isShutdown() || prefetchExecutor.isTerminated()) {
            return;
        }
        int firstBlock = startRow / BLOCK_SIZE;
        int lastBlock = Math.max(0, (endRow - 1)) / BLOCK_SIZE;

        for (int blockIndex = firstBlock; blockIndex <= lastBlock; blockIndex++) {
            final int blockToFetch = blockIndex;
            synchronized (blockCache) {
                if (blockCache.containsKey(blockToFetch)) {
                    continue;
                }
            }
            try {
                prefetchExecutor.submit(() -> loadBlock(blockToFetch));
            } catch (RejectedExecutionException e) {
                Log.debug("Prefetch task rejected, likely due to shutdown", e);
                return;
            }
        }

        int nextBlock = lastBlock + 1;
        if (nextBlock < blockCount) {
            final int aheadBlock = nextBlock;
            try {
                prefetchExecutor.submit(() -> loadBlock(aheadBlock));
            } catch (RejectedExecutionException e) {
                Log.debug("Failed to submit prefetch task, likely due to shutdown", e);
                return;
            }
        }
    }

    public boolean open() {
        if (!this.isClosed()) {
            return true;
        }

        prefetchExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "DataTable-Prefetch");
            thread.setDaemon(true);
            return thread;
        });

        try {
            this.connection = (DuckDBConnection) DriverManager.getConnection("jdbc:duckdb:");
        } catch (Exception e) {
            Log.debug("Failed to open DataTable", e);
            return false;
        }

        return true;
    }

    public void close() {
        prefetchExecutor.shutdownNow();
        this.invalidateCache();
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (Exception e) {
            Log.debug("Failed to close DataTable", e);
        }
    }

    public boolean isClosed() {
        if (prefetchExecutor.isShutdown() || prefetchExecutor.isTerminated()) {
            return true;
        }

        try {
            return connection == null || connection.isClosed();
        } catch (Exception e) {
            Log.debug("Failed to check if DataTable is closed", e);
            return true;
        }
    }

    private void saveFilters() {
        if (Config.getBoolean("settings.table.filters.save_with_table",
                true)) {
            ObjectService.save(new File(FileService.getAppDataDir(), "filters_" + alias + ".ser"), filters);
        }
    }

    public void addFilters(List<Filter> newFilters) {
        filters.addAll(newFilters);

        saveFilters();
        invalidateCache();
        refreshRowCount();
    }

    public void addFilter(Filter filter) {
        filters.add(filter);

        saveFilters();
        invalidateCache();
        refreshRowCount();
    }

    public List<Filter> getColumnFilters(int columnIndex) {
        return filters.stream()
                .filter(f -> f.getColumnIndex() == columnIndex)
                .toList();
    }

    public List<Filter> getFilters() {
        return filters;
    }

    public void clearColumnFilter(int columnIndex) {
        filters.removeIf(f -> f.getColumnIndex() == columnIndex);

        saveFilters();
        invalidateCache();
        refreshRowCount();
    }

    public void clearFilters() {
        filters.clear();

        saveFilters();
        invalidateCache();
        refreshRowCount();
    }

    private void invalidateCache() {
        synchronized (blockCache) {
            blockCache.clear();
        }
    }

    private void refreshRowCount() {
        boolean hasRowLimitingFilters = filters.stream()
                .anyMatch(f -> f.hasRowLimitingEffect());

        if (!hasRowLimitingFilters) {
            try {
                refreshRowCountFromQuery();
            } catch (Exception e) {
                Log.debug("Failed to refresh row count", e);
            }
            return;
        }

        try {
            refreshRowCountFromQuery();
        } catch (Exception e) {
            Log.debug("Failed to refresh row count", e);
        }
    }

    private void refreshRowCountFromQuery() throws Exception {
        String countQuery = FilterBuilder.count(this);
        synchronized (connection) {
            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(countQuery);
                resultSet.next();
                this.rowCount = resultSet.getLong(1);
                this.blockCount = (int) Math.ceil((double) this.rowCount / BLOCK_SIZE);
            }
        }
    }

    private Object[][] getBlock(int blockIndex) {
        synchronized (blockCache) {
            if (blockCache.containsKey(blockIndex)) {
                return blockCache.get(blockIndex);
            }
        }
        return loadBlock(blockIndex);
    }

    private Object[][] loadBlock(int blockIndex) {
        synchronized (blockCache) {
            if (blockCache.containsKey(blockIndex)) {
                return blockCache.get(blockIndex);
            }
        }

        long offsetRow = getBlockStartRow(blockIndex);
        long limitCount = getBlockRowCount(blockIndex);

        String query = FilterBuilder.query(this, limitCount, offsetRow);
        try {
            Object[][] blockData;
            synchronized (connection) {
                try (Statement statement = connection.createStatement()) {
                    ResultSet resultSet = statement.executeQuery(query);
                    ResultSetMetaData metaData = resultSet.getMetaData();
                    int fetchedColumnCount = metaData.getColumnCount();

                    List<Object[]> rows = new ArrayList<>((int) limitCount);
                    while (resultSet.next()) {
                        Object[] row = new Object[fetchedColumnCount];
                        for (int col = 1; col <= fetchedColumnCount; col++) {
                            row[col - 1] = resultSet.getObject(col);
                        }
                        rows.add(row);
                    }
                    blockData = rows.toArray(new Object[0][]);
                }
            }

            synchronized (blockCache) {
                blockCache.put(blockIndex, blockData);
            }
            return blockData;

        } catch (Exception e) {
            return new Object[0][];
        }
    }

}