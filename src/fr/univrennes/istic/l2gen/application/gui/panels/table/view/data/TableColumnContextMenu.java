package fr.univrennes.istic.l2gen.application.gui.panels.table.view.data;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingWorker;

import fr.univrennes.istic.l2gen.application.core.TaskStatus;
import fr.univrennes.istic.l2gen.application.core.config.Config;
import fr.univrennes.istic.l2gen.application.core.config.Lang;
import fr.univrennes.istic.l2gen.application.core.filter.Filter;
import fr.univrennes.istic.l2gen.application.core.notebook.NoteBookText;
import fr.univrennes.istic.l2gen.application.core.services.notebook.NoteBookService;
import fr.univrennes.istic.l2gen.application.core.services.statistic.StringStatisticService;
import fr.univrennes.istic.l2gen.application.core.services.statistic.DateStatisticService;
import fr.univrennes.istic.l2gen.application.core.services.statistic.NumericStatisticService;
import fr.univrennes.istic.l2gen.application.core.services.statistic.StatisticService;
import fr.univrennes.istic.l2gen.application.core.table.DataTable;
import fr.univrennes.istic.l2gen.application.core.table.DataType;
import fr.univrennes.istic.l2gen.application.gui.GUIController;
import fr.univrennes.istic.l2gen.application.gui.dialog.input.InputDateDialog;
import fr.univrennes.istic.l2gen.application.gui.dialog.input.InputDoubleDialog;
import fr.univrennes.istic.l2gen.application.gui.dialog.input.InputIntDialog;
import fr.univrennes.istic.l2gen.application.gui.dialog.input.InputSelectDialog;
import fr.univrennes.istic.l2gen.application.gui.dialog.input.InputStringDialog;
import fr.univrennes.istic.l2gen.application.gui.dialog.stats.StatisticsDialog;

public final class TableColumnContextMenu extends JPopupMenu {
        private static final int MAX_CATEGORIES = 25;

        private final DataTable table;
        private final int tableIndex;
        private final DataType columnType;
        private final TableDataView tableView;

        private final JMenu sortMenu;

        public TableColumnContextMenu(TableDataView tableView, int tableIndex) {
                this.table = tableView.getTableModel().getTable().get();
                this.tableView = tableView;
                this.tableIndex = tableIndex;
                this.columnType = table.getColumnType(tableIndex);

                sortMenu = buildSortMenu();
                add(sortMenu);
                addSeparator();
                add(buildFilterMenu());
                addSeparator();
                add(buildStatsMenu());
                addSeparator();

                if (Config.getBoolean("settings.table.manual_typing", true)) {
                        JMenu changeTypeItem = new JMenu(Lang.get("table.column.menu.change_type"));
                        for (DataType type : DataType.values()) {
                                String typeDisplayName = Lang.get("table.type." + type.name().toLowerCase());
                                JMenuItem typeItem = new JMenuItem(typeDisplayName);
                                typeItem.addActionListener(e -> {
                                        int confirm = JOptionPane.showConfirmDialog(tableView,
                                                        Lang.get("table.column.menu.change_type.confirm.message",
                                                                        tableView.getTableView()
                                                                                        .getColumnName(tableIndex),
                                                                        typeDisplayName),
                                                        Lang.get("table.column.menu.change_type.confirm.title"),
                                                        JOptionPane.YES_NO_OPTION);
                                        if (confirm == JOptionPane.YES_OPTION) {
                                                String taskId = GUIController.getInstance().addTask(
                                                                Lang.get("task.table.change_type",
                                                                                tableView.getTableView().getColumnName(
                                                                                                tableIndex),
                                                                                typeDisplayName),
                                                                TaskStatus.RUNNING);

                                                boolean success = table.setColumnType(tableIndex, type);
                                                GUIController.getInstance().updateTaskStatus(taskId,
                                                                success ? TaskStatus.SUCCESS : TaskStatus.FAILED);
                                        }
                                });
                                changeTypeItem.add(typeItem);
                        }
                        add(changeTypeItem);
                }

                JMenuItem renameColumnItem = new JMenuItem(Lang.get("table.column.menu.rename"));
                renameColumnItem.addActionListener(e -> {
                        String newName = JOptionPane.showInputDialog(tableView,
                                        Lang.get("table.column.menu.rename.prompt"),
                                        tableView.getColumnName(tableIndex));
                        if (newName != null && !newName.isBlank()) {
                                tableView.renameColumn(tableView.getTableToViewIndex(tableIndex), newName);
                                tableView.refresh();
                        }
                });
                add(renameColumnItem);

                JMenuItem hideColumnItem = new JMenuItem(Lang.get("table.column.menu.hide"));
                hideColumnItem.addActionListener(e -> tableView.hideColumn(tableView.getTableToViewIndex(tableIndex)));

                add(hideColumnItem);
        }

        public JMenu getSortMenu() {
                return sortMenu;
        }

        private JMenu buildSortMenu() {
                JMenu sortMenu = new JMenu(Lang.get("table.column.menu.sort"));

                JMenuItem sortAscendingItem = new JMenuItem(Lang.get("table.column.menu.sort.ascending"));
                sortAscendingItem.addActionListener(e -> {
                        table.clearFilters();
                        table.addFilter(Filter.sort(tableIndex, true));
                        GUIController.getInstance().getMainView().getTablePanel().refresh();
                });

                JMenuItem sortDescendingItem = new JMenuItem(Lang.get("table.column.menu.sort.descending"));
                sortDescendingItem
                                .addActionListener(e -> {
                                        table.clearFilters();
                                        table.addFilter(Filter.sort(tableIndex, false));
                                        GUIController.getInstance().getMainView().getTablePanel().refresh();
                                });

                sortMenu.add(sortAscendingItem);
                sortMenu.add(sortDescendingItem);
                return sortMenu;
        }

