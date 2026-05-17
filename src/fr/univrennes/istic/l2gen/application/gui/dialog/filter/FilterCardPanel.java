package fr.univrennes.istic.l2gen.application.gui.dialog.filter;

import fr.univrennes.istic.l2gen.application.core.config.Lang;
import fr.univrennes.istic.l2gen.application.core.filter.Filter;
import fr.univrennes.istic.l2gen.application.core.filter.FilterCondition;
import fr.univrennes.istic.l2gen.application.core.filter.FilterOperator;
import fr.univrennes.istic.l2gen.application.core.filter.FilterSort;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.util.List;

public final class FilterCardPanel {

    private final FilterDialog owner;
    private final Filter filter;
    private final List<String> columnNames;
    private int filterIndex;

    private JPanel panel;
    private JLabel indexLabel;
    private boolean expanded = true;
    private JPanel bodyPanel;

    public FilterCardPanel(FilterDialog owner, Filter filter, List<String> columnNames, int filterIndex) {
        this.owner = owner;
        this.filter = filter;
        this.columnNames = columnNames;
        this.filterIndex = filterIndex;
        build();
    }

    private void build() {
        panel = new JPanel(new BorderLayout());
        panel.setBorder(new CompoundBorder(
                new EmptyBorder(3, 8, 3, 8),
                BorderFactory.createLineBorder(UIManager.getColor("Separator.foreground"), 1, true)));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        panel.add(buildHeader(), BorderLayout.NORTH);
        bodyPanel = buildBody();
        panel.add(bodyPanel, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(6, 0));
        header.setBorder(new EmptyBorder(6, 8, 6, 6));
        header.setBackground(UIManager.getColor("Panel.background"));

        JPanel leftSection = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        leftSection.setOpaque(false);

        indexLabel = new JLabel("F" + filterIndex);
        indexLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        leftSection.add(indexLabel);

        String columnName = columnNames.get(filter.getColumnIndex());
        JLabel columnLabel = new JLabel(columnName);
        columnLabel.setFont(columnLabel.getFont().deriveFont(Font.BOLD));
        leftSection.add(columnLabel);

        FilterSort sort = filter.getSort();
        if (sort == FilterSort.NONE && !filter.getConditions().isEmpty()) {
            String logicText = filter.getOperator().name();
            JLabel logicLabel = new JLabel(logicText);
            logicLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            logicLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UIManager.getColor("Separator.foreground")),
                    new EmptyBorder(1, 4, 1, 4)));
            leftSection.add(logicLabel);
        }

        JPanel rightSection = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        rightSection.setOpaque(false);

        JButton expandButton = new JButton(
                expanded ? UIManager.getIcon("Tree.expandedIcon") : UIManager.getIcon("Tree.collapsedIcon"));
        expandButton.setPreferredSize(new Dimension(26, 22));
        expandButton.setToolTipText(Lang.get("filter.toggle_expand"));
        expandButton.addActionListener(event -> {
            expanded = !expanded;
            bodyPanel.setVisible(expanded);
            expandButton.setIcon(
                    expanded ? UIManager.getIcon("Tree.expandedIcon") : UIManager.getIcon("Tree.collapsedIcon"));
            panel.revalidate();
            panel.repaint();
        });

        JButton editButton = new JButton(Lang.get("filter.edit"));
        editButton.setPreferredSize(new Dimension(60, 22));
        editButton.addActionListener(event -> owner.editFilterCard(this));

        JButton removeButton = new JButton(Lang.get("filter.remove"));
        removeButton.setPreferredSize(new Dimension(60, 22));
        removeButton.addActionListener(event -> owner.removeFilterCard(this));

        rightSection.add(expandButton);
        rightSection.add(editButton);
        rightSection.add(removeButton);

        header.add(leftSection, BorderLayout.CENTER);
        header.add(rightSection, BorderLayout.EAST);

        return header;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        body.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, UIManager.getColor("Separator.foreground")),
                new EmptyBorder(4, 8, 4, 8)));

        FilterSort sort = filter.getSort();
        if (sort != FilterSort.NONE) {
            String columnName = columnNames.get(filter.getColumnIndex());
            JLabel sortLabel = new JLabel("ORDER BY " + columnName + (sort == FilterSort.ASCENDING ? " ASC" : " DESC"));
            body.add(sortLabel);
            return body;
        }

        List<FilterCondition> conditions = filter.getConditions();
        for (int i = 0; i < conditions.size(); i++) {
            if (i > 0) {
                JLabel logicSeparator = new JLabel(filter.getOperator().name());
                logicSeparator.setForeground(UIManager.getColor("Label.disabledForeground"));
                body.add(logicSeparator);
            }

            FilterCondition condition = conditions.get(i);
            String conditionText = buildConditionText(condition);
            JLabel conditionLabel = new JLabel(conditionText);
            conditionLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UIManager.getColor("Separator.foreground")),
                    new EmptyBorder(2, 6, 2, 6)));
            body.add(conditionLabel);
        }

        return body;
    }

    private String buildConditionText(FilterCondition condition) {
        FilterOperator operator = condition.operator();
        String value = condition.value();

        return switch (operator) {
            case EQUAL -> "= " + value;
            case NOT_EQUAL -> "!= " + value;
            case LIKE -> "LIKE \"" + value + "\"";
            case GREATER -> "> " + value;
            case GREATER_EQUAL -> ">= " + value;
            case LESS -> "< " + value;
            case LESS_EQUAL -> "<= " + value;
            case IS_NULL -> "IS NULL";
            case NOT_NULL -> "IS NOT NULL";
            default -> operator.getSQL() + " " + (value != null ? value : "");
        };
    }

    public void setIndex(int newIndex) {
        this.filterIndex = newIndex;
        if (indexLabel != null) {
            indexLabel.setText("F" + newIndex);
        }
    }

    public Filter getFilter() {
        return filter;
    }

    public JPanel getPanel() {
        return panel;
    }
}