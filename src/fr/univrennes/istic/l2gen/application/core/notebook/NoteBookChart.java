package fr.univrennes.istic.l2gen.application.core.notebook;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.naming.SizeLimitExceededException;

import org.duckdb.DuckDBConnection;

import fr.univrennes.istic.l2gen.application.Pangol1;
import fr.univrennes.istic.l2gen.application.core.TaskStatus;
import fr.univrennes.istic.l2gen.application.core.config.Lang;
import fr.univrennes.istic.l2gen.application.core.config.Log;
import fr.univrennes.istic.l2gen.application.core.filter.Filter;
import fr.univrennes.istic.l2gen.application.core.filter.FilterBuilder;
import fr.univrennes.istic.l2gen.application.core.table.DataTable;
import fr.univrennes.istic.l2gen.geometry.Point;
import fr.univrennes.istic.l2gen.io.svg.SVGExport;
import fr.univrennes.istic.l2gen.io.xml.model.XMLAttribute;
import fr.univrennes.istic.l2gen.io.xml.model.XMLTag;
import fr.univrennes.istic.l2gen.svg.color.Color;
import fr.univrennes.istic.l2gen.visustats.data.DataGroup;
import fr.univrennes.istic.l2gen.visustats.data.DataSet;
import fr.univrennes.istic.l2gen.visustats.data.Label;
import fr.univrennes.istic.l2gen.visustats.data.Value;
import fr.univrennes.istic.l2gen.visustats.view.DataViewType;
import fr.univrennes.istic.l2gen.visustats.view.datagroup.AbstractDataGroupView;
import fr.univrennes.istic.l2gen.visustats.view.datagroup.AreaDataGroupView;
import fr.univrennes.istic.l2gen.visustats.view.datagroup.BarDataGroupView;
import fr.univrennes.istic.l2gen.visustats.view.datagroup.ColumnsDataGroupView;
import fr.univrennes.istic.l2gen.visustats.view.datagroup.LineDataGroupView;
import fr.univrennes.istic.l2gen.visustats.view.datagroup.PieDataGroupView;
import fr.univrennes.istic.l2gen.visustats.view.datagroup.ScatterDataGroupView;
import fr.univrennes.istic.l2gen.visustats.view.datagroup.SpiderDataGroupView;
import fr.univrennes.istic.l2gen.visustats.view.datagroup.axis.DataAxisViewScaleType;

public final class NoteBookChart implements NoteBookValue {
    private static final int MAX_GROUP_LENGTH = 20;
    private static final String SVG_CHAR_ERROR = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"500\" height=\"100\"><rect width=\"100%\" height=\"100%\" fill=\"#f8d7da\"/><text x=\"50%\" y=\"50%\" dominant-baseline=\"middle\" text-anchor=\"middle\" fill=\"#721c24\" font-family=\"Arial, sans-serif\" font-size=\"14\">ERROR_MESSAGE</text></svg>";

    private final DataTable table;
    private final boolean includeFilters;
    private final boolean percentage;

    private final DataViewType type;
    private final String title;

    private final boolean showLegend;
    private final boolean horizontalLegend;

    private final boolean stacked;
    private final int gridLevel;

    private final int tickCount;
    private final DataAxisViewScaleType scale;

    private final boolean showXAxis;
    private final String xAxisLabel;
    private final boolean showYAxis;
    private final String yAxisLabel;

    private final Optional<Integer> biggerGroupColumn;
    private final int groupColumn;
    private final int valueColumn;

    private List<Color> colors;
    private List<String> labels;

    private String cachedSVG;

    public NoteBookChart(
            DataViewType type,
            String title,

            boolean showLegend,
            boolean horizontalLegend,

            boolean stacked,
            int gridLevel,
            int tickCount,
            DataAxisViewScaleType scale,

            boolean showXAxis,
            String xAxisLabel,
            boolean showYAxis,
            String yAxisLabel,

            DataTable table,
            Optional<Integer> biggerGroupColumn,
            int groupColumn,
            int valueColumn,

            boolean includeFilters,
            boolean percentage,

            List<Color> colors) {

        this.type = type;
        this.title = title;

        this.showLegend = showLegend;
        this.horizontalLegend = horizontalLegend;
        this.colors = colors;

        this.stacked = stacked;
        this.gridLevel = gridLevel;
        this.tickCount = tickCount;
        this.scale = scale;

        this.showXAxis = showXAxis;
        this.xAxisLabel = xAxisLabel;
        this.showYAxis = showYAxis;
        this.yAxisLabel = yAxisLabel;

        this.table = table;
        this.biggerGroupColumn = biggerGroupColumn;
        this.groupColumn = groupColumn;
        this.valueColumn = valueColumn;

        this.includeFilters = includeFilters;
        this.percentage = percentage;

        this.cachedSVG = null;

    }