        private JMenu buildFilterMenu() {
                JMenu filterMenu = new JMenu(Lang.get("table.column.menu.filter"));

                JMenuItem filterTopNItem = new JMenuItem(Lang.get("table.column.menu.filter.topn"));
                filterTopNItem.addActionListener(e -> {
                        try {
                                switch (columnType) {
                                        case STRING -> {
                                                Optional<Integer> min = StringStatisticService.getMinLength(table,
                                                                tableIndex);
                                                Optional<Integer> max = StringStatisticService.getMaxLength(table,
                                                                tableIndex);
                                                if (min.isEmpty() || max.isEmpty()) {
                                                        int length = InputIntDialog.show(
                                                                        Lang.get("table.column.menu.filter.length"),
                                                                        Lang.get("table.column.menu.filter.topn"),
                                                                        Lang.get("table.column.menu.filter.length.error"))
                                                                        .get();
                                                        if (length > 0) {
                                                                table.addFilter(Filter.topN(tableIndex, length));
                                                        }
                                                        return;
                                                }

                                                int length = InputIntDialog.show(
                                                                Lang.get("table.column.menu.filter.length"),
                                                                Lang.get("table.column.menu.filter.topn"),
                                                                Lang.get("table.column.menu.filter.length.error"),
                                                                min.get(), max.get())
                                                                .get();
                                                if (length > 0) {
                                                        table.addFilter(Filter.topN(tableIndex, length));
                                                }
                                        }
                                        case INTEGER, DOUBLE -> {
                                                Optional<Double> min = NumericStatisticService.getMin(table,
                                                                tableIndex);
                                                Optional<Double> max = NumericStatisticService.getMax(table,
                                                                tableIndex);
                                                if (min.isEmpty() || max.isEmpty()) {
                                                        double value = InputDoubleDialog.show(
                                                                        Lang.get("table.column.menu.filter.double"),
                                                                        Lang.get("table.column.menu.filter.topn"),
                                                                        Lang.get("table.column.menu.filter.double.error"))
                                                                        .get();
                                                        table.addFilter(Filter.topN(tableIndex, value));
                                                        return;
                                                }
                                                double value = InputDoubleDialog.show(
                                                                Lang.get("table.column.menu.filter.double"),
                                                                Lang.get("table.column.menu.filter.topn"),
                                                                Lang.get("table.column.menu.filter.double.error"),
                                                                min.get(), max.get())
                                                                .get();
                                                table.addFilter(Filter.topN(tableIndex, value));
                                        }
                                        case DATE -> {
                                                Optional<java.sql.Timestamp> min = DateStatisticService
                                                                .getMin(table, tableIndex);
                                                Optional<java.sql.Timestamp> max = DateStatisticService
                                                                .getMax(table, tableIndex);
                                                if (min.isEmpty() || max.isEmpty()) {
                                                        java.util.Date date = InputDateDialog.show(
                                                                        Lang.get("table.column.menu.filter.date"),
                                                                        Lang.get("table.column.menu.filter.topn"),
                                                                        Lang.get("table.column.menu.filter.date.error"))
                                                                        .get();
                                                        table.addFilter(Filter.topN(tableIndex,
                                                                        new Timestamp(date.getTime())));
                                                        return;
                                                }
                                                java.util.Date date = InputDateDialog.show(
                                                                Lang.get("table.column.menu.filter.date"),
                                                                Lang.get("table.column.menu.filter.topn"),
                                                                Lang.get("table.column.menu.filter.date.error"),
                                                                min.get(),
                                                                max.get())
                                                                .get();

                                                Timestamp sqlDate = new Timestamp(date.getTime());
                                                table.addFilter(Filter.topN(tableIndex, sqlDate));
                                        }
                                        default -> {
                                        }
                                }
                                tableView.refresh();
                        } catch (Exception ignored) {
                        }

                });

                JMenuItem filterBottomNItem = new JMenuItem(Lang.get("table.column.menu.filter.bottomn"));
                filterBottomNItem.addActionListener(e -> {
                        try {
                                switch (columnType) {
                                        case STRING -> {
                                                Optional<Integer> min = StringStatisticService.getMinLength(table,
                                                                tableIndex);
                                                Optional<Integer> max = StringStatisticService.getMaxLength(table,
                                                                tableIndex);
                                                if (min.isEmpty() || max.isEmpty()) {
                                                        int length = InputIntDialog.show(
                                                                        Lang.get("table.column.menu.filter.length"),
                                                                        Lang.get("table.column.menu.filter.bottomn"),
                                                                        Lang.get("table.column.menu.filter.length.error"))
                                                                        .get();
                                                        if (length > 0) {
                                                                table.addFilter(Filter.bottomN(tableIndex, length));
                                                        }
                                                        return;
                                                }
                                                int length = InputIntDialog.show(
                                                                Lang.get("table.column.menu.filter.length"),
                                                                Lang.get("table.column.menu.filter.bottomn"),
                                                                Lang.get("table.column.menu.filter.length.error"),
                                                                min.get(), max.get())
                                                                .get();
                                                if (length > 0) {
                                                        table.addFilter(Filter.bottomN(tableIndex, length));
                                                }
                                        }
                                        case INTEGER, DOUBLE -> {
                                                Optional<Double> min = NumericStatisticService.getMin(table,
                                                                tableIndex);
                                                Optional<Double> max = NumericStatisticService.getMax(table,
                                                                tableIndex);
                                                if (min.isEmpty() || max.isEmpty()) {
                                                        double value = InputDoubleDialog.show(
                                                                        Lang.get("table.column.menu.filter.double"),
                                                                        Lang.get("table.column.menu.filter.bottomn"),
                                                                        Lang.get("table.column.menu.filter.double.error"))
                                                                        .get();
                                                        table.addFilter(Filter.bottomN(tableIndex, value));
                                                        return;
                                                }
                                                double value = InputDoubleDialog.show(
                                                                Lang.get("table.column.menu.filter.double"),
                                                                Lang.get("table.column.menu.filter.bottomn"),
                                                                Lang.get("table.column.menu.filter.double.error"),
                                                                min.get(), max.get())
                                                                .get();
                                                table.addFilter(Filter.bottomN(tableIndex, value));
                                        }
                                        case DATE -> {
                                                Optional<java.sql.Timestamp> min = DateStatisticService
                                                                .getMin(table, tableIndex);
                                                Optional<java.sql.Timestamp> max = DateStatisticService
                                                                .getMax(table, tableIndex);
                                                if (min.isEmpty() || max.isEmpty()) {
                                                        java.util.Date date = InputDateDialog.show(
                                                                        Lang.get("table.column.menu.filter.date"),
                                                                        Lang.get("table.column.menu.filter.bottomn"),
                                                                        Lang.get("table.column.menu.filter.date.error"))
                                                                        .get();
                                                        table.addFilter(Filter.bottomN(tableIndex,
                                                                        new Timestamp(date.getTime())));
                                                        return;
                                                }
                                                java.util.Date date = InputDateDialog.show(
                                                                Lang.get("table.column.menu.filter.date"),
                                                                Lang.get("table.column.menu.filter.bottomn"),
                                                                Lang.get("table.column.menu.filter.date.error"),
                                                                min.get(),
                                                                max.get())
                                                                .get();

                                                Timestamp sqlDate = new Timestamp(date.getTime());
                                                table.addFilter(Filter.bottomN(tableIndex, sqlDate));
                                        }
                                        default -> {
                                        }
                                }

                                tableView.refresh();
                        } catch (Exception ignored) {
                        }
                });

                JMenuItem filterNumericRangeItem = new JMenuItem(Lang.get("table.column.menu.filter.range"));
                filterNumericRangeItem.addActionListener(e -> {
                        try {
                                switch (columnType) {
                                        case STRING -> {
                                                Optional<Integer> min = StringStatisticService.getMinLength(table,
                                                                tableIndex);
                                                Optional<Integer> max = StringStatisticService.getMaxLength(table,
                                                                tableIndex);
                                                if (min.isEmpty() || max.isEmpty()) {
                                                        int minLength = InputIntDialog.show(
                                                                        Lang.get("table.column.menu.filter.length"),
                                                                        Lang.get("table.column.menu.filter.range.min"),
                                                                        Lang.get("table.column.menu.filter.length.error"))
                                                                        .get();
                                                        int maxLength = InputIntDialog.show(

                                                                        Lang.get("table.column.menu.filter.length"),
                                                                        Lang.get("table.column.menu.filter.range.max"),
                                                                        Lang.get("table.column.menu.filter.length.error"))
                                                                        .get();
                                                        maxLength = Math.max(maxLength, minLength);
                                                        minLength = Math.min(minLength, maxLength);
                                                        table.addFilter(Filter.byRange(tableIndex, minLength,
                                                                        maxLength));
                                                        return;
                                                }
                                                int minLength = InputIntDialog.show(
                                                                Lang.get("table.column.menu.filter.length"),
                                                                Lang.get("table.column.menu.filter.range.min"),
                                                                Lang.get("table.column.menu.filter.length.error"),
                                                                min.get(), max.get())
                                                                .get();
                                                int maxLength = InputIntDialog.show(
                                                                Lang.get("table.column.menu.filter.length"),
                                                                Lang.get("table.column.menu.filter.range.max"),
                                                                Lang.get("table.column.menu.filter.length.error"),
                                                                min.get(), max.get())
                                                                .get();
                                                maxLength = Math.max(maxLength, minLength);
                                                minLength = Math.min(minLength, maxLength);
                                                table.addFilter(Filter.byRange(tableIndex, minLength, maxLength));
                                        }
                                        case INTEGER, DOUBLE -> {
                                                Optional<Double> min = NumericStatisticService.getMin(table,
                                                                tableIndex);
                                                Optional<Double> max = NumericStatisticService.getMax(table,
                                                                tableIndex);
                                                if (min.isEmpty() || max.isEmpty()) {
                                                        double minValue = InputDoubleDialog.show(
                                                                        Lang.get("table.column.menu.filter.double"),
                                                                        Lang.get("table.column.menu.filter.range.min"),
                                                                        Lang.get("table.column.menu.filter.double.error"))
                                                                        .get();
                                                        double maxValue = InputDoubleDialog.show(
                                                                        Lang.get("table.column.menu.filter.double"),
                                                                        Lang.get("table.column.menu.filter.range.max"),
                                                                        Lang.get("table.column.menu.filter.double.error"))
                                                                        .get();
                                                        maxValue = Math.max(maxValue, minValue);
                                                        minValue = Math.min(minValue, maxValue);
                                                        table.addFilter(Filter.byRange(tableIndex, minValue, maxValue));
                                                        return;
                                                }
                                                double minValue = InputDoubleDialog.show(
                                                                Lang.get("table.column.menu.filter.double"),
                                                                Lang.get("table.column.menu.filter.range.min"),
                                                                Lang.get("table.column.menu.filter.double.error"),
                                                                min.get(), max.get())
                                                                .get();
                                                double maxValue = InputDoubleDialog.show(
                                                                Lang.get("table.column.menu.filter.double"),
                                                                Lang.get("table.column.menu.filter.range.max"),
                                                                Lang.get("table.column.menu.filter.double.error"),
                                                                min.get(), max.get())
                                                                .get();
                                                maxValue = Math.max(maxValue, minValue);
                                                minValue = Math.min(minValue, maxValue);
                                                table.addFilter(Filter.byRange(tableIndex, minValue, maxValue));
                                        }
                                        case DATE -> {
                                                Optional<java.sql.Timestamp> min = DateStatisticService
                                                                .getMin(table, tableIndex);
                                                Optional<java.sql.Timestamp> max = DateStatisticService
                                                                .getMax(table, tableIndex);
                                                if (min.isEmpty() || max.isEmpty()) {
                                                        java.util.Date minDate = InputDateDialog.show(
                                                                        Lang.get("table.column.menu.filter.date"),
                                                                        Lang.get("table.column.menu.filter.range.min"),
                                                                        Lang.get("table.column.menu.filter.date.error"))
                                                                        .get();
                                                        java.util.Date maxDate = InputDateDialog.show(
                                                                        Lang.get("table.column.menu.filter.date"),
                                                                        Lang.get("table.column.menu.filter.range.max"),
                                                                        Lang.get("table.column.menu.filter.date.error"))
                                                                        .get();
                                                        Timestamp sqlMinDate = new Timestamp(minDate.getTime());
                                                        Timestamp sqlMaxDate = new Timestamp(maxDate.getTime());
                                                        sqlMaxDate = sqlMaxDate.after(sqlMinDate) ? sqlMaxDate
                                                                        : sqlMinDate;
                                                        sqlMinDate = sqlMinDate.before(sqlMaxDate) ? sqlMinDate
                                                                        : sqlMaxDate;
                                                        table.addFilter(Filter.byRange(tableIndex, sqlMinDate,
                                                                        sqlMaxDate));
                                                        return;
                                                }
                                                java.util.Date minDate = InputDateDialog.show(
                                                                Lang.get("table.column.menu.filter.date"),
                                                                Lang.get("table.column.menu.filter.range.min"),
                                                                Lang.get("table.column.menu.filter.date.error"),
                                                                min.get(), max.get())
                                                                .get();
                                                java.util.Date maxDate = InputDateDialog.show(
                                                                Lang.get("table.column.menu.filter.date"),
                                                                Lang.get("table.column.menu.filter.range.max"),
                                                                Lang.get("table.column.menu.filter.date.error"),
                                                                min.get(), max.get())
                                                                .get();
                                                Timestamp sqlMinDate = new Timestamp(minDate.getTime());
                                                Timestamp sqlMaxDate = new Timestamp(maxDate.getTime());
                                                sqlMaxDate = sqlMaxDate.after(sqlMinDate) ? sqlMaxDate : sqlMinDate;
                                                sqlMinDate = sqlMinDate.before(sqlMaxDate) ? sqlMinDate : sqlMaxDate;
                                                table.addFilter(Filter.byRange(tableIndex, sqlMinDate, sqlMaxDate));
                                        }
                                        default -> {
                                        }
                                }
                                tableView.refresh();
                        } catch (Exception ignored) {
                        }
                });

                JMenuItem filterEmptyItem = new JMenuItem(Lang.get("table.column.menu.filter.empty"));
                filterEmptyItem.addActionListener(e -> {
                        table.addFilter(Filter.showEmpty(tableIndex));
                        tableView.refresh();
                });

                JMenuItem filterNonEmptyItem = new JMenuItem(Lang.get("table.column.menu.filter.non_empty"));
                filterNonEmptyItem.addActionListener(e -> {
                        table.addFilter(Filter.hideEmpty(tableIndex));
                        tableView.refresh();
                });

                JMenuItem clearFilterItem = new JMenuItem(Lang.get("table.column.menu.filter.clear"));
                clearFilterItem.addActionListener(e -> {
                        table.clearColumnFilter(tableIndex);
                        tableView.refresh();
                });

                filterMenu.add(filterTopNItem);
                filterMenu.add(filterBottomNItem);
                filterMenu.addSeparator();
                filterMenu.add(filterNumericRangeItem);
                filterMenu.addSeparator();
                filterMenu.add(filterEmptyItem);
                filterMenu.add(filterNonEmptyItem);

                if (columnType.isCategorical()) {
                        new SwingWorker<>() {
                                private List<String> categories;
                                private boolean hasCategories;

                                @Override
                                protected Void doInBackground() throws Exception {
                                        hasCategories = StringStatisticService.hasCategories(table, tableIndex);
                                        if (hasCategories) {
                                                categories = StringStatisticService.getCategories(table,
                                                                tableIndex);
                                        } else {
                                                categories = Collections.emptyList();
                                        }
                                        return null;
                                }

                                @Override
                                protected void done() {
                                        filterMenu.addSeparator();
                                        if (hasCategories) {
                                                if (categories.size() > MAX_CATEGORIES) {
                                                        JMenuItem filterByCategory = new JMenuItem(
                                                                        Lang.get("table.column.menu.filter.by_category"));
                                                        filterByCategory.addActionListener(e -> {
                                                                String category = InputSelectDialog.show(
                                                                                categories,
                                                                                Lang.get("table.column.menu.filter.category"),
                                                                                Lang.get("table.column.menu.filter.by_category"),
                                                                                Lang.get("table.column.menu.filter.category.error"))
                                                                                .orElse(null);
                                                                if (category != null && !category.isBlank()) {
                                                                        table.addFilter(Filter.equals(tableIndex,
                                                                                        category));
                                                                        tableView.refresh();
                                                                }
                                                        });
                                                        filterMenu.add(filterByCategory);
                                                } else {
                                                        JMenu filterByCategoryMenu = new JMenu(
                                                                        Lang.get("table.column.menu.filter.by_category"));
                                                        for (String category : categories) {
                                                                JMenuItem categoryItem = new JMenuItem(category);
                                                                categoryItem.addActionListener(e -> {
                                                                        table.addFilter(Filter.equals(tableIndex,
                                                                                        category));
                                                                        tableView.refresh();
                                                                });
                                                                filterByCategoryMenu.add(categoryItem);
                                                        }
                                                        filterMenu.add(filterByCategoryMenu);
                                                }
                                        } else {
                                                JMenuItem filterByValueItem = new JMenuItem(
                                                                Lang.get("table.column.menu.filter.by_value"));
                                                filterByValueItem.addActionListener(e -> {
                                                        String value = InputStringDialog.show(
                                                                        Lang.get("table.column.menu.filter.value"),
                                                                        Lang.get("table.column.menu.filter.by_value"),
                                                                        Lang.get("table.column.menu.filter.value.error"))
                                                                        .orElse(null);
                                                        if (value != null && !value.isBlank()) {
                                                                table.addFilter(Filter.search(tableIndex, value));
                                                                tableView.refresh();
                                                        }
                                                });
                                                filterMenu.add(filterByValueItem);
                                        }

                                        filterMenu.addSeparator();
                                        filterMenu.add(clearFilterItem);

                                        filterMenu.revalidate();
                                        filterMenu.repaint();

                                        TableColumnContextMenu.this.revalidate();
                                        TableColumnContextMenu.this.repaint();
                                }
                        }.execute();
                } else {
                        filterMenu.addSeparator();
                        filterMenu.add(clearFilterItem);
                }
                return filterMenu;
        }

