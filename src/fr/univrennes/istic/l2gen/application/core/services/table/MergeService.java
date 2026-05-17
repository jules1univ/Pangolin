package fr.univrennes.istic.l2gen.application.core.services.table;

import fr.univrennes.istic.l2gen.application.core.TaskStatus;
import fr.univrennes.istic.l2gen.application.core.config.Lang;
import fr.univrennes.istic.l2gen.application.core.config.Log;
import fr.univrennes.istic.l2gen.application.core.table.DataTable;
import fr.univrennes.istic.l2gen.application.Pangol1;
import org.duckdb.DuckDBConnection;

import java.io.File;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

public final class MergeService {

    private MergeService() {
    }

    public static DataTable merge(DataTable leftTable, DataTable rightTable, MergeConfig config) {
        String taskId = Pangol1.getController().addTask(
                Lang.get("task.merge.execute", leftTable.getAlias(), rightTable.getAlias()),
                TaskStatus.PENDING);

        String query = buildQuery(leftTable, rightTable, config);
        File resultFile = new File(rightTable.getPath().getParent(), config.getResultName() + ".parquet");
        String absoluteFilePath = resultFile.getAbsolutePath().replace("\\", "/");
        String copyQuery = String.format("COPY (%s) TO '%s' (FORMAT PARQUET, CODEC 'SNAPPY')", query, absoluteFilePath);

        try (DuckDBConnection connection = (DuckDBConnection) DriverManager.getConnection("jdbc:duckdb:");
                Statement statement = connection.createStatement()) {

            Pangol1.getController().updateTaskStatus(taskId, TaskStatus.RUNNING);

            statement.execute(copyQuery);

            ResultSet resultSet = statement.executeQuery(String.format("SELECT * FROM '%s'", absoluteFilePath));
            DataTable result = DataTable.of(resultSet, resultFile, config.getResultName());

            TableService.add(result);
            TableService.addRecent(resultFile);

            Pangol1.getController().updateTaskStatus(taskId, TaskStatus.SUCCESS);
            return result;

        } catch (Exception exception) {
            Pangol1.getController().updateTaskStatus(taskId, TaskStatus.FAILED);
            Log.debug("Failed to execute merge query: " + copyQuery, exception);
            return null;
        }
    }

    public static String buildQuery(DataTable leftTable, DataTable rightTable, MergeConfig config) {
        String leftAlias = "left_table";
        String rightAlias = "right_table";

        if (config.getJoinType() == MergeJoinType.UNION) {
            return buildUnionQuery(leftTable, rightTable, config, leftAlias, rightAlias);
        }

        return buildJoinQuery(leftTable, rightTable, config, leftAlias, rightAlias);
    }

    private static String buildJoinQuery(DataTable leftTable, DataTable rightTable, MergeConfig config,
            String leftAlias, String rightAlias) {
        StringBuilder query = new StringBuilder();

        query.append("SELECT ");
        appendSelectColumns(query, leftTable, rightTable, config, leftAlias, rightAlias);

        query.append(" FROM ").append(leftTable.getSQLName()).append(" AS ").append(leftAlias);
        query.append(" ").append(config.getJoinType().getSql()).append(" ");
        query.append(rightTable.getSQLName()).append(" AS ").append(rightAlias);

        List<MergeJoinCondition> joinConditions = config.getJoinConditions();
        if (!joinConditions.isEmpty()) {
            query.append(" ON ");
            for (int i = 0; i < joinConditions.size(); i++) {
                MergeJoinCondition condition = joinConditions.get(i);
                query.append(leftAlias).append(".").append(condition.getLeftColumnSqlName());
                query.append(" = ");
                query.append(rightAlias).append(".").append(condition.getRightColumnSqlName());
                if (i < joinConditions.size() - 1) {
                    query.append(" AND ");
                }
            }
        }

        return query.toString();
    }

    private static String buildUnionQuery(DataTable leftTable, DataTable rightTable, MergeConfig config,
            String leftAlias, String rightAlias) {
        List<String> leftColumns = leftTable.getColumnNames();
        List<String> rightColumns = rightTable.getColumnNames();

        StringBuilder leftSelect = new StringBuilder("SELECT ");
        StringBuilder rightSelect = new StringBuilder("SELECT ");

        for (int i = 0; i < leftColumns.size(); i++) {
            leftSelect.append(leftTable.getSQLColumnName(i));
            if (i < leftColumns.size() - 1) {
                leftSelect.append(", ");
            }
        }
        leftSelect.append(" FROM ").append(leftTable.getSQLName());

        for (int i = 0; i < rightColumns.size(); i++) {
            rightSelect.append(rightTable.getSQLColumnName(i));
            if (i < rightColumns.size() - 1) {
                rightSelect.append(", ");
            }
        }
        rightSelect.append(" FROM ").append(rightTable.getSQLName());

        return leftSelect + " UNION ALL " + rightSelect;
    }

    private static void appendSelectColumns(StringBuilder query, DataTable leftTable, DataTable rightTable,
            MergeConfig config, String leftAlias, String rightAlias) {
        List<String> leftColumns = leftTable.getColumnNames();
        List<String> rightColumns = rightTable.getColumnNames();
        List<MergeJoinCondition> joinConditions = config.getJoinConditions();

        boolean firstColumn = true;

        for (int i = 0; i < leftColumns.size(); i++) {
            if (!firstColumn) {
                query.append(", ");
            }
            query.append(leftAlias).append(".").append(leftTable.getSQLColumnName(i));
            firstColumn = false;
        }

        for (int i = 0; i < rightColumns.size(); i++) {
            String rightColumnName = rightColumns.get(i);
            boolean isJoinKey = joinConditions.stream()
                    .anyMatch(condition -> condition.getRightColumnName().equals(rightColumnName));
            if (isJoinKey) {
                continue;
            }

            boolean collidesWithLeft = leftColumns.stream().anyMatch(leftCol -> leftCol.equals(rightColumnName));
            if (!firstColumn) {
                query.append(", ");
            }

            if (collidesWithLeft) {
                query.append(rightAlias).append(".").append(rightTable.getSQLColumnName(i))
                        .append(" AS ").append(rightAlias).append("_").append(rightTable.getSQLColumnName(i));
            } else {
                query.append(rightAlias).append(".").append(rightTable.getSQLColumnName(i));
            }
            firstColumn = false;
        }
    }
}