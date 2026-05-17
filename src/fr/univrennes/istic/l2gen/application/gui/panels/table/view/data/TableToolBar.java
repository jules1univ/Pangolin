package fr.univrennes.istic.l2gen.application.gui.panels.table.view.data;

import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.UIManager;

import fr.univrennes.istic.l2gen.application.core.config.Config;

import fr.univrennes.istic.l2gen.application.core.config.Lang;
import fr.univrennes.istic.l2gen.application.core.services.table.TableService;
import fr.univrennes.istic.l2gen.application.gui.GUIController;
import fr.univrennes.istic.l2gen.application.gui.panels.table.TablePanel;

public final class TableToolBar extends JToolBar {

        private JButton advancedFilterButton;
        private JButton clearFiltersButton;

        private JButton subtableButton;
        private JButton mergetableButton;

        private JButton showAllColumnsButton;
        private JButton hideEmptyColumnsButton;

        private final TablePanel tablePanel;
        private final TableDataView tableView;

        public TableToolBar(TablePanel tablePanel, TableDataView tableView) {
                this.tablePanel = tablePanel;
                this.tableView = tableView;
                build();
        }

        private void build() {
                setFloatable(false);

                advancedFilterButton = new JButton(Lang.get("table.toolbar.filters"),
                                Config.getIcon("icons/filter_on.svg"));
                advancedFilterButton.addActionListener(e -> GUIController.getInstance().onOpenFilterDialog());

                clearFiltersButton = new JButton(Lang.get("table.toolbar.clear_filters"),
                                Config.getIcon("icons/filter_off.svg"));
                clearFiltersButton.addActionListener(e -> {
                        GUIController.getInstance().getTable().ifPresent(table -> {
                                table.clearFilters();
                                tablePanel.refresh();
                        });
                });
                clearFiltersButton.setVisible(
                                tableView.getTableModel().getTable().map(t -> !t.getFilters().isEmpty()).orElse(false));

                subtableButton = new JButton(Lang.get("table.toolbar.subtable"),
                                Config.getIcon("icons/subtable.svg"));
                subtableButton.addActionListener(e -> GUIController.getInstance().onOpenSubtableDialog());

                mergetableButton = new JButton(Lang.get("table.toolbar.mergetable"),
                                Config.getIcon("icons/merge.svg"));
                mergetableButton.addActionListener(e -> GUIController.getInstance().onOpenMergetableDialog());

                showAllColumnsButton = new JButton(Lang.get("table.toolbar.show_all_columns"),
                                Config.getIcon("icons/show.svg"));
                showAllColumnsButton.addActionListener(e -> {
                        tableView.showAllColumns();
                        showAllColumnsButton.setVisible(false);
                        hideEmptyColumnsButton.setVisible(true);
                });
                showAllColumnsButton.setVisible(tableView.hasHiddenColumns());

                hideEmptyColumnsButton = new JButton(Lang.get("table.toolbar.hide_empty_columns"),
                                Config.getIcon("icons/hide.svg"));
                hideEmptyColumnsButton.addActionListener(e -> {
                        tableView.hideEmptyColumns();
                        showAllColumnsButton.setVisible(true);
                        hideEmptyColumnsButton.setVisible(false);
                });
                hideEmptyColumnsButton.setVisible(!tableView.hasHiddenColumns());

                Icon closeIcon = UIManager.getIcon("InternalFrame.closeIcon");
                JButton closeButton = new JButton(closeIcon);
                closeButton.addActionListener(e -> {
                        boolean confirmOnClose = Config.getBoolean("settings.closing.confirm_on_table_close",
                                        false);
                        if (confirmOnClose) {
                                int result = JOptionPane.showConfirmDialog(GUIController.getInstance().getMainView(),
                                                Lang.get("table.toolbar.close.confirm.message"),
                                                Lang.get("table.toolbar.close.confirm.title"),
                                                JOptionPane.YES_NO_OPTION,
                                                JOptionPane.QUESTION_MESSAGE);
                                if (result != JOptionPane.YES_OPTION) {
                                        return;
                                }
                        }
                        GUIController.getInstance().closeTable();
                });

                add(advancedFilterButton);
                add(clearFiltersButton);
                addSeparator();
                add(subtableButton);
                addSeparator();
                add(mergetableButton);
                addSeparator();
                add(showAllColumnsButton);
                add(hideEmptyColumnsButton);
                add(Box.createHorizontalGlue());
                add(closeButton);
        }

        public void refresh() {
                showAllColumnsButton.setVisible(tableView.hasHiddenColumns());
                hideEmptyColumnsButton.setVisible(!tableView.hasHiddenColumns());
                clearFiltersButton.setVisible(
                                tableView.getTableModel().getTable().map(t -> !t.getFilters().isEmpty()).orElse(false));

                mergetableButton.setEnabled(TableService.get().size() > 1);
        }

        public JButton getFilterButton() {
                return advancedFilterButton;
        }

        public JButton getSubtableButton() {
                return subtableButton;
        }

        public JButton getHideEmptyColumnsButton() {
                return hideEmptyColumnsButton;
        }

        public JButton getShowAllColumnsButton() {
                return showAllColumnsButton;
        }
}