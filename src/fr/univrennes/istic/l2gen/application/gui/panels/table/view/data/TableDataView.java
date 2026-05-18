package fr.univrennes.istic.l2gen.application.gui.panels.table.view.data;

import fr.univrennes.istic.l2gen.application.core.config.Config;

import fr.univrennes.istic.l2gen.application.core.config.Lang;
import fr.univrennes.istic.l2gen.application.core.services.statistic.DateStatisticService;
import fr.univrennes.istic.l2gen.application.core.services.statistic.NumericStatisticService;
import fr.univrennes.istic.l2gen.application.core.services.statistic.StringStatisticService;
import fr.univrennes.istic.l2gen.application.core.table.DataTable;
import fr.univrennes.istic.l2gen.application.core.table.DataType;
import fr.univrennes.istic.l2gen.application.gui.GUIController;
import fr.univrennes.istic.l2gen.application.gui.panels.table.TablePanel;

import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

public final class TableDataView extends JPanel {

    private final JTable tableView;
    private final JScrollPane tableScrollPane;

    private final TableModel tableModel;
    private final TableRowHeader rowHeader;
    private final TableToolBar toolBar;
    private final TablePagination paginationBar;

    private TableColumnContextMenu columnContextMenu;

    private Set<Integer> hiddenViewIndex = new TreeSet<>();