        private JMenu buildStatsMenu() {
                JMenu stats = new JMenu(Lang.get("table.column.menu.stats"));
                String columnName = table.getColumnName(tableIndex);

                JMenuItem summary = new JMenuItem(Lang.get("table.column.menu.stats.summary"));
                summary.addActionListener(e -> onOpenStatisticDialog(
                                Lang.get("table.column.menu.stats.summary.title", table.getColumnName(tableIndex)),
                                StatisticService.getSummary(table, tableIndex)));
                stats.add(summary);

                JMenuItem nullRateItem = new JMenuItem(Lang.get("table.column.menu.stats.null_rate"));
                nullRateItem.addActionListener(e -> onOpenStatisticDialog(
                                Lang.get("table.column.menu.stats.null_rate.title", columnName),
                                Lang.get("table.column.menu.stats.null_rate.content",
                                                formatDouble(StatisticService.getNullRate(table, tableIndex)))));
                stats.add(nullRateItem);

                JMenuItem nullCountItem = new JMenuItem(Lang.get("table.column.menu.stats.null_count"));
                nullCountItem.addActionListener(e -> onOpenStatisticDialog(
                                Lang.get("table.column.menu.stats.null_count.title", columnName),
                                Lang.get("table.column.menu.stats.null_count.content",
                                                formatInteger(StatisticService.getNullCount(table, tableIndex)))));
                stats.add(nullCountItem);
                JMenuItem cardinalityRatioItem = new JMenuItem(Lang.get("table.column.menu.stats.cardinality_ratio"));
                cardinalityRatioItem.addActionListener(e -> onOpenStatisticDialog(
                                Lang.get("table.column.menu.stats.cardinality_ratio.title", columnName),
                                Lang.get("table.column.menu.stats.cardinality_ratio.content",
                                                formatDouble(StatisticService.getCardinalityRatio(table,
                                                                tableIndex)))));
                stats.add(cardinalityRatioItem);

                if (columnType == DataType.EMPTY || columnType == DataType.BOOLEAN) {
                        return stats;
                }
                stats.addSeparator();

                switch (columnType) {
                        case STRING -> addStringStatistics(stats, columnName);
                        case INTEGER, DOUBLE -> addNumericStatistics(stats, columnName);
                        case DATE -> addDateStatistics(stats, columnName);
                        default -> {
                        }
                }

                return stats;
        }

