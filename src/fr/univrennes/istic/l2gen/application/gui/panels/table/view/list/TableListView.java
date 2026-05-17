package fr.univrennes.istic.l2gen.application.gui.panels.table.view.list;

import fr.univrennes.istic.l2gen.application.core.config.Lang;
import fr.univrennes.istic.l2gen.application.core.services.table.TableService;
import fr.univrennes.istic.l2gen.application.core.table.DataTable;
import fr.univrennes.istic.l2gen.application.gui.GUIController;
import fr.univrennes.istic.l2gen.application.gui.panels.table.TablePanel;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

public final class TableListView extends JPanel {

    private final DefaultTableModel listModel;
    private final JTable tableList;
    private final JPopupMenu menu = new JPopupMenu();
    private final JMenuItem renameItem = new JMenuItem(Lang.get("table.list.menu.rename"));
    private final JMenuItem openLocationItem = new JMenuItem(Lang.get("table.list.menu.open_location"));
    private final JMenuItem removeRecentItem = new JMenuItem(Lang.get("table.list.menu.remove_recent"));
    private final JMenuItem deleteDiskItem = new JMenuItem(Lang.get("table.list.menu.delete_disk"));

    public TableListView(TablePanel panel) {
        super(new BorderLayout());
        this.listModel = new DefaultTableModel(new String[] {
                Lang.get("table.list.path"),
                Lang.get("table.list.alias"),
                Lang.get("table.list.rows"),
                Lang.get("table.list.columns"),
                Lang.get("table.list.size") }, 0) {

            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        this.tableList = new JTable(listModel);
        tableList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableList.setFillsViewportHeight(true);

        buildContextMenu();

        tableList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                showContextMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                showContextMenu(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int selectedRow = tableList.rowAtPoint(e.getPoint());
                    if (selectedRow < 0) {
                        return;
                    }
                    tableList.setRowSelectionInterval(selectedRow, selectedRow);
                    File path = (File) listModel.getValueAt(selectedRow, 0);
                    DataTable table = TableService.get(path);
                    if (table == null) {
                        refresh();
                        return;
                    }
                    GUIController.getInstance().setTable(table);
                }
            }
        });

        add(new JScrollPane(tableList), BorderLayout.CENTER);
    }

    public void refresh() {
        listModel.setRowCount(0);
        for (DataTable table : TableService.get()) {
            listModel.addRow(new Object[] {
                    table.getPath(),
                    table.getAlias(),
                    table.getRowCount(),
                    table.getColumnCount(),
                    formatSize(table.getPath().length())
            });
        }
    }

    private String formatSize(long size) {
        if (size < 1024) {
            return size + " B";
        }

        int exp = (int) (Math.log(size) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", size / Math.pow(1024, exp), pre);
    }

    public boolean isEmpty() {
        return listModel.getRowCount() == 0;
    }

    private void buildContextMenu() {
        renameItem.addActionListener(event -> renameSelectedTable());
        menu.add(renameItem);

        openLocationItem.addActionListener(event -> openSelectedLocation());
        menu.add(openLocationItem);

        menu.addSeparator();

        removeRecentItem.addActionListener(event -> removeSelectedRecent());
        menu.add(removeRecentItem);

        menu.addSeparator();

        deleteDiskItem.addActionListener(event -> deleteSelectedTable());
        menu.add(deleteDiskItem);
    }

    private void showContextMenu(MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }

        int row = tableList.rowAtPoint(e.getPoint());
        if (row < 0) {
            return;
        }

        tableList.setRowSelectionInterval(row, row);
        menu.show(tableList, e.getX(), e.getY());
    }

    private File getSelectedPath() {
        int row = tableList.getSelectedRow();
        if (row < 0) {
            return null;
        }
        return (File) listModel.getValueAt(row, 0);
    }

    private void renameSelectedTable() {
        File path = getSelectedPath();
        if (path == null) {
            return;
        }

        DataTable table = TableService.get(path);
        if (table == null) {
            refresh();
            return;
        }

        String newAlias = JOptionPane.showInputDialog(this,
                Lang.get("table.list.menu.rename.prompt"),
                table.getAlias());
        if (newAlias == null || newAlias.isBlank()) {
            return;
        }

        newAlias = newAlias.trim();
        if (newAlias.equals(table.getAlias())) {
            return;
        }

        File newPath = new File(path.getParentFile(), newAlias + ".parquet");
        table.setAlias(newAlias);
        if (newPath.exists() && !path.exists()) {
            TableService.updateLoadedPath(path, newPath, table);
            TableService.removeRecent(path);
            TableService.addRecent(newPath);
            TableService.saveRecents();
        }
        refresh();
    }

    private void openSelectedLocation() {
        File path = getSelectedPath();
        if (path == null) {
            return;
        }

        File parent = path.getParentFile();
        if (parent == null) {
            return;
        }

        try {
            if (!Desktop.isDesktopSupported()) {
                throw new IOException(Lang.get("table.list.menu.open_location.unsupported"));
            }

            Desktop.getDesktop().open(parent);
        } catch (Exception e) {
            GUIController.getInstance().onException(e);
        }
    }

    private void removeSelectedRecent() {
        File path = getSelectedPath();
        if (path == null) {
            return;
        }

        TableService.removeLoaded(path);
        TableService.removeRecent(path);
        TableService.saveRecents();
        refresh();
    }

    private void deleteSelectedTable() {
        File path = getSelectedPath();
        if (path == null) {
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                Lang.get("table.list.menu.delete.confirm", path.getName()),
                Lang.get("table.list.menu.delete.title"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        GUIController.getInstance().getTable().ifPresent(currentTable -> {
            if (path.equals(currentTable.getPath())) {
                GUIController.getInstance().closeTable();
            }
        });

        TableService.removeLoaded(path);
        TableService.removeRecent(path);
        TableService.saveRecents();

        if (path.exists() && !path.delete()) {
            GUIController.getInstance().onException(
                    new IOException(Lang.get("table.list.menu.delete.failed", path.getAbsolutePath())));
            return;
        }

        refresh();
    }
}