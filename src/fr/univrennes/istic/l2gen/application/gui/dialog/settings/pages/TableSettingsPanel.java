package fr.univrennes.istic.l2gen.application.gui.dialog.settings.pages;

import javax.swing.JCheckBox;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import fr.univrennes.istic.l2gen.application.core.config.Config;
import fr.univrennes.istic.l2gen.application.core.config.Lang;
import fr.univrennes.istic.l2gen.application.gui.dialog.settings.AbstractSettingsPanel;
import fr.univrennes.istic.l2gen.application.gui.dialog.settings.SettingsRowPanel;
import fr.univrennes.istic.l2gen.application.gui.dialog.settings.SettingsSectionPanel;

public final class TableSettingsPanel extends AbstractSettingsPanel {

        private final JCheckBox manualTypingCheckBox;
        private final JCheckBox readOnlyCheckBox;
        private final JSpinner castSensitivity;
        private final JCheckBox showRowNumbersCheckBox;
        private final JCheckBox showNullValuesCheckBox;

        private final JCheckBox hideEmptyColumnsCheckBox;
        private final JCheckBox showColumnTypesCheckBox;
        private final JCheckBox autoResizeColumnsCheckBox;

        private final JCheckBox calculateStatisticsCheckBox;

        private final JCheckBox saveFiltersWithTableCheckBox;

        public TableSettingsPanel() {

                SettingsSectionPanel modeSection = new SettingsSectionPanel(
                                Lang.get("settings.table.section.mode"));

                readOnlyCheckBox = new JCheckBox();
                readOnlyCheckBox.setSelected(Config.getBoolean("settings.table.read_only", true));
                modeSection.addRow(new SettingsRowPanel(Lang.get("settings.table.read_only"), readOnlyCheckBox));

                manualTypingCheckBox = new JCheckBox();
                manualTypingCheckBox.setSelected(Config.getBoolean("settings.table.manual_typing", true));
                modeSection.addRow(
                                new SettingsRowPanel(Lang.get("settings.table.manual_typing"), manualTypingCheckBox));

                castSensitivity = new JSpinner(new SpinnerNumberModel(0.95, 0.5, 1.0, 0.01));
                castSensitivity.setValue((double) Config.getFloat("settings.table.cast_sensitivity", 0.95f));
                modeSection.addRow(new SettingsRowPanel(Lang.get("settings.table.cast_sensitivity"), castSensitivity));

                SettingsSectionPanel displaySection = new SettingsSectionPanel(
                                Lang.get("settings.table.section.display"));

                showRowNumbersCheckBox = new JCheckBox();
                showRowNumbersCheckBox.setSelected(Config.getBoolean("settings.table.show_row_numbers", false));
                displaySection
                                .addRow(new SettingsRowPanel(Lang.get("settings.table.show_row_numbers"),
                                                showRowNumbersCheckBox));

                showNullValuesCheckBox = new JCheckBox();
                showNullValuesCheckBox.setSelected(Config.getBoolean("settings.table.show_null_values", false));
                displaySection
                                .addRow(new SettingsRowPanel(Lang.get("settings.table.show_null_values"),
                                                showNullValuesCheckBox));

                SettingsSectionPanel columnsSection = new SettingsSectionPanel(
                                Lang.get("settings.table.section.columns"));

                hideEmptyColumnsCheckBox = new JCheckBox();
                hideEmptyColumnsCheckBox.setSelected(
                                Config.getBoolean("settings.table.columns.hide_empty", false));
                columnsSection
                                .addRow(new SettingsRowPanel(Lang.get("settings.table.columns.hide_empty"),
                                                hideEmptyColumnsCheckBox));

                showColumnTypesCheckBox = new JCheckBox();
                showColumnTypesCheckBox.setSelected(
                                Config.getBoolean("settings.table.columns.show_types", false));
                columnsSection
                                .addRow(new SettingsRowPanel(Lang.get("settings.table.columns.show_types"),
                                                showColumnTypesCheckBox));

                autoResizeColumnsCheckBox = new JCheckBox();
                autoResizeColumnsCheckBox.setSelected(
                                Config.getBoolean("settings.table.columns.auto_resize", true));
                columnsSection.addRow(
                                new SettingsRowPanel(Lang.get("settings.table.columns.auto_resize"),
                                                autoResizeColumnsCheckBox));

                SettingsSectionPanel statsSection = new SettingsSectionPanel(
                                Lang.get("settings.table.section.statistics"));

                calculateStatisticsCheckBox = new JCheckBox();
                calculateStatisticsCheckBox.setSelected(
                                Config.getBoolean("settings.table.columns.calculate_statistics", true));
                statsSection.addRow(new SettingsRowPanel(
                                Lang.get("settings.table.columns.calculate_statistics"),
                                calculateStatisticsCheckBox));

                SettingsSectionPanel filterSection = new SettingsSectionPanel(
                                Lang.get("settings.table.section.filters"));

                saveFiltersWithTableCheckBox = new JCheckBox();
                saveFiltersWithTableCheckBox.setSelected(
                                Config.getBoolean("settings.table.filters.save_with_table", true));
                filterSection.addRow(new SettingsRowPanel(
                                Lang.get("settings.table.filters.save_with_table"),
                                saveFiltersWithTableCheckBox));

                addSection(modeSection);
                addSection(displaySection);
                addSection(columnsSection);
                addSection(statsSection);
                addSection(filterSection);
        }

