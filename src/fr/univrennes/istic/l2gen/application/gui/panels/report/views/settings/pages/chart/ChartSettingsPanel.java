package fr.univrennes.istic.l2gen.application.gui.panels.report.views.settings.pages.chart;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;

import fr.univrennes.istic.l2gen.application.core.config.Lang;
import fr.univrennes.istic.l2gen.application.core.notebook.NoteBookChart;
import fr.univrennes.istic.l2gen.application.core.notebook.NoteBookValue;
import fr.univrennes.istic.l2gen.application.core.table.DataTable;
import fr.univrennes.istic.l2gen.application.gui.GUIController;
import fr.univrennes.istic.l2gen.application.gui.panels.report.views.settings.SettingRowPanel;
import fr.univrennes.istic.l2gen.application.gui.panels.report.views.settings.SettingSectionPanel;
import fr.univrennes.istic.l2gen.application.gui.panels.report.views.settings.SettingSeparatorRow;
import fr.univrennes.istic.l2gen.application.gui.panels.report.views.settings.pages.IReportSettingPanel;
import fr.univrennes.istic.l2gen.visustats.view.DataViewType;

public final class ChartSettingsPanel extends SettingSectionPanel implements IReportSettingPanel {

        private JComboBox<String> chartTypeCombo;
        private JTextField titleField;

        private JCheckBox stackedCheckBox;
        private JComboBox<String> gridLevelCombo;

        private final SharedChartSettings shared;

        public ChartSettingsPanel(AxisSettingsPanel axisSettings, LegendSettingsPanel legendSettings,
                        ColorSettingsPanel colorSettings,
                        DataSettingsPanel dataSettings) {
                super(Lang.get("report.settings.chart"));

                build();

                shared = new SharedChartSettings(this, axisSettings, legendSettings, colorSettings, dataSettings);
                dataSettings.setShared(shared);
        }

        private void build() {
                // !!! NE PAS CHANGER L'ORDRE DES ITEMS, IL CORRESPOND A L'ORDRE DE DataViewType
                chartTypeCombo = new JComboBox<>(new String[] {
                                Lang.get("report.settings.chart.bar"),
                                Lang.get("report.settings.chart.columns"),
                                Lang.get("report.settings.chart.pie"),
                                Lang.get("report.settings.chart.line"),
                                Lang.get("report.settings.chart.scatter"),
                                Lang.get("report.settings.chart.area"),
                                Lang.get("report.settings.chart.spider"),
                });
                chartTypeCombo.addItemListener(e -> {
                        DataViewType type = DataViewType.values()[chartTypeCombo.getSelectedIndex()];
                        switch (type) {
                                case PIE -> {
                                        shared.axis().setVisible(false);
                                }
                                case AREA, LINE, SCATTER -> {
                                        shared.axis().setVisible(true);
                                        stackedCheckBox.setEnabled(shared.data().getBiggerGroupColumn().isPresent());
                                }
                                case SPIDER -> {
                                        shared.axis().setVisible(true);
                                        stackedCheckBox.setEnabled(shared.data().getBiggerGroupColumn().isPresent());

                                        gridLevelCombo.setEnabled(true);
                                }
                                default -> {
                                        shared.axis().setVisible(true);
                                }
                        }
                });
                addRow(new SettingRowPanel(Lang.get("report.settings.chart.type"), chartTypeCombo));

                titleField = new JTextField(Lang.get("report.settings.chart.default_title"));
                addRow(new SettingRowPanel(Lang.get("report.settings.chart.title"), titleField));

                addRow(new SettingSeparatorRow(Lang.get("report.settings.chart.advanced")));

                stackedCheckBox = new JCheckBox();
                stackedCheckBox.setEnabled(false);
                addRow(new SettingRowPanel(Lang.get("report.settings.chart.stacked"), stackedCheckBox));

                gridLevelCombo = new JComboBox<>(new String[] {
                                "1",
                                "2",
                                "3",
                                "4",
                                "5",
                });
                gridLevelCombo.setSelectedIndex(3);
                gridLevelCombo.setEnabled(false);
                addRow(new SettingRowPanel(Lang.get("report.settings.chart.spider_grid_level"), gridLevelCombo));
        }

        public JTextField getTitleField() {
                return titleField;
        }

        @Override
        public NoteBookValue createNoteBook() {
                NoteBookChart chart = new NoteBookChart(
                                DataViewType.values()[chartTypeCombo.getSelectedIndex()],
                                titleField.getText(),

                                shared.legend().isLegendVisible(),
                                shared.legend().isLegendHorizontal(),

                                stackedCheckBox.isSelected(),
                                gridLevelCombo.getSelectedIndex() + 1,

                                shared.axis().getTickCount(),
                                shared.axis().getScale(),

                                shared.axis().isXVisible(),
                                shared.axis().getXLabel(),

                                shared.axis().isYVisible(),
                                shared.axis().getYLabel(),

                                shared.data().getTable(),
                                shared.data().getBiggerGroupColumn(),
                                shared.data().getGroupColumn(),
                                shared.data().getValueColumn(),
                                shared.data().isIncludeFilters(),
                                shared.data().isPercentage(),

                                shared.color().getColors());

                return chart;
        }

        @Override
        public void loadNoteBook(NoteBookValue value) {
                if (!(value instanceof NoteBookChart chart)) {
                        return;
                }
                chartTypeCombo.setSelectedIndex(chart.getType().ordinal());
                titleField.setText(chart.getTitle());

                stackedCheckBox.setSelected(chart.isStacked());
                gridLevelCombo.setSelectedIndex(chart.getGridLevel() - 1);

                shared.legend().setLegendVisible(chart.isLegendVisible());
                shared.legend().setLegendHorizontal(chart.isLegendHorizontal());

                shared.axis().setTickCount(chart.getTickCount());
                shared.axis().setScale(chart.getScale());

                shared.axis().setXVisible(chart.isXVisible());
                shared.axis().setXLabel(chart.getXLabel());
                shared.axis().setYVisible(chart.isYVisible());
                shared.axis().setYLabel(chart.getYLabel());

                shared.data().setBiggerGroupColumn(chart.getBiggerGroupColumn());
                shared.data().setGroupColumn(chart.getGroupColumn());
                shared.data().setValueColumn(chart.getValueColumn());
                shared.data().setIncludeFilters(chart.isIncludeFilters());
                shared.data().setIsPercentage(chart.isPercentage());

                shared.color().setColorLabels(chart.getColors(), chart.getColorLabels());

                DataTable table = GUIController.getInstance().getTable().orElse(null);
                if (table != null && chart.getTable() != null && table.getPath() == chart.getTable().getPath()) {
                        return;
                }
                GUIController.getInstance().setTable(chart.getTable());
                shared.data().setTable(chart.getTable());

        }
}
