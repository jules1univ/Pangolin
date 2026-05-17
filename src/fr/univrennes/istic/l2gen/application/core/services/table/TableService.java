package fr.univrennes.istic.l2gen.application.core.services.table;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.duckdb.DuckDBConnection;

import fr.univrennes.istic.l2gen.application.Pangol1;
import fr.univrennes.istic.l2gen.application.core.TaskStatus;
import fr.univrennes.istic.l2gen.application.core.config.Config;
import fr.univrennes.istic.l2gen.application.core.config.Lang;
import fr.univrennes.istic.l2gen.application.core.config.Log;
import fr.univrennes.istic.l2gen.application.core.services.FileService;
import fr.univrennes.istic.l2gen.application.core.table.DataTable;
import fr.univrennes.istic.l2gen.application.core.table.DataType;

public final class TableService {
    private static final Map<File, DataTable> loaded = Collections.synchronizedMap(new HashMap<>());
    private static final Set<File> recents = new HashSet<>();
    private static final int MAX_RECENTS = 10;

    public static DataTable get(File file) {
        if (loaded.containsKey(file)) {
            return loaded.get(file);
        }

        List<DataTable> tables = load(file, file.getParentFile());
        if (!tables.isEmpty()) {
            DataTable table = tables.get(0);
            loaded.put(file, table);
            return table;
        }

        return null;
    }

    public static List<DataTable> get() {
        return loaded.values().stream().toList();
    }

    public static void add(DataTable table) {
        loaded.put(table.getPath(), table);
    }

    public static void remove(File file) {
        DataTable table = loaded.remove(file);
        if (table != null) {
            table.close();
        }
    }

    public static void update(File oldPath, File newPath, DataTable table) {
        if (oldPath != null) {
            loaded.remove(oldPath);
        }
        if (newPath != null && table != null) {
            loaded.put(newPath, table);
        }
    }

    public static List<DataTable> load(File file, File targetDir) {
        if (!file.exists()) {
            return List.of();
        }

        String taskId = Pangol1.getController().addTask(Lang.get("task.table.load", file.getName()),
                TaskStatus.PENDING);
        try {
            if (file.isDirectory()) {
                Pangol1.getController().updateTaskStatus(taskId, TaskStatus.RUNNING);
                List<DataTable> tables = processDirectory(file);
                Pangol1.getController().updateTaskStatus(taskId, TaskStatus.SUCCESS);
                return tables;
            }

            if (file.isFile() && file.canRead()) {
                String ext = FileService.getExtension(file);

                switch (ext) {
                    case "zip": {
                        Pangol1.getController().updateTaskStatus(taskId, TaskStatus.RUNNING);
                        List<DataTable> tables = processZip(file, targetDir);
                        Pangol1.getController().updateTaskStatus(taskId, TaskStatus.SUCCESS);
                        return tables;
                    }
                    case "parquet": {
                        Pangol1.getController().updateTaskStatus(taskId, TaskStatus.RUNNING);
                        DataTable table = DataTable.of(file);
                        if (table != null) {
                            loaded.put(file, table);
                            Pangol1.getController().updateTaskStatus(taskId, TaskStatus.SUCCESS);
                            return List.of(table);
                        }
                    }
                        break;
                    default: {
                        Pangol1.getController().updateTaskStatus(taskId, TaskStatus.RUNNING);
                        File outputFile = FileService.replaceExtention(file, "parquet");
                        DataTable table = convert(file, outputFile);
                        if (table != null) {
                            loaded.put(outputFile, table);
                            Pangol1.getController().updateTaskStatus(taskId, TaskStatus.SUCCESS);
                            return List.of(table);
                        }
                    }
                        break;
                }
            }

        } catch (Exception e) {
            Log.debug("Failed to load table from file: " + file.getAbsolutePath(), e);
        }

        Pangol1.getController().updateTaskStatus(taskId, TaskStatus.FAILED);
        return List.of();
    }

    public static List<DataTable> load(URI uri, File targetDir) {
        try {
            URL url = uri.toURL();
            return processURL(url, targetDir);
        } catch (Exception e) {
            Log.mode(() -> {
                Log.debug("Failed to load table from URI: " + uri, e);
            }, () -> {
                Pangol1.getController().onException(e);
            });
            return List.of();
        }
    }

