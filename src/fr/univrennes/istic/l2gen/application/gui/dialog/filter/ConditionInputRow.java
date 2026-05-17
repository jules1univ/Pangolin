package fr.univrennes.istic.l2gen.application.gui.dialog.filter;

import fr.univrennes.istic.l2gen.application.core.config.Lang;
import fr.univrennes.istic.l2gen.application.core.filter.FilterCondition;
import fr.univrennes.istic.l2gen.application.core.filter.FilterLogic;
import fr.univrennes.istic.l2gen.application.core.filter.FilterOperator;
import fr.univrennes.istic.l2gen.application.core.table.DataType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Timestamp;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ConditionInputRow {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final FilterOperator[] STRING_OPERATORS = {
            FilterOperator.EQUAL, FilterOperator.LIKE, FilterOperator.NOT_EQUAL,
            FilterOperator.IS_NULL, FilterOperator.NOT_NULL
    };
    private static final FilterOperator[] NUMERIC_OPERATORS = {
            FilterOperator.EQUAL, FilterOperator.NOT_EQUAL,
            FilterOperator.GREATER, FilterOperator.GREATER_EQUAL,
            FilterOperator.LESS, FilterOperator.LESS_EQUAL,
            FilterOperator.IS_NULL, FilterOperator.NOT_NULL
    };
    private static final FilterOperator[] DATE_OPERATORS = {
            FilterOperator.EQUAL, FilterOperator.NOT_EQUAL,
            FilterOperator.GREATER, FilterOperator.GREATER_EQUAL,
            FilterOperator.LESS, FilterOperator.LESS_EQUAL,
            FilterOperator.IS_NULL, FilterOperator.NOT_NULL
    };
    private static final FilterOperator[] BOOLEAN_OPERATORS = {
            FilterOperator.EQUAL, FilterOperator.IS_NULL, FilterOperator.NOT_NULL
    };

    private final FilterDialog owner;
    private DataType columnType;
    private final boolean showLogicToggle;

    private JPanel panel;
    private JComboBox<String> logicComboBox;
    private JComboBox<FilterOperator> operatorComboBox;
    private JPanel valuePanel;
    private CardLayout valueCardLayout;

    private static final String CARD_TEXT = "TEXT";
    private static final String CARD_NUMERIC = "NUMERIC";
    private static final String CARD_DATE = "DATE";
    private static final String CARD_BOOLEAN = "BOOLEAN";
    private static final String CARD_NONE = "NONE";

    private JTextField textValueField;
    private JSpinner numericValueSpinner;
    private JSpinner dateValueSpinner;
    private JComboBox<String> booleanValueComboBox;

    public ConditionInputRow(FilterDialog owner, DataType columnType, boolean showLogicToggle, int rowIndex) {
        this.owner = owner;
        this.columnType = columnType;
        this.showLogicToggle = showLogicToggle;
        buildPanel();
    }

    private void buildPanel() {
        panel = new JPanel(new BorderLayout(4, 0));
        panel.setBorder(new EmptyBorder(3, 6, 3, 6));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JPanel leftSection = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        leftSection.setOpaque(false);

        if (showLogicToggle) {
            logicComboBox = new JComboBox<>(new String[] {
                    Lang.get("filter.logic_operator.and"),
                    Lang.get("filter.logic_operator.or")
            });
            logicComboBox.setPreferredSize(new Dimension(60, 26));
            leftSection.add(logicComboBox);
        } else {
            JLabel placeholderLabel = new JLabel();
            placeholderLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            placeholderLabel.setPreferredSize(new Dimension(60, 26));
            placeholderLabel.setHorizontalAlignment(SwingConstants.CENTER);
            leftSection.add(placeholderLabel);
        }

        operatorComboBox = new JComboBox<>(buildOperatorModels(columnType));
        operatorComboBox.setRenderer(new FilterOperatorRenderer());
        operatorComboBox.setPreferredSize(new Dimension(120, 26));
        operatorComboBox.addActionListener(event -> refreshValueVisibility());
        leftSection.add(operatorComboBox);

        valueCardLayout = new CardLayout();
        valuePanel = new JPanel(valueCardLayout);
        valuePanel.setOpaque(false);

        textValueField = new JTextField();
        textValueField.setPreferredSize(new Dimension(120, 26));
        valuePanel.add(textValueField, CARD_TEXT);

        numericValueSpinner = FilterDialog.createDoubleSpinner();
        valuePanel.add(numericValueSpinner, CARD_NUMERIC);

        dateValueSpinner = FilterDialog.createDateSpinner();
        valuePanel.add(dateValueSpinner, CARD_DATE);

        booleanValueComboBox = new JComboBox<>(new String[] { "true", "false" });
        valuePanel.add(booleanValueComboBox, CARD_BOOLEAN);

        valuePanel.add(new JLabel(), CARD_NONE);

        JButton removeButton = new JButton(UIManager.getIcon("InternalFrame.closeIcon"));
        removeButton.setPreferredSize(new Dimension(26, 26));
        removeButton.setToolTipText(Lang.get("filter.remove_condition"));
        removeButton.addActionListener(event -> owner.removeConditionInputRow(this));

        panel.add(leftSection, BorderLayout.WEST);
        panel.add(valuePanel, BorderLayout.CENTER);
        panel.add(removeButton, BorderLayout.EAST);

        refreshValueVisibility();
    }

    private FilterOperator[] buildOperatorModels(DataType type) {
        return switch (type) {
            case STRING -> STRING_OPERATORS;
            case INTEGER, DOUBLE -> NUMERIC_OPERATORS;
            case DATE -> DATE_OPERATORS;
            case BOOLEAN -> BOOLEAN_OPERATORS;
            case EMPTY -> new FilterOperator[] {};
        };
    }

    private void refreshValueVisibility() {
        FilterOperator selectedOperator = (FilterOperator) operatorComboBox.getSelectedItem();
        if (selectedOperator == null) {
            return;
        }

        boolean noValueNeeded = selectedOperator == FilterOperator.IS_NULL
                || selectedOperator == FilterOperator.NOT_NULL;

        if (noValueNeeded) {
            valueCardLayout.show(valuePanel, CARD_NONE);
            return;
        }

        String card = switch (columnType) {
            case INTEGER, DOUBLE -> CARD_NUMERIC;
            case DATE -> CARD_DATE;
            case BOOLEAN -> CARD_BOOLEAN;
            default -> CARD_TEXT;
        };

        valueCardLayout.show(valuePanel, card);
    }

    public void updateColumnType(DataType newType) {
        this.columnType = newType;
        FilterOperator[] operators = buildOperatorModels(newType);
        operatorComboBox.setModel(new DefaultComboBoxModel<>(operators));
        refreshValueVisibility();
    }

    public void loadCondition(FilterCondition condition) {
        FilterOperator operator = condition.operator();
        for (int i = 0; i < operatorComboBox.getItemCount(); i++) {
            if (operatorComboBox.getItemAt(i) == operator) {
                operatorComboBox.setSelectedIndex(i);
                break;
            }
        }

        String value = condition.value();
        if (value == null || value.isEmpty()) {
            return;
        }

        switch (columnType) {
            case INTEGER, DOUBLE -> {
                try {
                    numericValueSpinner.setValue(Double.parseDouble(value));
                } catch (NumberFormatException ignored) {
                }
            }
            case DATE -> {
                try {
                    LocalDateTime ldt = LocalDateTime.parse(value, TIMESTAMP_FORMATTER);
                    dateValueSpinner.setValue(Timestamp.valueOf(ldt));
                } catch (Exception ignored) {
                }
            }
            case BOOLEAN -> {
                booleanValueComboBox.setSelectedItem(value);
            }
            default -> {
                textValueField.setText(value);
            }
        }

        refreshValueVisibility();
    }

    public FilterCondition buildCondition() throws ParseException {
        FilterOperator selectedOperator = (FilterOperator) operatorComboBox.getSelectedItem();
        if (selectedOperator == null) {
            return null;
        }

        if (selectedOperator == FilterOperator.IS_NULL || selectedOperator == FilterOperator.NOT_NULL) {
            return new FilterCondition(selectedOperator);
        }

        String value = readCurrentValue();
        if (value == null) {
            return null;
        }

        return new FilterCondition(selectedOperator, value);
    }

    private String readCurrentValue() throws ParseException {
        return switch (columnType) {
            case INTEGER -> String.valueOf(FilterDialog.readIntegerSpinner(numericValueSpinner));
            case DOUBLE -> String.valueOf(FilterDialog.readDoubleSpinner(numericValueSpinner));
            case DATE -> {
                Timestamp timestamp = FilterDialog.readTimestampSpinner(dateValueSpinner);
                yield timestamp.toLocalDateTime().format(TIMESTAMP_FORMATTER);
            }
            case BOOLEAN -> (String) booleanValueComboBox.getSelectedItem();
            default -> {
                String text = textValueField.getText().trim();
                yield text.isEmpty() ? null : text;
            }
        };
    }

    public FilterLogic getSelectedLogic() {
        if (logicComboBox == null) {
            return FilterLogic.AND;
        }
        return logicComboBox.getSelectedIndex() == 0 ? FilterLogic.AND : FilterLogic.OR;
    }

    public JPanel getPanel() {
        return panel;
    }
}