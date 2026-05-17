package fr.univrennes.istic.l2gen.application.gui.dialog.subtable;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import fr.univrennes.istic.l2gen.application.Pangol1;
import fr.univrennes.istic.l2gen.application.core.TaskStatus;
import fr.univrennes.istic.l2gen.application.core.config.Lang;
import fr.univrennes.istic.l2gen.application.core.filter.FilterBuilder;
import fr.univrennes.istic.l2gen.application.core.services.table.TableService;
import fr.univrennes.istic.l2gen.application.core.table.DataTable;
import fr.univrennes.istic.l2gen.application.core.table.DataType;
import fr.univrennes.istic.l2gen.application.gui.GUIController;
import fr.univrennes.istic.l2gen.application.gui.dialog.DialogBase;

public final class SubtableDialog extends JDialog {

    private final DataTable table;

    private JTextField nameField;
    private JComboBox<String> groupColumnBox;
    private JCheckBox includeFiltersBox;
    private JPanel aggregationPanel;
    private JButton createButton;

    private DataTable result;

    private final Map<Integer, AggregationOp> selectedAggregations = new HashMap<>();

    private SubtableDialog(Frame parent, DataTable table) {
        super(parent, Lang.get("subtable.title"), true);
        this.table = table;
        build();
        setMinimumSize(new Dimension((int) (DialogBase.WIDTH * 0.7), DialogBase.HEIGHT));
        pack();
        setLocationRelativeTo(parent);
    }

    public static DataTable show(Frame parent, DataTable table) {
        SubtableDialog dialog = new SubtableDialog(parent, table);
        dialog.setVisible(true);
        return dialog.result;
    }