    private static DataTable convert(File inputPath, File outputPath) {
        String taskId = Pangol1.getController().addTask(Lang.get("task.convert", inputPath.getName()),
                TaskStatus.PENDING);

        String tableIn = inputPath.getAbsolutePath().replace("\\", "/");
        String tableOut = outputPath.getAbsolutePath().replace("\\", "/");

        try (DuckDBConnection connection = (DuckDBConnection) DriverManager.getConnection("jdbc:duckdb:")) {

            List<String> columnNames = new ArrayList<>();
            List<DataType> columnTypes = new ArrayList<>();
            float castSensitivity = Config.getFloat("settings.table.cast_sensitivity", 0.95f);

            try (Statement statement = connection.createStatement()) {
                statement.execute(String.format(
                        "CREATE TEMP TABLE staging AS " +
                                "SELECT * FROM read_csv('%s', header=true, all_varchar=true, nullstr=['',' '])",
                        tableIn));

                try (ResultSet columnResult = statement.executeQuery("DESCRIBE staging")) {
                    while (columnResult.next()) {
                        columnNames.add(columnResult.getString("column_name"));
                    }
                }

                Pangol1.getController().updateTask(taskId, Lang.get("task.convert", inputPath.getName()),
                        TaskStatus.RUNNING);

                String statsQuery = "SELECT " + IntStream.range(0, columnNames.size())
                        .mapToObj(index -> {
                            String columnName = columnNames.get(index);
                            return String.format(
                                    "COUNT(\"%s\") AS col_%d_non_null, " +
                                            "COUNT(TRY_CAST(\"%s\" AS INTEGER)) AS col_%d_int, " +
                                            "COUNT(TRY_CAST(REPLACE(\"%s\", ',', '.') AS DOUBLE)) AS col_%d_double, " +
                                            "COUNT(TRY_CAST(\"%s\" AS BOOLEAN)) AS col_%d_boolean, " +
                                            "COUNT(COALESCE(" +
                                            "TRY_STRPTIME(\"%s\", '%%d/%%m/%%Y')," +
                                            "TRY_STRPTIME(\"%s\", '%%Y-%%m-%%d')," +
                                            "TRY_STRPTIME(\"%s\", '%%m/%%d/%%Y')" +
                                            ")) AS col_%d_date",
                                    columnName, index,
                                    columnName, index,
                                    columnName, index,
                                    columnName, index,
                                    columnName, columnName, columnName, index);
                        })
                        .collect(Collectors.joining(", "))
                        + " FROM staging";

                List<Map<String, Long>> columnStats = new ArrayList<>();
                try (ResultSet statsResult = statement.executeQuery(statsQuery)) {
                    statsResult.next();
                    for (int index = 0; index < columnNames.size(); index++) {
                        Map<String, Long> stats = new HashMap<>();
                        stats.put("non_null", statsResult.getLong("col_" + index + "_non_null"));
                        stats.put("int", statsResult.getLong("col_" + index + "_int"));
                        stats.put("double", statsResult.getLong("col_" + index + "_double"));
                        stats.put("boolean", statsResult.getLong("col_" + index + "_boolean"));
                        stats.put("date", statsResult.getLong("col_" + index + "_date"));
                        columnStats.add(stats);
                    }
                }

                for (Map<String, Long> stats : columnStats) {
                    long nonNull = stats.get("non_null");
                    if (nonNull == 0) {
                        columnTypes.add(DataType.STRING);
                        continue;
                    }
                    if (stats.get("boolean") >= nonNull * castSensitivity) {
                        columnTypes.add(DataType.BOOLEAN);
                    } else if (stats.get("int") >= nonNull * castSensitivity) {
                        columnTypes.add(DataType.INTEGER);
                    } else if (stats.get("double") >= nonNull * castSensitivity) {
                        columnTypes.add(DataType.DOUBLE);
                    } else if (stats.get("date") >= nonNull * castSensitivity) {
                        columnTypes.add(DataType.DATE);
                    } else {
                        columnTypes.add(DataType.STRING);
                    }
                }

                String castSelectClause = IntStream.range(0, columnNames.size())
                        .mapToObj(index -> {
                            String columnName = columnNames.get(index);
                            DataType columnType = columnTypes.get(index);
                            return switch (columnType) {
                                case INTEGER ->
                                    String.format("TRY_CAST(\"%s\" AS INTEGER) AS \"%s\"", columnName, columnName);
                                case DOUBLE ->
                                    String.format("TRY_CAST(REPLACE(\"%s\", ',', '.')  AS DOUBLE) AS \"%s\"",
                                            columnName, columnName);
                                case BOOLEAN ->
                                    String.format("TRY_CAST(\"%s\" AS BOOLEAN) AS \"%s\"", columnName, columnName);
                                case DATE -> String.format(
                                        "COALESCE(" +
                                                "TRY_STRPTIME(\"%s\", '%%d/%%m/%%Y')," +
                                                "TRY_STRPTIME(\"%s\", '%%Y-%%m-%%d')," +
                                                "TRY_STRPTIME(\"%s\", '%%m/%%d/%%Y')" +
                                                ") AS \"%s\"",
                                        columnName, columnName, columnName, columnName);
                                default -> String.format("\"%s\"", columnName);
                            };
                        })
                        .collect(Collectors.joining(", "));

                statement.execute(String.format(
                        "COPY (SELECT %s FROM staging) TO '%s' (FORMAT PARQUET, CODEC 'SNAPPY')",
                        castSelectClause, tableOut));

                long rowCount;
                try (ResultSet countResult = statement.executeQuery(
                        String.format("SELECT COUNT(*) FROM '%s'", tableOut))) {
                    countResult.next();
                    rowCount = countResult.getLong(1);
                }

                Pangol1.getController().updateTask(taskId, Lang.get("task.convert_done", inputPath.getName()),
                        TaskStatus.SUCCESS);
                String alias = inputPath.getName().replaceFirst("[.][^.]+$", "");
                return new DataTable(outputPath, alias, columnNames, columnTypes, rowCount, columnNames.size());
            }

        } catch (Exception e) {
            Pangol1.getController().updateTaskStatus(taskId, TaskStatus.FAILED);
            Log.debug("Failed to convert table from " + inputPath + " to " + outputPath, e);
            return convertSafe(inputPath, outputPath);
        }
    }

