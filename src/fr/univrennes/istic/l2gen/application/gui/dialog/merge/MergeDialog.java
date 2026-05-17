package fr.univrennes.istic.l2gen.application.gui.dialog.merge;

import fr.univrennes.istic.l2gen.application.core.config.Lang;
import fr.univrennes.istic.l2gen.application.core.services.table.MergeConfig;
import fr.univrennes.istic.l2gen.application.core.services.table.MergeJoinCondition;
import fr.univrennes.istic.l2gen.application.core.services.table.MergeJoinType;
import fr.univrennes.istic.l2gen.application.core.services.table.MergeService;
import fr.univrennes.istic.l2gen.application.core.services.table.TableService;
import fr.univrennes.istic.l2gen.application.core.table.DataTable;
import fr.univrennes.istic.l2gen.application.gui.dialog.DialogBase;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public final class MergeDialog extends JDialog {

    private final DataTable initialTable;

    private DataTable leftTable;
    private DataTable rightTable;

    private JComboBox<DataTable> leftTableComboBox;
    private JComboBox<DataTable> rightTableComboBox;
    private JComboBox<MergeJoinType> joinTypeComboBox;
    private JTextField resultNameField;

    private JPanel joinConditionsPanel;
    private final List<MergeJoinRow> joinRows = new ArrayList<>();

    private JPanel onSection;
    private JPanel outputColumnsPanel;

    private DataTable result = null;

    private MergeDialog(Frame parent, DataTable table) {
        super(parent, Lang.get("merge.title"), true);
        this.initialTable = table;

        List<DataTable> availableTables = TableService.get();
        this.leftTable = table;
        this.rightTable = availableTables.stream()
                .filter(availableTable -> availableTable != table)
                .findFirst()
                .orElse(table);

        build();
        pack();
        setSize((int) (DialogBase.WIDTH * 1.5), DialogBase.HEIGHT + 80);
        setMinimumSize(new Dimension(DialogBase.WIDTH, DialogBase.HEIGHT));
        setLocationRelativeTo(parent);
        setResizable(true);

        autoDetectJoinColumns();
        refreshOutputColumnsPreview();
    }

    private void build() {
        JPanel root = new JPanel(new BorderLayout());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildConfigPanel(), buildPreviewPanel());
        SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(0.55));

        root.add(splitPane, BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JPanel buildConfigPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.add(buildTableSelectionSection());
        topSection.add(buildJoinTypeSection());
        topSection.add(buildResultNameSection());

        onSection = buildOnConditionsSection();

        panel.add(topSection, BorderLayout.NORTH);
        panel.add(onSection, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildTableSelectionSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(10, 12, 6, 12));

        GridBagConstraints labelGbc = new GridBagConstraints();
        labelGbc.anchor = GridBagConstraints.WEST;
        labelGbc.insets = new Insets(4, 0, 4, 8);
        labelGbc.gridx = 0;

        GridBagConstraints fieldGbc = new GridBagConstraints();
        fieldGbc.fill = GridBagConstraints.HORIZONTAL;
        fieldGbc.weightx = 1.0;
        fieldGbc.insets = new Insets(4, 0, 4, 0);
        fieldGbc.gridx = 1;

        List<DataTable> availableTables = TableService.get();

        labelGbc.gridy = 0;
        fieldGbc.gridy = 0;
        panel.add(new JLabel(Lang.get("merge.left_table")), labelGbc);
        leftTableComboBox = new JComboBox<>(availableTables.toArray(new DataTable[0]));
        leftTableComboBox.setRenderer(new TableNameRenderer());
        leftTableComboBox.setSelectedItem(initialTable);
        leftTableComboBox.addActionListener(event -> onLeftTableChanged());
        panel.add(leftTableComboBox, fieldGbc);

        labelGbc.gridy = 1;
        fieldGbc.gridy = 1;
        panel.add(new JLabel(Lang.get("merge.right_table")), labelGbc);
        rightTableComboBox = new JComboBox<>(availableTables.toArray(new DataTable[0]));
        rightTableComboBox.setRenderer(new TableNameRenderer());
        rightTableComboBox.setSelectedItem(rightTable);
        rightTableComboBox.addActionListener(event -> onRightTableChanged());
        panel.add(rightTableComboBox, fieldGbc);

        return panel;
    }

    private JPanel buildJoinTypeSection() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setBorder(new EmptyBorder(0, 12, 6, 12));

        panel.add(new JLabel(Lang.get("merge.join_type")), BorderLayout.WEST);

        joinTypeComboBox = new JComboBox<>(MergeJoinType.values());
        joinTypeComboBox.setRenderer(new JoinTypeRenderer());
        joinTypeComboBox.addActionListener(event -> onJoinTypeChanged());
        panel.add(joinTypeComboBox, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildResultNameSection() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setBorder(new EmptyBorder(0, 12, 10, 12));

        panel.add(new JLabel(Lang.get("merge.result_name")), BorderLayout.WEST);

        resultNameField = new JTextField(buildDefaultResultName());
        panel.add(resultNameField, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildOnConditionsSection() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(BorderFactory.createTitledBorder(
                null,
                Lang.get("merge.join_conditions"),
                TitledBorder.LEFT,
                TitledBorder.TOP));

        joinConditionsPanel = new JPanel();
        joinConditionsPanel.setLayout(new BoxLayout(joinConditionsPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(joinConditionsPanel);
        scrollPane.setBorder(null);

        JButton addConditionButton = new JButton(Lang.get("merge.add_condition"));
        addConditionButton.addActionListener(event -> appendJoinRow());

        JPanel addButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        addButtonPanel.add(addConditionButton);

        wrapper.add(scrollPane, BorderLayout.CENTER);
        wrapper.add(addButtonPanel, BorderLayout.SOUTH);

        return wrapper;
    }

    private JPanel buildPreviewPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                null,
                Lang.get("merge.output_columns"),
                TitledBorder.LEFT,
                TitledBorder.TOP));

        outputColumnsPanel = new JPanel();
        outputColumnsPanel.setLayout(new BoxLayout(outputColumnsPanel, BoxLayout.Y_AXIS));
        outputColumnsPanel.setBorder(new EmptyBorder(4, 8, 4, 8));

        JScrollPane scrollPane = new JScrollPane(outputColumnsPanel);
        scrollPane.setBorder(null);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildFooter() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setBorder(new EmptyBorder(8, 12, 8, 12));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));

        JButton cancelButton = new JButton(Lang.get("merge.cancel"));
        cancelButton.addActionListener(event -> {
            result = null;
            dispose();
        });

        JButton mergeButton = new JButton(Lang.get("merge.execute"));
        mergeButton.addActionListener(event -> {
            new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    mergeButton.setEnabled(false);
                    cancelButton.setEnabled(false);
                    executeMerge();

                    return null;
                }

                @Override
                protected void done() {
                    mergeButton.setEnabled(true);
                    cancelButton.setEnabled(true);
                }
            }.execute();
        });

        buttonPanel.add(cancelButton);
        buttonPanel.add(mergeButton);
        panel.add(buttonPanel, BorderLayout.EAST);

        return panel;
    }

    private void onLeftTableChanged() {
        DataTable selected = (DataTable) leftTableComboBox.getSelectedItem();
        if (selected == null) {
            return;
        }
        leftTable = selected;
        for (MergeJoinRow row : joinRows) {
            row.updateLeftTable(leftTable);
        }
        resultNameField.setText(buildDefaultResultName());
        autoDetectJoinColumns();
        refreshOutputColumnsPreview();
    }

    private void onRightTableChanged() {
        DataTable selected = (DataTable) rightTableComboBox.getSelectedItem();
        if (selected == null) {
            return;
        }
        rightTable = selected;
        for (MergeJoinRow row : joinRows) {
            row.updateRightTable(rightTable);
        }
        resultNameField.setText(buildDefaultResultName());
        autoDetectJoinColumns();
        refreshOutputColumnsPreview();
    }

    private void onJoinTypeChanged() {
        MergeJoinType selectedType = (MergeJoinType) joinTypeComboBox.getSelectedItem();
        boolean isUnion = selectedType == MergeJoinType.UNION;
        onSection.setVisible(!isUnion);
        refreshOutputColumnsPreview();
    }

    private void autoDetectJoinColumns() {
        joinRows.clear();
        joinConditionsPanel.removeAll();

        List<String> leftColumns = leftTable.getColumnNames();
        List<String> rightColumns = rightTable.getColumnNames();

        for (String leftColumnName : leftColumns) {
            boolean matchFound = rightColumns.stream()
                    .anyMatch(rightColumnName -> rightColumnName.equalsIgnoreCase(leftColumnName));
            if (matchFound) {
                String matchingRightColumn = rightColumns.stream()
                        .filter(rightColumnName -> rightColumnName.equalsIgnoreCase(leftColumnName))
                        .findFirst()
                        .orElse(leftColumnName);
                MergeJoinRow row = new MergeJoinRow(this, leftTable, rightTable, leftColumnName, matchingRightColumn);
                joinRows.add(row);
                joinConditionsPanel.add(row.getPanel());
            }
        }

        if (joinRows.isEmpty()) {
            appendJoinRow();
        }

        joinConditionsPanel.revalidate();
        joinConditionsPanel.repaint();
    }

    private void appendJoinRow() {
        MergeJoinRow row = new MergeJoinRow(this, leftTable, rightTable);
        joinRows.add(row);
        joinConditionsPanel.add(row.getPanel());
        joinConditionsPanel.revalidate();
        joinConditionsPanel.repaint();
    }

    public void removeJoinRow(MergeJoinRow row) {
        joinRows.remove(row);
        joinConditionsPanel.remove(row.getPanel());
        joinConditionsPanel.revalidate();
        joinConditionsPanel.repaint();
    }

    private void refreshOutputColumnsPreview() {
        outputColumnsPanel.removeAll();

        MergeJoinType joinType = (MergeJoinType) joinTypeComboBox.getSelectedItem();

        if (joinType == MergeJoinType.UNION) {
            appendColumnPreviewSection(Lang.get("merge.preview.from_left"), leftTable.getColumnNames(), false);
            appendColumnPreviewSection(Lang.get("merge.preview.from_right"), rightTable.getColumnNames(), false);
        } else {
            List<MergeJoinCondition> currentConditions = buildJoinConditions();
            List<String> joinKeyRightColumnNames = currentConditions.stream()
                    .map(MergeJoinCondition::getRightColumnName)
                    .toList();

            appendColumnPreviewSection(Lang.get("merge.preview.from_left"), leftTable.getColumnNames(), false);

            List<String> rightColumnsFiltered = rightTable.getColumnNames().stream()
                    .filter(rightColumnName -> !joinKeyRightColumnNames.contains(rightColumnName))
                    .toList();

            List<String> rightColumnsRenamed = rightColumnsFiltered.stream()
                    .map(rightColumnName -> {
                        boolean collidesWithLeft = leftTable.getColumnNames().stream()
                                .anyMatch(leftColumnName -> leftColumnName.equals(rightColumnName));
                        return collidesWithLeft ? "right_table_" + rightColumnName + " (renamed)" : rightColumnName;
                    })
                    .toList();

            appendColumnPreviewSection(Lang.get("merge.preview.from_right"), rightColumnsRenamed, false);
        }

        outputColumnsPanel.revalidate();
        outputColumnsPanel.repaint();
    }

    private void appendColumnPreviewSection(String sectionTitle, List<String> columnNames, boolean dimmed) {
        JLabel sectionLabel = new JLabel(sectionTitle);
        sectionLabel.setFont(sectionLabel.getFont().deriveFont(Font.BOLD, 11f));
        sectionLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        sectionLabel.setBorder(new EmptyBorder(6, 0, 2, 0));
        sectionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        outputColumnsPanel.add(sectionLabel);

        for (String columnName : columnNames) {
            JLabel columnLabel = new JLabel(columnName);
            columnLabel.setBorder(new EmptyBorder(1, 8, 1, 0));
            columnLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            if (dimmed) {
                columnLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            }
            outputColumnsPanel.add(columnLabel);
        }
    }

    private void executeMerge() {
        MergeConfig config = buildCurrentConfig();
        if (config == null) {
            JOptionPane.showMessageDialog(this,
                    Lang.get("merge.validation.no_conditions"),
                    Lang.get("merge.validation.error"),
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        result = MergeService.merge(leftTable, rightTable, config);
        if (result == null) {
            JOptionPane.showMessageDialog(this,
                    Lang.get("merge.validation.failed"),
                    Lang.get("merge.validation.error"),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        dispose();
    }

    private MergeConfig buildCurrentConfig() {
        MergeJoinType selectedJoinType = (MergeJoinType) joinTypeComboBox.getSelectedItem();
        if (selectedJoinType == null) {
            return null;
        }

        List<MergeJoinCondition> joinConditions = selectedJoinType == MergeJoinType.UNION
                ? List.of()
                : buildJoinConditions();

        String resultName = resultNameField.getText().trim();
        if (resultName.isEmpty()) {
            resultName = buildDefaultResultName();
        }

        return new MergeConfig(selectedJoinType, joinConditions, resultName);
    }

    private List<MergeJoinCondition> buildJoinConditions() {
        List<MergeJoinCondition> conditions = new ArrayList<>();
        for (MergeJoinRow row : joinRows) {
            MergeJoinCondition condition = row.buildCondition(leftTable, rightTable);
            if (condition != null) {
                conditions.add(condition);
            }
        }
        return conditions;
    }

    private String buildDefaultResultName() {
        return leftTable.getAlias() + "_" + rightTable.getAlias();
    }

    public DataTable getResult() {
        return result;
    }

    public static DataTable show(Frame parent, DataTable table) {
        MergeDialog dialog = new MergeDialog(parent, table);
        dialog.setVisible(true);
        return dialog.getResult();
    }
}