    private void createShape() {
        List<Filter> filters = new ArrayList<>(table.getFilters());
        if (!includeFilters) {
            table.clearFilters();
        }

        if (biggerGroupColumn.isEmpty()) {
            createDataSetViewShape();
        } else {
            createDataGroupViewShape();
        }

        if (!includeFilters) {
            table.addFilters(filters);
        }
    }

    private AbstractDataGroupView buildChart(DataGroup dataGroup) throws Exception {
        AbstractDataGroupView chart = switch (type) {
            case PIE -> new PieDataGroupView(dataGroup, new Point(), 50, 200, horizontalLegend);
            case BAR -> new BarDataGroupView(dataGroup, new Point(), 50, 20, 400, horizontalLegend);
            case COLUMNS -> new ColumnsDataGroupView(dataGroup, new Point(), 50, 20, 400, horizontalLegend);
            case SCATTER -> new ScatterDataGroupView(dataGroup, new Point(), 50, 20, 400, 5, stacked, horizontalLegend);
            case LINE -> new LineDataGroupView(dataGroup, new Point(), 50, 20, 400, 5, stacked, horizontalLegend);
            case AREA -> new AreaDataGroupView(dataGroup, new Point(), 50, 20, 400, 5, stacked, horizontalLegend);
            case SPIDER ->
                new SpiderDataGroupView(dataGroup, new Point(), 50, 200, 4, gridLevel, stacked, horizontalLegend);
            default -> throw new Exception("Unsupported chart type: " + type);
        };

        chart.setAxisStepCount(tickCount);
        chart.setAxisScaleType(scale);
        chart.setShowXAxis(showXAxis);
        chart.setXAxisLabel(xAxisLabel);
        chart.setShowYAxis(showYAxis);
        chart.setYAxisLabel(yAxisLabel);

        return chart;
    }

    private String wrapInSVG(AbstractDataGroupView chart) {
        int margin = 50;
        XMLTag svgTag = new XMLTag("svg");
        svgTag.addAttribute(new XMLAttribute("xmlns", "http://www.w3.org/2000/svg"));
        svgTag.addAttribute(new XMLAttribute("version", "1.1"));
        svgTag.addAttribute(new XMLAttribute("width", String.valueOf(chart.getWidth() + 2 * margin)));
        svgTag.addAttribute(new XMLAttribute("height", String.valueOf(chart.getHeight() + 2 * margin)));

        chart.move(chart.getWidth() / 2 + margin, chart.getHeight() / 2 + margin);

        XMLTag chartTag = SVGExport.convert(chart, true);
        svgTag.appendChild(chartTag);

        return svgTag.toString();
    }