        private void addStringStatistics(JMenu stats, String columnName) {
                JMenu lengthMenu = new JMenu(Lang.get("table.column.menu.stats.length"));

                JMenuItem sumLengthItem = new JMenuItem(Lang.get("table.column.menu.stats.length.sum"));
                sumLengthItem.addActionListener(e -> onOpenStatisticDialog(
                                Lang.get("table.column.menu.stats.length.sum.title", columnName),
                                Lang.get("table.column.menu.stats.length.sum.content",
                                                formatInteger(StringStatisticService.getSumLength(table,
                                                                tableIndex)))));
                lengthMenu.add(sumLengthItem);

                JMenuItem meanLengthItem = new JMenuItem(Lang.get("table.column.menu.stats.length.mean"));
                meanLengthItem.addActionListener(e -> onOpenStatisticDialog(
                                Lang.get("table.column.menu.stats.length.mean.title", columnName),
                                Lang.get("table.column.menu.stats.length.mean.content",
                                                formatInteger(StringStatisticService.getMeanLength(table,
                                                                tableIndex)))));
                lengthMenu.add(meanLengthItem);

                JMenuItem minLengthItem = new JMenuItem(Lang.get("table.column.menu.stats.length.min"));
                minLengthItem.addActionListener(e -> onOpenStatisticDialog(
                                Lang.get("table.column.menu.stats.length.min.title", columnName),
                                Lang.get("table.column.menu.stats.length.min.content",
                                                formatInteger(StringStatisticService.getMinLength(table,
                                                                tableIndex)))));
                lengthMenu.add(minLengthItem);

                JMenuItem maxLengthItem = new JMenuItem(Lang.get("table.column.menu.stats.length.max"));
                maxLengthItem.addActionListener(e -> onOpenStatisticDialog(
                                Lang.get("table.column.menu.stats.length.max.title", columnName),
                                Lang.get("table.column.menu.stats.length.max.content",
                                                formatInteger(StringStatisticService.getMaxLength(table,
                                                                tableIndex)))));
                lengthMenu.add(maxLengthItem);

                stats.add(lengthMenu);

                JMenuItem entropyEstimateItem = new JMenuItem(Lang.get("table.column.menu.stats.length.entropy"));
                entropyEstimateItem.addActionListener(e -> onOpenStatisticDialog(
                                Lang.get("table.column.menu.stats.length.entropy.title", columnName),
                                Lang.get("table.column.menu.stats.length.entropy.content",
                                                formatDouble(StringStatisticService.getEntropyEstimate(table,
                                                                tableIndex)))));
                stats.add(entropyEstimateItem);

                JMenuItem modeItem = new JMenuItem(Lang.get("table.column.menu.stats.length.mode"));
                modeItem.addActionListener(e -> onOpenStatisticDialog(
                                Lang.get("table.column.menu.stats.length.mode.title", columnName),
                                Lang.get("table.column.menu.stats.length.mode.content",
                                                StringStatisticService.getMostFrequentValue(table, tableIndex)
                                                                .orElse("N/A"))));
                stats.add(modeItem);

                JMenuItem categoriesItem = new JMenuItem(Lang.get("table.column.menu.stats.string.categories"));
                categoriesItem.addActionListener(e -> {
                        List<String> categories = StringStatisticService.getCategories(table, tableIndex);
                        String content = Lang.get("table.column.menu.stats.string.categories.content",
                                        categories.size());
                        if (categories.size() <= MAX_CATEGORIES) {
                                content += ":\n" + String.join("\n", categories);
                        } else {
                                content += ".";
                        }
                        onOpenStatisticDialog(Lang.get("table.column.menu.stats.string.categories.title", columnName),
                                        content);
                });
                stats.add(categoriesItem);

                JMenuItem frequencyDistributionItem = new JMenuItem(
                                Lang.get("table.column.menu.stats.string.frequency_distribution"));
                frequencyDistributionItem.addActionListener(e -> {
                        Map<String, Long> frequencies = StringStatisticService.getFrequencyDistribution(table,
                                        tableIndex);
                        onOpenStatisticDialog(
                                        Lang.get("table.column.menu.stats.string.frequency_distribution.title",
                                                        columnName),
                                        formatFrequencyMap(frequencies));
                });
                stats.add(frequencyDistributionItem);
        }

