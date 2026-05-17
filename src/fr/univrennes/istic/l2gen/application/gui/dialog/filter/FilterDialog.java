package fr.univrennes.istic.l2gen.application.gui.dialog.filter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.NumberFormatter;

import fr.univrennes.istic.l2gen.application.core.config.Lang;
import fr.univrennes.istic.l2gen.application.core.filter.Filter;
import fr.univrennes.istic.l2gen.application.core.filter.FilterCondition;
import fr.univrennes.istic.l2gen.application.core.filter.FilterLogic;
import fr.univrennes.istic.l2gen.application.core.filter.FilterSort;
import fr.univrennes.istic.l2gen.application.core.table.DataTable;
import fr.univrennes.istic.l2gen.application.core.table.DataType;
import fr.univrennes.istic.l2gen.application.gui.dialog.DialogBase;

import java.awt.*;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class FilterDialog extends JDialog {

    private final DataTable table;
    private final List<String> columnNames;

    private final List<Filter> result = new ArrayList<>();
    private final List<FilterCardPanel> filterCards = new ArrayList<>();
    private FilterLogic globalLogic = FilterLogic.AND;

    private JComboBox<String> columnComboBox;
    private JComboBox<String> rowLogicComboBox;
    private JPanel conditionRowsPanel;
    private final List<ConditionInputRow> conditionInputRows = new ArrayList<>();

    private JPanel filterListPanel;
    private JLabel sqlPreviewLabel;

    private FilterDialog(Frame parent, DataTable table) {
        super(parent, Lang.get("filter.title"), true);
        this.table = table;
        this.columnNames = table.getColumnNames();
        build();
        pack();

        setSize((int) (DialogBase.WIDTH * 1.4), DialogBase.HEIGHT);
        setMinimumSize(new Dimension(DialogBase.WIDTH, DialogBase.HEIGHT));
        setLocationRelativeTo(parent);
        setResizable(true);
        loadExistingFilters();
    }

    private void build() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(0, 0, 0, 0));

        JSplitPane splitPane = buildSplitPane();
        root.add(splitPane, BorderLayout.CENTER);
        root.add(buildDialogFooter(), BorderLayout.SOUTH);

        setContentPane(root);

        SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(0.5));
    }

    private JSplitPane buildSplitPane() {
        JPanel leftComponent = buildBuilderPanel();
        JPanel rightComponent = buildFilterListPanel();

        leftComponent.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Separator.foreground")));
        rightComponent.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Separator.foreground")));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftComponent, rightComponent);
        return splitPane;
    }

    private JPanel buildBuilderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(320, 0));

        panel.add(buildColumnAndLogicSection(), BorderLayout.NORTH);
        panel.add(buildConditionsSection(), BorderLayout.CENTER);
        panel.add(buildAddFilterButton(), BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildColumnAndLogicSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(10, 12, 10, 12));

        GridBagConstraints labelGbc = new GridBagConstraints();
        labelGbc.anchor = GridBagConstraints.WEST;
        labelGbc.insets = new Insets(3, 0, 3, 8);
        labelGbc.gridx = 0;

        GridBagConstraints fieldGbc = new GridBagConstraints();
        fieldGbc.fill = GridBagConstraints.HORIZONTAL;
        fieldGbc.weightx = 1.0;
        fieldGbc.insets = new Insets(3, 0, 3, 0);
        fieldGbc.gridx = 1;

        labelGbc.gridy = 0;
        fieldGbc.gridy = 0;
        panel.add(new JLabel(Lang.get("filter.column")), labelGbc);
        columnComboBox = new JComboBox<>(columnNames.toArray(new String[0]));
        panel.add(columnComboBox, fieldGbc);

        labelGbc.gridy = 1;
        fieldGbc.gridy = 1;
        panel.add(new JLabel(Lang.get("filter.logic_operator")), labelGbc);
        rowLogicComboBox = new JComboBox<>(new String[] {
                Lang.get("filter.logic_operator.and"),
                Lang.get("filter.logic_operator.or")
        });
        panel.add(rowLogicComboBox, fieldGbc);

        columnComboBox.addActionListener(event -> refreshAllConditionOperators());

        return panel;
    }

    private JPanel buildConditionsSection() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(BorderFactory.createTitledBorder(Lang.get("filter.added_conditions")));

        conditionRowsPanel = new JPanel();
        conditionRowsPanel.setLayout(new BoxLayout(conditionRowsPanel, BoxLayout.Y_AXIS));

        JButton addConditionButton = new JButton(Lang.get("filter.add_condition"));
        addConditionButton.addActionListener(event -> appendConditionInputRow(true));

        JPanel addConditionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        addConditionPanel.add(addConditionButton);

        JScrollPane scrollPane = new JScrollPane(conditionRowsPanel);
        scrollPane.setBorder(null);
        scrollPane.setPreferredSize(new Dimension(0, 240));

        wrapper.add(scrollPane, BorderLayout.CENTER);
        wrapper.add(addConditionPanel, BorderLayout.SOUTH);

        appendConditionInputRow(false);

        return wrapper;
    }

    private void appendConditionInputRow(boolean showLogicToggle) {
        DataType columnType = getSelectedColumnType();
        ConditionInputRow row = new ConditionInputRow(this, columnType, showLogicToggle, conditionInputRows.size());
        conditionInputRows.add(row);
        conditionRowsPanel.add(row.getPanel());
        conditionRowsPanel.revalidate();
        conditionRowsPanel.repaint();
    }

    public void removeConditionInputRow(ConditionInputRow row) {
        conditionInputRows.remove(row);
        conditionRowsPanel.remove(row.getPanel());
        conditionRowsPanel.revalidate();
        conditionRowsPanel.repaint();
    }

    private JPanel buildAddFilterButton() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(6, 12, 10, 12));

        JButton addFilterButton = new JButton(Lang.get("filter.add"));
        addFilterButton.addActionListener(event -> commitCurrentFilter());
        panel.add(addFilterButton, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildFilterListPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        panel.add(buildGlobalLogicRow(), BorderLayout.NORTH);

        filterListPanel = new JPanel();
        filterListPanel.setLayout(new BoxLayout(filterListPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(filterListPanel);
        scrollPane.setBorder(null);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildGlobalLogicRow() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Separator.foreground")));

        panel.add(new JLabel(Lang.get("filter.global_logic")));

        JComboBox<String> globalLogicComboBox = new JComboBox<>(new String[] {
                Lang.get("filter.logic_operator.and"),
                Lang.get("filter.logic_operator.or")
        });
        globalLogicComboBox.addActionListener(event -> {
            globalLogic = globalLogicComboBox.getSelectedIndex() == 0 ? FilterLogic.AND : FilterLogic.OR;
            refreshFilterList();
            updateSqlPreview();
        });
        panel.add(globalLogicComboBox);

        return panel;
    }

    private JPanel buildDialogFooter() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setBorder(new EmptyBorder(8, 12, 8, 12));

        sqlPreviewLabel = new JLabel();
        sqlPreviewLabel.setForeground(UIManager.getColor("Label.disabledForeground"));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));

        JButton cancelButton = new JButton(Lang.get("filter.cancel"));
        cancelButton.addActionListener(event -> {
            result.clear();
            dispose();
        });

        JButton confirmButton = new JButton(Lang.get("filter.confirm"));
        confirmButton.addActionListener(event -> dispose());

        buttonPanel.add(cancelButton);
        buttonPanel.add(confirmButton);

        panel.add(sqlPreviewLabel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.EAST);

        return panel;
    }

    private void refreshAllConditionOperators() {
        DataType newType = getSelectedColumnType();
        for (ConditionInputRow row : conditionInputRows) {
            row.updateColumnType(newType);
        }
    }

    private void commitCurrentFilter() {
        if (conditionInputRows.isEmpty()) {
            showValidationError(Lang.get("filter.validation.no_conditions"));
            return;
        }

        int selectedColumnIndex = columnComboBox.getSelectedIndex();
        FilterLogic selectedRowLogic = rowLogicComboBox.getSelectedIndex() == 0 ? FilterLogic.AND : FilterLogic.OR;
        Filter filter = new Filter(selectedColumnIndex);
        filter.setOperator(selectedRowLogic);

        try {
            for (ConditionInputRow inputRow : conditionInputRows) {
                FilterCondition condition = inputRow.buildCondition();
                if (condition == null) {
                    showValidationError(Lang.get("filter.validation.invalid_condition"));
                    return;
                }
                filter.add(condition);
            }
        } catch (ParseException ex) {
            showValidationError(Lang.get("filter.validation.invalid_number"));
            return;
        }

        result.add(filter);
        addFilterCard(filter);
        resetBuilder();
        updateSqlPreview();
    }

    private void resetBuilder() {
        conditionInputRows.clear();
        conditionRowsPanel.removeAll();
        conditionRowsPanel.revalidate();
        conditionRowsPanel.repaint();
        appendConditionInputRow(false);
    }

    private void addFilterCard(Filter filter) {
        FilterCardPanel card = new FilterCardPanel(this, filter, columnNames, filterCards.size() + 1);
        filterCards.add(card);
        refreshFilterList();
    }

    public void removeFilterCard(FilterCardPanel card) {
        result.remove(card.getFilter());
        filterCards.remove(card);
        refreshFilterList();
        updateSqlPreview();
    }

    public void editFilterCard(FilterCardPanel card) {
        int filterIndex = filterCards.indexOf(card);
        if (filterIndex < 0) {
            return;
        }
        Filter filter = card.getFilter();
        columnComboBox.setSelectedIndex(filter.getColumnIndex());
        rowLogicComboBox.setSelectedIndex(filter.getOperator() == FilterLogic.AND ? 0 : 1);

        conditionInputRows.clear();
        conditionRowsPanel.removeAll();

        List<FilterCondition> conditions = filter.getConditions();
        for (int i = 0; i < conditions.size(); i++) {
            FilterCondition condition = conditions.get(i);
            DataType columnType = table.getColumnType(filter.getColumnIndex());
            ConditionInputRow row = new ConditionInputRow(this, columnType, i > 0, i);
            row.loadCondition(condition);
            conditionInputRows.add(row);
            conditionRowsPanel.add(row.getPanel());
        }

        conditionRowsPanel.revalidate();
        conditionRowsPanel.repaint();

        result.remove(filter);
        filterCards.remove(card);
        refreshFilterList();
        updateSqlPreview();
    }

    private void refreshFilterList() {
        filterListPanel.removeAll();

        for (int i = 0; i < filterCards.size(); i++) {
            FilterCardPanel card = filterCards.get(i);
            card.setIndex(i + 1);

            if (i > 0) {
                JLabel separatorLabel = new JLabel("- " + globalLogic.name() + " -", SwingConstants.CENTER);
                separatorLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
                separatorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                separatorLabel.setBorder(new EmptyBorder(2, 0, 2, 0));
                filterListPanel.add(separatorLabel);
            }

            filterListPanel.add(card.getPanel());
        }

        filterListPanel.revalidate();
        filterListPanel.repaint();
    }

    private void updateSqlPreview() {
        if (result.isEmpty()) {
            sqlPreviewLabel.setText("");
            return;
        }

        StringBuilder sql = new StringBuilder("WHERE ");
        for (int i = 0; i < result.size(); i++) {
            Filter filter = result.get(i);
            String columnName = columnNames.get(filter.getColumnIndex());
            FilterSort sort = filter.getSort();

            if (sort != FilterSort.NONE) {
                sql.append("ORDER BY ").append(columnName).append(sort == FilterSort.ASCENDING ? " ASC" : " DESC");
            } else {
                sql.append("(").append(filter.getSQL(columnName)).append(")");
            }

            if (i < result.size() - 1) {
                sql.append(" ").append(globalLogic.name()).append(" ");
            }
        }

        sqlPreviewLabel.setText(sql.toString());
        sqlPreviewLabel.setToolTipText(sql.toString());
    }

    private void loadExistingFilters() {
        for (Filter filter : table.getFilters()) {
            result.add(filter);
            addFilterCard(filter);
        }
        updateSqlPreview();
    }

    private DataType getSelectedColumnType() {
        int selectedIndex = columnComboBox.getSelectedIndex();
        if (selectedIndex < 0) {
            return DataType.EMPTY;
        }
        return table.getColumnType(selectedIndex);
    }

    private void showValidationError(String message) {
        JOptionPane.showMessageDialog(this, message, Lang.get("filter.validation.error"), JOptionPane.WARNING_MESSAGE);
    }

    public DataTable getTable() {
        return table;
    }

    public List<String> getColumnNames() {
        return columnNames;
    }

    public List<Filter> getResult() {
        return result;
    }

    public static List<Filter> show(Frame parent, DataTable table) {
        FilterDialog dialog = new FilterDialog(parent, table);
        dialog.setVisible(true);
        return dialog.getResult();
    }

    public static JSpinner createIntegerSpinner() {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, null, null, 1));
        configureNumericEditor(spinner, "0", Integer.class);
        return spinner;
    }

    public static JSpinner createDoubleSpinner() {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(0.0, null, null, 0.1));
        configureNumericEditor(spinner, "0.###", Double.class);
        return spinner;
    }

    public static void configureNumericEditor(JSpinner spinner, String pattern, Class<? extends Number> valueClass) {
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, pattern);
        JFormattedTextField textField = editor.getTextField();
        textField.setColumns(10);
        JFormattedTextField.AbstractFormatter formatter = textField.getFormatter();
        if (formatter instanceof NumberFormatter numberFormatter) {
            numberFormatter.setValueClass(valueClass);
        }
        spinner.setEditor(editor);
    }

    public static JSpinner createDateSpinner() {
        JSpinner spinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, "yyyy-MM-dd HH:mm:ss");
        spinner.setEditor(editor);
        editor.getTextField().setColumns(16);
        return spinner;
    }

    public static int readIntegerSpinner(JSpinner spinner) throws ParseException {
        spinner.commitEdit();
        Object value = spinner.getValue();
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new ParseException("Invalid integer", 0);
    }

    public static double readDoubleSpinner(JSpinner spinner) throws ParseException {
        spinner.commitEdit();
        JSpinner.NumberEditor editor = (JSpinner.NumberEditor) spinner.getEditor();
        String text = editor.getTextField().getText();
        Number parsed = editor.getFormat().parse(text);
        if (parsed == null) {
            throw new ParseException("Invalid number", 0);
        }
        return parsed.doubleValue();
    }

    public static Timestamp readTimestampSpinner(JSpinner spinner) throws ParseException {
        spinner.commitEdit();
        JSpinner.DateEditor editor = (JSpinner.DateEditor) spinner.getEditor();
        String text = editor.getTextField().getText();
        Date parsed = editor.getFormat().parse(text);
        if (parsed == null) {
            throw new ParseException("Invalid date", 0);
        }
        return new Timestamp(parsed.getTime());
    }
}