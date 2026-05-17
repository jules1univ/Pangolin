package fr.univrennes.istic.l2gen.application.gui.dialog.merge;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import fr.univrennes.istic.l2gen.application.core.table.DataTable;

public final class TableNameRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof DataTable dataTable) {
            setText(dataTable.getAlias());
        }
        return this;
    }
}