        private void addNumericStatistics(JMenu stats, String columnName) {
                JMenuItem sumItem = new JMenuItem(Lang.get("table.column.menu.stats.length.sum"));
                sumItem.addActionListener(e -> onOpenStatisticDialog(
                                Lang.get("table.column.menu.stats.length.sum.title", columnName),
                                Lang.get("table.column.menu.stats.length.sum.content",
                                                formatDouble(NumericStatisticService.getSum(table, tableIndex)))));
                stats.add(sumItem);

                JMenuItem meanItem = new JMenuItem(Lang.get("table.column.menu.stats.length.mean"));
                meanItem.addActionListener(e -> onOpenStatisticDialog(
                                Lang.get("table.column.menu.stats.length.mean.title", columnName),
                                Lang.get("table.column.menu.stats.length.mean.content",
                                                formatDouble(NumericStatisticService.getMean(table, tableIndex)))));
                stats.add(meanItem);

                JMenuItem minItem = new JMenuItem(Lang.get("table.column.menu.stats.length.min"));
                minItem.addActionListener(e -> onOpenStatisticDialog(
                                Lang.get("table.column.menu.stats.length.min.title", columnName),
                                Lang.get("table.column.menu.stats.length.min.content",
                                                formatDouble(NumericStatisticService.getMin(table, tableIndex)))));
                stats.add(minItem);

                JMenuItem maxItem = new JMenuItem(Lang.get("table.column.menu.stats.length.max"));
                maxItem.addActionListener(e -> onOpenStatisticDialog(
                                Lang.get("table.column.menu.stats.length.max.title", columnName),
                                Lang.get("table.column.menu.stats.length.max.content",
                                                formatDouble(NumericStatisticService.getMax(table, tableIndex)))));
                stats.add(maxItem);

                JMenuItem medianItem = new JMenuItem(Lang.get("table.column.menu.stats.length.median"));
                medianItem.addActionListener(e -> onOpenStatisticDialog(
                                Lang.get("table.column.menu.stats.length.median.title", columnName),
                                Lang.get("table.column.menu.stats.length.median.content",
                                                formatDouble(NumericStatisticService.getMedian(table, tableIndex)))));
                stats.add(medianItem);

                JMenuItem modeItem = new JMenuItem(Lang.get("table.column.menu.stats.length.mode"));
                modeItem.addActionListener(e -> onOpenStatisticDialog(
                                Lang.get("table.column.menu.stats.length.mode.title", columnName),
                                Lang.get("table.column.menu.stats.length.mode.content",
                                                formatDouble(NumericStatisticService.getMostFrequentValueAsDouble(
                                                                table,
                                                                tableIndex)))));
                stats.add(modeItem);

                JMenuItem varianceItem = new JMenuItem(Lang.get("table.column.menu.stats.length.variance"));
                varianceItem.addActionListener(e -> onOpenStatisticDialog(
                                Lang.get("table.column.menu.stats.length.variance.title", columnName),
                                Lang.get("table.column.menu.stats.length.variance.content",
                                                formatDouble(NumericStatisticService.getVariance(table, tableIndex)))));
                stats.add(varianceItem);

                JMenuItem stdDevItem = new JMenuItem(Lang.get("table.column.menu.stats.length.standard_deviation"));
                stdDevItem.addActionListener(e -> onOpenStatisticDialog(
                                Lang.get("table.column.menu.stats.length.standard_deviation.title", columnName),
                                Lang.get("table.column.menu.stats.length.standard_deviation.content", formatDouble(
                                                NumericStatisticService.getStandardDeviation(table, tableIndex)))));
                stats.add(stdDevItem);

                JMenuItem rangeItem = new JMenuItem(Lang.get("table.column.menu.stats.length.range"));
                rangeItem.addActionListener(e -> onOpenStatisticDialog(
                                Lang.get("table.column.menu.stats.length.range.title", columnName),
                                Lang.get("table.column.menu.stats.length.range.content",
                                                formatDouble(NumericStatisticService.getRange(table, tableIndex)))));
                stats.add(rangeItem);

                JMenuItem iqrItem = new JMenuItem(Lang.get("table.column.menu.stats.interquartile_range"));
                iqrItem.addActionListener(e -> onOpenStatisticDialog(
                                Lang.get("table.column.menu.stats.interquartile_range.title", columnName),
                                Lang.get("table.column.menu.stats.interquartile_range.content",
                                                formatDouble(NumericStatisticService.getInterquartileRange(
                                                                table,
                                                                tableIndex)))));
                stats.add(iqrItem);

                JMenuItem intercentileRatioItem = new JMenuItem(Lang.get("table.column.menu.stats.intercentile_ratio"));
                intercentileRatioItem.addActionListener(e -> onOpenStatisticDialog(
                                Lang.get("table.column.menu.stats.intercentile_range_ratio.title", columnName),
                                Lang.get("table.column.menu.stats.intercentile_range_ratio.content",
                                                formatDouble(NumericStatisticService.getIntercentileRangeRatio(
                                                                table,
                                                                tableIndex)))));
                stats.add(intercentileRatioItem);

                JMenuItem skewnessItem = new JMenuItem(Lang.get("table.column.menu.stats.skewness"));
                skewnessItem.addActionListener(e -> onOpenStatisticDialog(
                                Lang.get("table.column.menu.stats.skewness.title", columnName),
                                Lang.get("table.column.menu.stats.skewness.content",
                                                formatDouble(NumericStatisticService.getSkewness(table,
                                                                tableIndex)))));
                stats.add(skewnessItem);

                JMenuItem kurtosisItem = new JMenuItem("Kurtosis");
                kurtosisItem.addActionListener(e -> onOpenStatisticDialog(
                                Lang.get("table.column.menu.stats.kurtosis.title", columnName),
                                Lang.get("table.column.menu.stats.kurtosis.content",
                                                formatDouble(NumericStatisticService.getKurtosis(table, tableIndex)))));
                stats.add(kurtosisItem);

                JMenuItem coefficientOfVariationItem = new JMenuItem(
                                Lang.get("table.column.menu.stats.coef_variation"));
                coefficientOfVariationItem.addActionListener(e -> onOpenStatisticDialog(
                                Lang.get("table.column.menu.stats.coefficient_of_variation.title", columnName),
                                Lang.get("table.column.menu.stats.coefficient_of_variation.content",
                                                formatDouble(
                                                                NumericStatisticService
                                                                                .getCoefficientOfVariation(
                                                                                                table,
                                                                                                tableIndex)))));
                stats.add(coefficientOfVariationItem);

                JMenuItem geometricMeanItem = new JMenuItem(Lang.get("table.column.menu.stats.geometric_mean"));
                geometricMeanItem.addActionListener(e -> onOpenStatisticDialog(
                                Lang.get("table.column.menu.stats.geometric_mean.title", columnName),
                                Lang.get("table.column.menu.stats.geometric_mean.content",
                                                formatDouble(NumericStatisticService.getGeometricMean(table,
                                                                tableIndex)))));
                stats.add(geometricMeanItem);

                JMenuItem harmonicMeanItem = new JMenuItem(Lang.get("table.column.menu.stats.harmonic_mean"));
                harmonicMeanItem.addActionListener(e -> onOpenStatisticDialog(
                                Lang.get("table.column.menu.stats.harmonic_mean.title", columnName),
                                Lang.get("table.column.menu.stats.harmonic_mean.content",
                                                formatDouble(NumericStatisticService.getHarmonicMean(table,
                                                                tableIndex)))));
                stats.add(harmonicMeanItem);

                JMenuItem madItem = new JMenuItem(Lang.get("table.column.menu.stats.mean_absolute_deviation"));
                madItem.addActionListener(e -> onOpenStatisticDialog(
                                Lang.get("table.column.menu.stats.mean_absolute_deviation.title", columnName),
                                Lang.get("table.column.menu.stats.mean_absolute_deviation.content",
                                                formatDouble(
                                                                NumericStatisticService.getMeanAbsoluteDeviation(table,
                                                                                tableIndex)))));
                stats.add(madItem);

                JMenuItem rmsItem = new JMenuItem(Lang.get("table.column.menu.stats.root_mean_square"));
                rmsItem.addActionListener(e -> onOpenStatisticDialog(
                                Lang.get("table.column.menu.stats.root_mean_square.title", columnName),
                                Lang.get("table.column.menu.stats.root_mean_square.content",
                                                formatDouble(NumericStatisticService.getRootMeanSquare(table,
                                                                tableIndex)))));
                stats.add(rmsItem);

                JMenu percentileMenu = new JMenu(Lang.get("table.column.menu.stats.percentile"));
                addPercentileItem(percentileMenu, columnName, 0.1, "P10");
                addPercentileItem(percentileMenu, columnName, 0.25, "P25");
                addPercentileItem(percentileMenu, columnName, 0.5, "P50");
                addPercentileItem(percentileMenu, columnName, 0.75, "P75");
                addPercentileItem(percentileMenu, columnName, 0.9, "P90");
                stats.add(percentileMenu);

                JMenu correlationMenu = new JMenu(Lang.get("table.column.menu.stats.correlation"));
                createColumnSelectionMenu(correlationMenu,
                                i -> i != tableIndex && table.getColumnType(i).isNumeric(),
                                i -> {
                                        String otherColumnName = table.getColumnName(i);
                                        Optional<Double> correlation = NumericStatisticService.getCorrelation(
                                                        table, tableIndex, i);

                                        onOpenStatisticDialog(
                                                        Lang.get("table.column.menu.stats.correlation.title",
                                                                        columnName,
                                                                        otherColumnName),
                                                        Lang.get("table.column.menu.stats.correlation.content",
                                                                        formatDouble(correlation)));
                                        return null;
                                });

                if (correlationMenu.getItemCount() > 0) {
                        stats.add(correlationMenu);
                }
        }