    private void createDataSetViewShape() {
        if (groupColumn < 0 || valueColumn < 0) {
            this.cachedSVG = SVG_CHAR_ERROR.replace("ERROR_MESSAGE", Lang.get("report.settings.chart.no_data"));
            return;
        }

        String taskLoad = Pangol1.getController().addTask(Lang.get("task.chart.load"), TaskStatus.PENDING);
        String taskCreate = Pangol1.getController().addTask(Lang.get("task.chart.create"), TaskStatus.PENDING);

        String groupColumnName = table.getSQLColumnName(groupColumn);
        String valueColumnName = table.getSQLColumnName(valueColumn);

        StringBuilder sqlSelectClause = new StringBuilder("SELECT ");
        sqlSelectClause.append(groupColumnName).append(", ");
        if (percentage) {
            sqlSelectClause.append("SUM(").append(valueColumnName)
                    .append(") * 100.0 / (SELECT SUM(")
                    .append(valueColumnName).append(") FROM ").append(table.getSQLName()).append(")");
        } else {
            sqlSelectClause.append("SUM(").append(valueColumnName).append(")");
        }
        sqlSelectClause.append(" FROM");

        StringBuilder queryBuilder = FilterBuilder.base(sqlSelectClause.toString(), table, false);
        queryBuilder.append(" GROUP BY ").append(groupColumnName);
        String query = queryBuilder.toString();

        try (DuckDBConnection connection = (DuckDBConnection) DriverManager.getConnection("jdbc:duckdb:");
                Statement statement = connection.createStatement()) {

            Pangol1.getController().updateTaskStatus(taskLoad, TaskStatus.RUNNING);
            ResultSet resultSet = statement.executeQuery(query);

            Pangol1.getController().updateTaskStatus(taskLoad, TaskStatus.SUCCESS);
            Pangol1.getController().updateTaskStatus(taskCreate, TaskStatus.RUNNING);

            DataSet dataSet = new DataSet(new Label(title));
            Map<String, Color> legendColors = new LinkedHashMap<>();

            boolean hasRows = false;
            int colorIndex = 0;
            while (resultSet.next()) {
                if (colors.size() <= colorIndex) {
                    colors.add(Color.random());
                }

                Color color = colors.get(colorIndex++);
                dataSet.values().add(new Value(resultSet.getDouble(2), color));
                legendColors.put(resultSet.getString(1), color);
                hasRows = true;
            }
            labels = new ArrayList<>(legendColors.keySet());

            if (hasRows) {
                DataGroup dataGroup = new DataGroup(new Label(title));
                dataGroup.add(dataSet);

                if (showLegend) {
                    for (Map.Entry<String, Color> entry : legendColors.entrySet()) {
                        dataGroup.add(new Label(entry.getKey()));
                    }
                }

                this.cachedSVG = wrapInSVG(buildChart(dataGroup));
            } else {
                throw new Exception("No data to display");
            }

            Pangol1.getController().updateTaskStatus(taskCreate, TaskStatus.SUCCESS);
        } catch (Exception e) {
            Pangol1.getController().updateTaskStatus(taskLoad, TaskStatus.FAILED);
            Pangol1.getController().updateTaskStatus(taskCreate, TaskStatus.FAILED);
            if (e instanceof SQLException) {
                Log.debug(query);
            }
            this.cachedSVG = SVG_CHAR_ERROR.replace("ERROR_MESSAGE", Lang.get("report.settings.chart.no_data"));
            Log.debug("Failed to create chart SVG", e);
        }
    }