    private static DataTable convertSafe(File inputPath, File outputPath) {
        String taskId = Pangol1.getController().addTask(Lang.get("task.convert_safe", inputPath.getName()),
                TaskStatus.PENDING);

        String tableIn = inputPath.getAbsolutePath().replace("\\", "/");
        String tableOut = outputPath.getAbsolutePath().replace("\\", "/");
        try (DuckDBConnection connection = (DuckDBConnection) DriverManager.getConnection("jdbc:duckdb:")) {
            try (Statement statement = connection.createStatement()) {
                Pangol1.getController().updateTaskStatus(taskId, TaskStatus.RUNNING);
                statement.execute(String.format(
                        "COPY read_csv_auto('%s', header=true, nullstr=['',' '], ignore_errors=true) TO '%s' (FORMAT PARQUET, CODEC 'SNAPPY')",
                        tableIn, tableOut));
            }

            Pangol1.getController().updateTaskStatus(taskId, TaskStatus.SUCCESS);
            return DataTable.of(outputPath);
        } catch (Exception e) {
            Log.mode(() -> {
                Log.debug("Failed to convert table from " + inputPath + " to " + outputPath + " using safe mode", e);
            }, () -> {
                Pangol1.getController().onException(e);
            });
            Pangol1.getController().updateTaskStatus(taskId, TaskStatus.FAILED);
            return null;
        }
    }