        private void addDateStatistics(JMenu stats, String columnName) {
                JMenuItem minItem = new JMenuItem(Lang.get("table.column.menu.stats.min_date"));
                minItem.addActionListener(e -> onOpenStatisticDialog(
                                Lang.get("table.column.menu.stats.min_date.title", columnName),
                                Lang.get("table.column.menu.stats.min_date.content",
                                                formatTimestamp(DateStatisticService.getMin(table, tableIndex)))));
                stats.add(minItem);

                JMenuItem maxItem = new JMenuItem(Lang.get("table.column.menu.stats.max_date"));
                maxItem.addActionListener(e -> onOpenStatisticDialog(
                                Lang.get("table.column.menu.stats.max_date.title", columnName),
                                Lang.get("table.column.menu.stats.max_date.content",
                                                formatTimestamp(DateStatisticService.getMax(table, tableIndex)))));
                stats.add(maxItem);

                JMenuItem medianItem = new JMenuItem(Lang.get("table.column.menu.stats.median_date"));
                medianItem.addActionListener(e -> onOpenStatisticDialog(
                                Lang.get("table.column.menu.stats.median_date.title", columnName),
                                Lang.get("table.column.menu.stats.median_date.content",
                                                formatTimestamp(DateStatisticService.getMedian(table, tableIndex)))));
                stats.add(medianItem);

                JMenuItem modeItem = new JMenuItem(Lang.get("table.column.menu.stats.mode_date"));
                modeItem.addActionListener(e -> onOpenStatisticDialog(
                                Lang.get("table.column.menu.stats.mode_date.title", columnName),
                                Lang.get("table.column.menu.stats.mode_date.content",
                                                formatTimestamp(DateStatisticService.getMostFrequentValue(table,
                                                                tableIndex)))));
                stats.add(modeItem);
        }