    private void build() {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));

        root.add(buildConfigPanel(), BorderLayout.NORTH);
        root.add(buildAggregationPanel(), BorderLayout.CENTER);
        root.add(buildButtonBar(), BorderLayout.SOUTH);

        setContentPane(root);
        refreshAggregationPanel();
    }

    private JPanel buildConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());

        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(4, 0, 4, 10);
        labelConstraints.gridx = 0;

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.weightx = 1.0;
        fieldConstraints.insets = new Insets(4, 0, 4, 0);
        fieldConstraints.gridx = 1;

        labelConstraints.gridy = 0;
        fieldConstraints.gridy = 0;
        panel.add(new JLabel(Lang.get("subtable.name")), labelConstraints);
        nameField = new JTextField(24);
        nameField.setText(Lang.get("subtable.default_name", table.getAlias()));
        panel.add(nameField, fieldConstraints);

        labelConstraints.gridy = 1;
        fieldConstraints.gridy = 1;
        panel.add(new JLabel(Lang.get("subtable.group_by")), labelConstraints);
        groupColumnBox = new JComboBox<>(table.getColumnNames().toArray(new String[0]));
        groupColumnBox.addActionListener(event -> refreshAggregationPanel());
        panel.add(groupColumnBox, fieldConstraints);

        labelConstraints.gridy = 2;
        fieldConstraints.gridy = 2;
        panel.add(new JLabel(Lang.get("subtable.include_filters")), labelConstraints);
        includeFiltersBox = new JCheckBox();
        includeFiltersBox.setSelected(true);
        panel.add(includeFiltersBox, fieldConstraints);

        return panel;
    }

    private JPanel buildAggregationPanel() {
        aggregationPanel = new JPanel();
        aggregationPanel.setLayout(new BoxLayout(aggregationPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(aggregationPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder(Lang.get("subtable.aggregations")));
        scrollPane.setPreferredSize(new Dimension(0, 260));

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(scrollPane, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildButtonBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));

        JButton cancelButton = new JButton(Lang.get("subtable.cancel"));
        cancelButton.addActionListener(event -> dispose());

        createButton = new JButton(Lang.get("subtable.create"));
        createButton.addActionListener(event -> createSubtable());

        panel.add(cancelButton);
        panel.add(createButton);
        getRootPane().setDefaultButton(createButton);
        return panel;
    }

    private void refreshAggregationPanel() {
        if (aggregationPanel == null || groupColumnBox == null) {
            return;
        }

        aggregationPanel.removeAll();

        int groupIndex = groupColumnBox.getSelectedIndex();
        int columnCount = (int) table.getColumnCount();

        for (int i = 0; i < columnCount; i++) {
            if (i == groupIndex) {
                continue;
            }

            final int columnIndex = i;

            DataType type = table.getColumnType(columnIndex);
            AggregationOp[] options = getAggregationOptions(type);
            AggregationOp selected = selectedAggregations.getOrDefault(columnIndex, getDefaultAggregation(type));
            selectedAggregations.put(columnIndex, selected);

            JComboBox<AggregationOp> comboBox = new JComboBox<>(options);
            comboBox.setSelectedItem(selected);
            comboBox.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
                JLabel label = new JLabel(value != null ? value.getLabel() : "");
                if (isSelected) {
                    label.setOpaque(true);
                    label.setBackground(list.getSelectionBackground());
                    label.setForeground(list.getSelectionForeground());
                }
                return label;
            });
            comboBox.addActionListener(event -> {
                AggregationOp op = (AggregationOp) comboBox.getSelectedItem();
                if (op != null) {
                    selectedAggregations.put(columnIndex, op);
                }
            });

            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setBorder(new EmptyBorder(2, 0, 2, 0));
            row.add(new JLabel(formatColumnLabel(columnIndex, type)), BorderLayout.WEST);
            row.add(comboBox, BorderLayout.CENTER);
            aggregationPanel.add(row);
        }

        aggregationPanel.revalidate();
        aggregationPanel.repaint();
    }

    private void createSubtable() {
        String taskId = Pangol1.getController().addTask(Lang.get("task.subtable", nameField.getText().trim()),
                TaskStatus.PENDING);

        String subtableName = nameField.getText().trim();
        if (subtableName.isBlank()) {
            showValidationError(Lang.get("subtable.validation.name"));
            Pangol1.getController().updateTaskStatus(taskId, TaskStatus.FAILED);
            return;
        }

        int groupIndex = groupColumnBox.getSelectedIndex();
        if (groupIndex < 0) {
            showValidationError(Lang.get("subtable.validation.group"));
            Pangol1.getController().updateTaskStatus(taskId, TaskStatus.FAILED);
            return;
        }

        String baseAlias = table.getAlias();
        String newAlias = baseAlias + "_" + subtableName;
        File outputFile = new File(table.getPath().getParentFile(), newAlias + ".parquet");

        if (outputFile.exists()) {
            int overwrite = JOptionPane.showConfirmDialog(this,
                    Lang.get("subtable.overwrite", outputFile.getName()),
                    Lang.get("subtable.overwrite_title"),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (overwrite != JOptionPane.YES_OPTION) {
                Pangol1.getController().updateTaskStatus(taskId, TaskStatus.FAILED);

                return;
            }

            TableService.removeLoaded(outputFile);
            if (!outputFile.delete()) {
                GUIController.getInstance()
                        .onException(new java.io.IOException(
                                Lang.get("table.list.menu.delete.failed", outputFile.getAbsolutePath())));
                Pangol1.getController().updateTaskStatus(taskId, TaskStatus.FAILED);
                return;
            }
        }

        try {
            Pangol1.getController().updateTaskStatus(taskId, TaskStatus.RUNNING);

            createSubtableFile(outputFile, groupIndex);

            List<DataTable> tables = TableService.load(outputFile, outputFile.getParentFile());
            if (tables.isEmpty()) {
                showValidationError(Lang.get("subtable.error.failed"));
                Pangol1.getController().updateTaskStatus(taskId, TaskStatus.FAILED);
                return;
            }

            TableService.addRecent(outputFile);
            TableService.saveRecents();

            result = tables.get(0);
            dispose();

            Pangol1.getController().updateTaskStatus(taskId, TaskStatus.SUCCESS);

        } catch (Exception e) {
            GUIController.getInstance().onException(e);
            Pangol1.getController().updateTaskStatus(taskId, TaskStatus.FAILED);
        }
    }

    private void createSubtableFile(File outputFile, int groupIndex) throws Exception {
        String groupColumnSql = table.getSQLColumnName(groupIndex);

        StringBuilder selectClause = new StringBuilder("SELECT ");
        selectClause.append(groupColumnSql);

        int columnCount = (int) table.getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            if (i == groupIndex) {
                continue;
            }

            AggregationOp op = selectedAggregations.getOrDefault(i, getDefaultAggregation(table.getColumnType(i)));
            if (op == AggregationOp.NONE) {
                continue;
            }
            String columnSql = table.getSQLColumnName(i);
            String alias = table.getColumnName(i) + "_" + op.getSuffix();
            selectClause.append(", ")
                    .append(op.toSql(columnSql))
                    .append(" AS ")
                    .append(quoteIdentifier(alias));
        }

        selectClause.append(" FROM");

        StringBuilder queryBuilder;
        if (includeFiltersBox != null && includeFiltersBox.isSelected()) {
            queryBuilder = FilterBuilder.base(selectClause.toString(), table, false);
        } else {
            queryBuilder = new StringBuilder(selectClause).append(" ").append(table.getSQLName());
        }
        queryBuilder.append(" GROUP BY ").append(groupColumnSql);

        String outputPath = outputFile.getAbsolutePath().replace("\\", "/");
        String copyQuery = String.format(
                "COPY (%s) TO '%s' (FORMAT PARQUET, CODEC 'SNAPPY')",
                queryBuilder.toString(),
                outputPath);

        try (Connection connection = DriverManager.getConnection("jdbc:duckdb:");
                Statement statement = connection.createStatement()) {
            statement.execute(copyQuery);
        }
    }

    private String formatColumnLabel(int columnIndex, DataType type) {
        return String.format("(%s) %s", getTypeLabel(type), table.getColumnName(columnIndex));
    }

    private String getTypeLabel(DataType type) {
        return switch (type) {
            case STRING -> Lang.get("table.type.string");
            case INTEGER -> Lang.get("table.type.integer");
            case DOUBLE -> Lang.get("table.type.double");
            case BOOLEAN -> Lang.get("table.type.boolean");
            case DATE -> Lang.get("table.type.date");
            case EMPTY -> Lang.get("table.type.empty");
        };
    }

    private AggregationOp[] getAggregationOptions(DataType type) {
        return switch (type) {
            case INTEGER, DOUBLE -> NUMERIC_AGGS;
            case DATE -> DATE_AGGS;
            case BOOLEAN -> BOOLEAN_AGGS;
            case STRING -> STRING_AGGS;
            case EMPTY -> EMPTY_AGGS;
        };
    }

    private AggregationOp getDefaultAggregation(DataType type) {
        AggregationOp[] options = getAggregationOptions(type);
        return options.length > 0 ? options[0] : AggregationOp.COUNT;
    }

    private void showValidationError(String message) {
        JOptionPane.showMessageDialog(this, message, Lang.get("filter.validation.error"),
                JOptionPane.WARNING_MESSAGE);
    }

    private static String quoteIdentifier(String name) {
        return "\"" + name.replace("\"", "\"\"") + "\"";
    }

    private static final AggregationOp[] NUMERIC_AGGS = new AggregationOp[] {
            AggregationOp.NONE,
            AggregationOp.SUM,
            AggregationOp.AVG,
            AggregationOp.MIN,
            AggregationOp.MAX,
            AggregationOp.COUNT,
            AggregationOp.COUNT_DISTINCT
    };

    private static final AggregationOp[] DATE_AGGS = new AggregationOp[] {
            AggregationOp.NONE,
            AggregationOp.MIN,
            AggregationOp.MAX,
            AggregationOp.COUNT,
            AggregationOp.COUNT_DISTINCT
    };

    private static final AggregationOp[] BOOLEAN_AGGS = new AggregationOp[] {
            AggregationOp.NONE,
            AggregationOp.COUNT,
            AggregationOp.COUNT_DISTINCT,
            AggregationOp.MIN,
            AggregationOp.MAX
    };

    private static final AggregationOp[] STRING_AGGS = new AggregationOp[] {
            AggregationOp.NONE,
            AggregationOp.COUNT,
            AggregationOp.COUNT_DISTINCT,
            AggregationOp.MIN,
            AggregationOp.MAX
    };

    private static final AggregationOp[] EMPTY_AGGS = new AggregationOp[] {
            AggregationOp.NONE,
            AggregationOp.COUNT
    };
}
