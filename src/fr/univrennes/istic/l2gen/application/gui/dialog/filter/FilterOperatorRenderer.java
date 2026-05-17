package fr.univrennes.istic.l2gen.application.gui.dialog.filter;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import fr.univrennes.istic.l2gen.application.core.filter.FilterOperator;

public final class FilterOperatorRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
            boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof FilterOperator operator) {
            setText(operator.getSQL());
        }
        return this;
    }
}