    private static List<DataTable> processDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return List.of();
        }

        List<DataTable> result = new ArrayList<>();
        for (File f : files) {
            result.addAll(load(f, dir));
        }
        return result;
    }

    private static HttpURLConnection openHttpConnection(URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(true);

        int code = conn.getResponseCode();
        if (code == HttpURLConnection.HTTP_MOVED_PERM
                || code == HttpURLConnection.HTTP_MOVED_TEMP
                || code == 307
                || code == 308) {

            String redirect = conn.getHeaderField("Location");
            if (redirect != null) {
                conn.disconnect();
                return openHttpConnection(URI.create(redirect).toURL());
            }
        }

        return conn;
    }

    private static List<DataTable> processURL(URL url, File targetDir) {
        String taskId = Pangol1.getController().addTask(Lang.get("task.download", url.toString()),
                TaskStatus.PENDING);

        try {
            HttpURLConnection conn = openHttpConnection(url);

            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                throw new FileNotFoundException("Remote resource not found: " + url);
            }
            if (code < 200 || code >= 300) {
                throw new IOException("Unexpected HTTP response " + code + " for: " + url);
            }

            Pangol1.getController().updateTask(taskId, Lang.get("task.download", url.toString()),
                    TaskStatus.RUNNING);
            File file = new File(targetDir, FileService.getRemoteFileName(conn, url));
            try (InputStream inputStream = conn.getInputStream();
                    FileOutputStream outputStream = new FileOutputStream(file)) {
                inputStream.transferTo(outputStream);
            }

            Pangol1.getController().updateTask(taskId, Lang.get("task.download_done", url.toString()),
                    TaskStatus.SUCCESS);
            return load(file, targetDir);
        } catch (Exception e) {
            Pangol1.getController().updateTaskStatus(taskId, TaskStatus.FAILED);

            Log.mode(() -> {
                Log.debug("Failed to load table from URI: " + url, e);
            }, () -> {
                Pangol1.getController().onException(e);
            });
        }

        return List.of();
    }

    private static List<DataTable> processZip(File zipFile, File targetDir) {
        String taskId = Pangol1.getController().addTask(Lang.get("task.unzip", zipFile.getName()),
                TaskStatus.PENDING);

        List<DataTable> result = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {

            Pangol1.getController().updateTask(taskId, Lang.get("task.unzip", zipFile.getName()),
                    TaskStatus.RUNNING);
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {

                File resolvedFile = new File(targetDir, entry.getName()).getCanonicalFile();

                if (!resolvedFile.toPath().startsWith(targetDir.getCanonicalFile().toPath())) {
                    throw new IOException("Zip Slip detected: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    resolvedFile.mkdirs();
                    continue;
                }

                resolvedFile.getParentFile().mkdirs();
                try (FileOutputStream fos = new FileOutputStream(resolvedFile)) {
                    zis.transferTo(fos);
                }

                result.addAll(load(resolvedFile, targetDir));
            }

            Pangol1.getController().updateTask(taskId, Lang.get("task.unzip_done", zipFile.getName()),
                    TaskStatus.SUCCESS);
        } catch (Exception e) {
            Pangol1.getController().updateTaskStatus(taskId, TaskStatus.FAILED);

            Log.mode(() -> {
                Log.debug("Failed to unzip file: " + zipFile, e);
            }, () -> {
                Pangol1.getController().onException(e);
            });
        }
        return result;
    }

    public static void addRecent(File file) {
        recents.add(file);
    }

    public static void removeRecent(File file) {
        recents.remove(file);
    }

    public static Set<File> getRecentTables() {
        return recents;
    }

    public static void loadRecents() {
        Config.getByteArray("settings.startup.recent_tables", new byte[0]);
        try (Scanner scanner = new Scanner(
                new ByteArrayInputStream(Config.getByteArray("settings.startup.recent_tables", new byte[0])))) {
            int i = 0;
            while (scanner.hasNextLine() && i < MAX_RECENTS) {
                File recentFile = new File(scanner.nextLine());
                if (recentFile.exists() && recentFile.isFile()) {
                    recents.add(recentFile);
                }
                i++;
            }
        }
    }

    public static void saveRecents() {
        StringBuilder sb = new StringBuilder();
        for (File recent : recents) {
            if (recent != null && recent.exists() && recent.isFile()) {
                sb.append(recent.getAbsolutePath()).append("\n");
            }
        }
        Config.put("settings.startup.recent_tables", sb.toString().getBytes());
    }

}