        @Override
        public boolean applySettings() {
                boolean changed = false;
                if (Config.getBoolean("settings.table.read_only", true) != readOnlyCheckBox.isSelected()) {
                        changed = true;
                }
                if (Config.getBoolean("settings.table.manual_typing", true) != manualTypingCheckBox
                                .isSelected()) {
                        changed = true;
                }
                if (Config.getFloat("settings.table.cast_sensitivity",
                                0.95f) != ((Double) castSensitivity.getValue()).floatValue()) {
                        changed = true;
                }
                if (Config.getBoolean("settings.table.show_row_numbers", false) != showRowNumbersCheckBox
                                .isSelected()) {
                        changed = true;
                }
                if (Config.getBoolean("settings.table.show_null_values", false) != showNullValuesCheckBox
                                .isSelected()) {
                        changed = true;
                }
                if (Config.getBoolean("settings.table.columns.hide_empty", false) != hideEmptyColumnsCheckBox
                                .isSelected()) {
                        changed = true;
                }
                if (Config.getBoolean("settings.table.columns.show_types", false) != showColumnTypesCheckBox
                                .isSelected()) {
                        changed = true;
                }
                if (Config.getBoolean("settings.table.columns.auto_resize", true) != autoResizeColumnsCheckBox
                                .isSelected()) {
                        changed = true;
                }
                if (Config.getBoolean("settings.table.columns.calculate_statistics",
                                true) != calculateStatisticsCheckBox.isSelected()) {
                        changed = true;
                }
                if (Config.getBoolean("settings.table.filters.save_with_table",
                                true) != saveFiltersWithTableCheckBox.isSelected()) {
                        changed = true;
                }

                Config.put("settings.table.read_only", readOnlyCheckBox.isSelected());
                Config.put("settings.table.manual_typing", manualTypingCheckBox.isSelected());
                Config.put("settings.table.cast_sensitivity",
                                ((Double) castSensitivity.getValue()).floatValue());
                Config.put("settings.table.show_row_numbers", showRowNumbersCheckBox.isSelected());
                Config.put("settings.table.show_null_values", showNullValuesCheckBox.isSelected());
                Config.put("settings.table.columns.hide_empty", hideEmptyColumnsCheckBox.isSelected());
                Config.put("settings.table.columns.show_types", showColumnTypesCheckBox.isSelected());
                Config.put("settings.table.columns.auto_resize", autoResizeColumnsCheckBox.isSelected());
                Config.put("settings.table.columns.calculate_statistics",
                                calculateStatisticsCheckBox.isSelected());
                Config.put("settings.table.filters.save_with_table", saveFiltersWithTableCheckBox.isSelected());

                return changed;
        }

        @Override
        public boolean requiresRestart() {
                return false;
        }
}