    private void createDataGroupViewShape() {
        if (groupColumn < 0 || valueColumn < 0) {
            this.cachedSVG = SVG_CHAR_ERROR.replace("ERROR_MESSAGE", Lang.get("report.settings.chart.no_data"));
            return;
        }

        String taskLoad = Pangol1.getController().addTask(Lang.get("task.chart.load"), TaskStatus.PENDING);
        String taskCreate = Pangol1.getController().addTask(Lang.get("task.chart.create"), TaskStatus.PENDING);

        String biggerGroupColumnName = table.getSQLColumnName(biggerGroupColumn.get());
        String groupColumnName = table.getSQLColumnName(groupColumn);
        String valueColumnName = table.getSQLColumnName(valueColumn);

        StringBuilder sqlSelectClause = new StringBuilder("SELECT ");
        sqlSelectClause.append(biggerGroupColumnName).append(", ");
        sqlSelectClause.append(groupColumnName).append(", ");
        if (percentage) {
            sqlSelectClause.append("SUM(").append(valueColumnName)
                    .append(") * 100.0 / (SELECT SUM(")
                    .append(valueColumnName).append(") FROM ").append(table.getSQLName()).append(")");
        } else {
            sqlSelectClause.append("SUM(").append(valueColumnName).append(")");
        }
        sqlSelectClause.append(" FROM");

        StringBuilder queryBuilder = FilterBuilder.base(sqlSelectClause.toString(), table, false);
        queryBuilder.append(" GROUP BY ")
                .append(biggerGroupColumnName).append(", ").append(groupColumnName);
        queryBuilder.append(" ORDER BY ")
                .append(biggerGroupColumnName).append(", ").append(groupColumnName);
        String query = queryBuilder.toString();

        try (DuckDBConnection connection = (DuckDBConnection) DriverManager.getConnection("jdbc:duckdb:");
                Statement statement = connection.createStatement()) {

            Pangol1.getController().updateTaskStatus(taskLoad, TaskStatus.RUNNING);
            ResultSet resultSet = statement.executeQuery(query);

            Pangol1.getController().updateTaskStatus(taskLoad, TaskStatus.SUCCESS);
            Pangol1.getController().updateTaskStatus(taskCreate, TaskStatus.RUNNING);

            Map<String, Map<String, Double>> groupedValues = new LinkedHashMap<>();
            Map<String, Color> legendColors = new LinkedHashMap<>();
            List<String> legendOrder = new ArrayList<>();

            boolean hasRows = false;
            int colorIndex = 0;
            while (resultSet.next()) {
                String biggerGroupValue = resultSet.getString(1);
                String groupValue = resultSet.getString(2);
                double value = resultSet.getDouble(3);

                groupedValues
                        .computeIfAbsent(biggerGroupValue, key -> new LinkedHashMap<>())
                        .put(groupValue, value);

                if (!legendColors.containsKey(groupValue)) {
                    if (colors.size() <= colorIndex) {
                        colors.add(Color.random());
                    }
                    legendColors.put(groupValue, colors.get(colorIndex++));
                    legendOrder.add(groupValue);
                }

                hasRows = true;
            }
            labels = new ArrayList<>(legendOrder);

            if (hasRows) {
                DataGroup dataGroup = new DataGroup(new Label(title));

                for (Map.Entry<String, Map<String, Double>> entry : groupedValues.entrySet()) {
                    DataSet dataSet = new DataSet(new Label(entry.getKey()));
                    Map<String, Double> valuesByLegend = entry.getValue();

                    for (String legend : legendOrder) {
                        double value = valuesByLegend.getOrDefault(legend, 0.0);
                        dataSet.values().add(new Value(value, legendColors.get(legend)));
                    }

                    if (dataGroup.size() < MAX_GROUP_LENGTH) {
                        dataGroup.add(dataSet);
                    } else {
                        throw new SizeLimitExceededException();
                    }
                }

                if (showLegend) {
                    for (String legend : legendOrder) {
                        dataGroup.add(new Label(legend));
                    }
                }

                this.cachedSVG = wrapInSVG(buildChart(dataGroup));
            } else {
                throw new Exception("No data to display");
            }

            Pangol1.getController().updateTaskStatus(taskCreate, TaskStatus.SUCCESS);
        } catch (SizeLimitExceededException e) {
            Pangol1.getController().updateTaskStatus(taskLoad, TaskStatus.FAILED);
            Pangol1.getController().updateTaskStatus(taskCreate, TaskStatus.FAILED);

            this.cachedSVG = SVG_CHAR_ERROR.replace("ERROR_MESSAGE", Lang.get("report.settings.chart.size_limit"));
            Log.debug("Failed to create chart SVG: too much groups", e);
        } catch (Exception e) {
            Pangol1.getController().updateTaskStatus(taskLoad, TaskStatus.FAILED);
            Pangol1.getController().updateTaskStatus(taskCreate, TaskStatus.FAILED);

            if (e instanceof SQLException) {
                Log.debug(query);
            }
            this.cachedSVG = SVG_CHAR_ERROR.replace("ERROR_MESSAGE", Lang.get("report.settings.chart.no_data"));
            Log.debug("Failed to create chart SVG", e);
        }
    }

    public String getSVG() {
        if (cachedSVG == null) {
            createShape();
        }
        return cachedSVG;
    }

    public DataViewType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public boolean isStacked() {
        return stacked;
    }

    public int getGridLevel() {
        return gridLevel;
    }

    public boolean isLegendVisible() {
        return showLegend;
    }

    public boolean isLegendHorizontal() {
        return horizontalLegend;
    }

    public int getTickCount() {
        return tickCount;
    }

    public DataAxisViewScaleType getScale() {
        return scale;
    }

    public boolean isXVisible() {
        return showXAxis;
    }

    public String getXLabel() {
        return xAxisLabel;
    }

    public boolean isYVisible() {
        return showYAxis;
    }

    public String getYLabel() {
        return yAxisLabel;
    }

    public DataTable getTable() {
        return table;
    }

    public Optional<Integer> getBiggerGroupColumn() {
        return biggerGroupColumn;
    }

    public int getGroupColumn() {
        return groupColumn;
    }

    public int getValueColumn() {
        return valueColumn;
    }

    public boolean isIncludeFilters() {
        return includeFilters;
    }

    public boolean isPercentage() {
        return percentage;
    }

    public List<Color> getColors() {
        return colors;
    }

    public List<String> getColorLabels() {
        return labels;
    }

    @Override
    public void exportHTML(StringBuilder html) {
        html.append(getSVG());
    }

}