        private void addPercentileItem(JMenu menu, String columnName, double percentile, String label) {
                JMenuItem item = new JMenuItem(label);
                item.addActionListener(e -> onOpenStatisticDialog(
                                Lang.get("table.column.menu.stats.percentile.title", label, columnName),
                                Lang.get("table.column.menu.stats.percentile.content", label, formatDouble(
                                                NumericStatisticService.getPercentile(table, tableIndex,
                                                                percentile)))));
                menu.add(item);
        }

        private String formatInteger(Optional<Integer> value) {
                return String.valueOf(value.orElse(-1));
        }

        private String formatDouble(Optional<Double> value) {
                return String.valueOf(value.orElse(Double.NaN));
        }

        private String formatTimestamp(Optional<Timestamp> value) {
                return value.map(Timestamp::toString).orElse("N/A");
        }

        private String formatFrequencyMap(Map<String, Long> frequencies) {
                if (frequencies.isEmpty()) {
                        return Lang.get("table.column.menu.stats.string.frequency_distribution.empty");
                }

                StringBuilder builder = new StringBuilder();
                frequencies.forEach((key, count) -> builder.append(key).append(": ").append(count).append("\n"));
                return builder.toString();
        }

        private void createColumnSelectionMenu(JMenu menu, Function<Integer, Boolean> condition,
                        Function<Integer, Void> action) {
                for (int i = 0; i < table.getColumnCount(); i++) {
                        if (!condition.apply(i)) {
                                continue;
                        }
                        JMenuItem colItem = new JMenuItem(table.getColumnName(i));
                        int colIndex = i;
                        colItem.addActionListener(e -> action.apply(colIndex));
                        menu.add(colItem);
                }
        }

        private void onOpenStatisticDialog(String title, String content) {
                StatisticsDialog dialog = new StatisticsDialog(GUIController.getInstance().getMainView(), title,
                                content);
                dialog.setVisible(true);
                if (dialog.isAddedToNotebook()) {
                        NoteBookService.add(new NoteBookText(content));
                        GUIController.getInstance().getMainView().getReportPanel().refresh();
                }
        }
}