    public TableDataView(TablePanel tablePanel) {
        super(new BorderLayout());

        tableModel = new TableModel(this);
        tableView = new JTable(tableModel);

        tableView.setShowGrid(true);
        tableView.setRowSelectionAllowed(false);
        tableView.setFillsViewportHeight(true);
        tableView.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tableView.setRowSorter(null);
        tableView.getTableHeader().setReorderingAllowed(false);

        TableDataView selfView = this;

        TableDataViewHeader headerRenderer = new TableDataViewHeader(tableView.getTableHeader().getDefaultRenderer());
        tableView.getTableHeader().setDefaultRenderer(headerRenderer);
        tableView.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int viewIndex = tableView.columnAtPoint(e.getPoint());
                if (viewIndex == -1) {
                    return;
                }

                int tableIndex = getViewToTableIndex(viewIndex);
                if (SwingUtilities.isRightMouseButton(e)) {
                    columnContextMenu = new TableColumnContextMenu(selfView, tableIndex);
                    columnContextMenu.show(tableView.getTableHeader(), e.getX(), e.getY());
                    columnContextMenu.addPopupMenuListener(new PopupMenuListener() {
                        @Override
                        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                        }

                        @Override
                        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                            columnContextMenu = null;
                        }

                        @Override
                        public void popupMenuCanceled(PopupMenuEvent e) {
                            columnContextMenu = null;
                        }
                    });
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    selectColumn(viewIndex);
                    onColumnSelected(tableIndex);
                }
            }
        });

        rowHeader = new TableRowHeader(tableView, tableModel);
        tableScrollPane = new JScrollPane(tableView);
        applyRowHeaderVisibility();

        paginationBar = new TablePagination(tableModel);
        toolBar = new TableToolBar(tablePanel, this);

        add(toolBar, BorderLayout.NORTH);
        add(tableScrollPane, BorderLayout.CENTER);
        add(paginationBar, BorderLayout.SOUTH);
    }

    public int getViewToTableIndex(int viewIndex) {
        int tableIndex = viewIndex;
        for (int hiddenColumn : hiddenViewIndex) {
            if (hiddenColumn <= tableIndex) {
                tableIndex++;
            } else {
                break;
            }
        }
        return tableIndex;
    }

    public int getTableToViewIndex(int tableIndex) {
        int viewIndex = tableIndex;
        for (int hiddenColumn : hiddenViewIndex) {
            if (hiddenColumn == tableIndex) {
                return -1;
            }
            if (hiddenColumn < tableIndex) {
                viewIndex--;
            } else {
                break;
            }
        }
        return viewIndex;
    }

    private void onColumnSelected(int tableIndex) {
        if (!Config.getBoolean("settings.table.columns.calculate_statistics", true)) {
            return;
        }
        GUIController.getInstance().getTable().ifPresent(table -> {
            GUIController.getInstance().getMainView().getBottomBar().clearColumnStats();
            GUIController.getInstance().enableLoading();

            new SwingWorker<List<Optional<String>>, Void>() {

                @Override
                protected List<Optional<String>> doInBackground() throws Exception {
                    DataType columnType = table.getColumnType(tableIndex);
                    Optional<String> min = Optional.empty();
                    Optional<String> max = Optional.empty();
                    Optional<String> avg = Optional.empty();
                    Optional<String> med = Optional.empty();
                    Optional<String> sum = Optional.empty();

                    if (columnType.isNumeric()) {
                        min = NumericStatisticService.getMin(table, tableIndex).map(String::valueOf);
                        max = NumericStatisticService.getMax(table, tableIndex).map(String::valueOf);
                        avg = NumericStatisticService.getMean(table, tableIndex).map(String::valueOf);
                        sum = NumericStatisticService.getSum(table, tableIndex).map(String::valueOf);
                    } else if (columnType.isDate()) {
                        min = DateStatisticService.getMin(table, tableIndex)
                                .map(v -> Lang.get("statistics.value.date", v.toString()));
                        max = DateStatisticService.getMax(table, tableIndex)
                                .map(v -> Lang.get("statistics.value.date", v.toString()));
                        med = DateStatisticService.getMedian(table, tableIndex)
                                .map(v -> Lang.get("statistics.value.date", v.toString()));
                    } else if (columnType == DataType.STRING) {
                        min = StringStatisticService.getMinLength(table, tableIndex)
                                .map(v -> Lang.get("statistics.value.length", v));
                        max = StringStatisticService.getMaxLength(table, tableIndex)
                                .map(v -> Lang.get("statistics.value.length", v));
                        avg = StringStatisticService.getMeanLength(table, tableIndex)
                                .map(v -> Lang.get("statistics.value.length", v));
                        sum = StringStatisticService.getSumLength(table, tableIndex)
                                .map(v -> Lang.get("statistics.value.length", v));
                    }

                    return List.of(min, max, avg, med, sum);
                }

                @Override
                protected void done() {
                    try {
                        Optional<String> min = get().get(0);
                        Optional<String> max = get().get(1);
                        Optional<String> avg = get().get(2);
                        Optional<String> med = get().get(3);
                        Optional<String> sum = get().get(4);

                        SwingUtilities.invokeLater(() -> {

                            GUIController.getInstance().getMainView().getBottomBar().setTableInfo(table.getAlias(),
                                    (int) table.getRowCount(), (int) table.getColumnCount());
                            GUIController.getInstance().getMainView().getBottomBar().setColumnStats(min, max, avg, med,
                                    sum);
                        });

                    } catch (Exception e) {
                        GUIController.getInstance().onException(e);
                    } finally {
                        GUIController.getInstance().disableLoading();
                    }
                }
            }.execute();
        });
    }

    public void open(DataTable table) {
        tableModel.open(table);
        paginationBar.refresh();
        toolBar.refresh();
        applyRowHeaderVisibility();
        adjustColumnWidths();

        if (Config.getBoolean("settings.table.columns.hide_empty", false)) {
            hideEmptyColumns();
        }
    }

    public void close() {
        tableModel.close();
        paginationBar.refresh();
        hiddenViewIndex.clear();
        applyRowHeaderVisibility();
    }

    public void refresh() {
        tableModel.fireTableDataChanged();
        paginationBar.refresh();
        toolBar.refresh();

        GUIController.getInstance().getTable()
                .ifPresent(table -> GUIController.getInstance().getMainView().getBottomBar()
                        .setTableInfo(table.getAlias(), (int) table.getRowCount(),
                                (int) table.getColumnCount()));

        updateHeaderIcons();

        applyRowHeaderVisibility();

        adjustColumnWidths();

    }

    public String getColumnName(int viewIndex) {
        TableColumnModel columnModel = tableView.getColumnModel();
        if (viewIndex < 0 || viewIndex >= columnModel.getColumnCount()) {
            return "";
        }
        return columnModel.getColumn(viewIndex).getHeaderValue().toString();
    }

    public void hideColumn(int viewIndex) {
        TableColumnModel columnModel = tableView.getColumnModel();
        if (viewIndex < 0 || viewIndex >= columnModel.getColumnCount()) {
            return;
        }
        int tableIndex = getViewToTableIndex(viewIndex);
        tableView.removeColumn(columnModel.getColumn(viewIndex));
        hiddenViewIndex.add(tableIndex);
        updateHeaderIcons();
    }

    public void renameColumn(int viewIndex, String newName) {
        TableColumnModel columnModel = tableView.getColumnModel();
        if (viewIndex < 0 || viewIndex >= columnModel.getColumnCount()) {
            return;
        }

        columnModel.getColumn(viewIndex).setHeaderValue(newName);
        tableView.getTableHeader().repaint();
    }

    public boolean hasHiddenColumns() {
        return !hiddenViewIndex.isEmpty();
    }

    public void showAllColumns() {
        hiddenViewIndex.clear();
        TableColumnModel columnModel = tableView.getColumnModel();
        while (columnModel.getColumnCount() > 0) {
            columnModel.removeColumn(columnModel.getColumn(0));
        }
        tableView.createDefaultColumnsFromModel();

        updateHeaderIcons();
        adjustColumnWidths();
    }

    public void hideEmptyColumns() {
        DataTable table = tableModel.getTable().orElse(null);
        if (table == null) {
            return;
        }

        for (int tableIndex = 0; tableIndex < table.getColumnCount(); tableIndex++) {
            if (table.getColumnType(tableIndex) == DataType.EMPTY) {
                hideColumn(getTableToViewIndex(tableIndex));
            }
        }
        updateHeaderIcons();
    }

    public void selectColumn(int viewIndex) {
        TableColumnModel columnModel = tableView.getColumnModel();
        if (viewIndex < 0 || viewIndex >= columnModel.getColumnCount()) {
            return;
        }
        tableView.setColumnSelectionAllowed(true);
        tableView.clearSelection();
        tableView.setColumnSelectionInterval(viewIndex, viewIndex);
        tableView.selectAll();
        tableView.setColumnSelectionInterval(viewIndex, viewIndex);
    }

    public int getSelectedColumnTableIndex() {
        int viewIndex = tableView.getSelectedColumn();
        if (viewIndex < 0) {
            return -1;
        }
        return getViewToTableIndex(viewIndex);
    }

    private void applyRowHeaderVisibility() {
        boolean showRowNumbers = Config.getBoolean("settings.table.show_row_numbers", false);
        if (showRowNumbers && tableModel.getTable().isPresent()) {
            rowHeader.refresh();
            tableScrollPane.setRowHeaderView(rowHeader.getComponent());
        } else {
            tableScrollPane.setRowHeaderView(null);
        }
    }

    private void updateHeaderIcons() {
        TableDataViewHeader headerRenderer = (TableDataViewHeader) tableView.getTableHeader().getDefaultRenderer();
        DataTable table = tableModel.getTable().orElse(null);
        if (table == null) {
            return;
        }

        Icon filterIcon = Config.getIcon("icons/filter_on.svg");
        for (int tableIndex = 0; tableIndex < table.getColumnCount(); tableIndex++) {
            int viewIndex = getTableToViewIndex(tableIndex);
            if (viewIndex == -1) {
                continue;
            }
            boolean hasFilter = table.getColumnFilters(tableIndex).size() > 0;

            if (hasFilter) {
                headerRenderer.setIcon(viewIndex, filterIcon);
            } else {
                headerRenderer.clearIcon(viewIndex);
            }
        }
        tableView.getTableHeader().repaint();
    }

    private void adjustColumnWidths() {
        if (!Config.getBoolean("settings.table.columns.auto_resize", true)) {
            return;
        }

        int sampleRowCount = Math.min(tableView.getRowCount(), 50);
        for (int viewIndex = 0; viewIndex < tableView.getColumnCount(); viewIndex++) {
            int maxWidth = 0;
            TableColumn tableColumn = tableView.getColumnModel().getColumn(viewIndex);

            TableCellRenderer headerRenderer = tableView.getTableHeader().getDefaultRenderer();
            Component headerComponent = headerRenderer.getTableCellRendererComponent(
                    tableView, tableColumn.getHeaderValue(), false, false, -1, viewIndex);
            maxWidth = Math.max(maxWidth, headerComponent.getPreferredSize().width);

            for (int rowIndex = 0; rowIndex < sampleRowCount; rowIndex++) {
                TableCellRenderer cellRenderer = tableView.getCellRenderer(rowIndex, viewIndex);
                Component cellComponent = cellRenderer.getTableCellRendererComponent(
                        tableView, tableView.getValueAt(rowIndex, viewIndex), false, false, rowIndex, viewIndex);
                maxWidth = Math.max(maxWidth, cellComponent.getPreferredSize().width);
            }

            tableColumn.setPreferredWidth(maxWidth + 16);
        }
    }

    public TableToolBar getToolBar() {
        return toolBar;
    }

    public TablePagination getPaginationBar() {
        return paginationBar;
    }

    public JTable getTableView() {
        return tableView;
    }

    public TableModel getTableModel() {
        return tableModel;
    }

    public TableColumnContextMenu getColumnContextMenu() {
        return columnContextMenu;
    }

}