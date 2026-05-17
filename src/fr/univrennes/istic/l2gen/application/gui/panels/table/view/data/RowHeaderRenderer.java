package fr.univrennes.istic.l2gen.application.gui.panels.table.view.data;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

public final class RowHeaderRenderer extends JLabel implements ListCellRenderer<String> {

    private final TableRowHeader tableRowHeader;

    public RowHeaderRenderer(TableRowHeader tableRowHeader) {
        this.tableRowHeader = tableRowHeader;
        setOpaque(true);
        setHorizontalAlignment(SwingConstants.CENTER);
        setBorder(UIManager.getBorder("TableHeader.cellBorder"));

        setBackground(UIManager.getColor("TableHeader.background"));
        setForeground(this.tableRowHeader.tableView.getTableHeader().getForeground());
        setFont(this.tableRowHeader.tableView.getTableHeader().getFont());
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends String> list, String value,
            int index, boolean isSelected, boolean cellHasFocus) {
        setText(value);
        return this;
    }
}