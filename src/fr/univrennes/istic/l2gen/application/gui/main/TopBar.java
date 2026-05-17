package fr.univrennes.istic.l2gen.application.gui.main;

import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import fr.univrennes.istic.l2gen.application.core.config.Lang;
import fr.univrennes.istic.l2gen.application.core.services.table.TableService;
import fr.univrennes.istic.l2gen.application.core.table.DataTable;
import fr.univrennes.istic.l2gen.application.gui.GUIController;
import fr.univrennes.istic.l2gen.application.gui.dialog.settings.SettingsDialog;
import fr.univrennes.istic.l2gen.application.gui.shortcuts.Shortcuts;

public final class TopBar extends JMenuBar {

    public TopBar() {
        add(buildFileMenu());
        add(buildViewMenu());
        add(buildHelpMenu());
    }

    private JMenu buildFileMenu() {
        JMenu fileMenu = new JMenu(Lang.get("menu.file"));
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem openItem = new JMenuItem(Lang.get("menu.file.open"));
        openItem.setAccelerator(Shortcuts.getKeyStroke(Shortcuts.KEY_TABLE_OPEN, Shortcuts.DEFAULT_TABLE_OPEN));
        openItem.addActionListener(e -> GUIController.getInstance().onOpenFileDialog());
        fileMenu.add(openItem);

        if (!TableService.getRecentTables().isEmpty()) {
            JMenu openRecentMenu = new JMenu(Lang.get("menu.file.open_recent"));
            for (File recentFile : TableService.getRecentTables()) {
                JMenuItem recentItem = new JMenuItem(recentFile.getAbsolutePath());
                recentItem.addActionListener(e -> {
                    DataTable table = TableService.get(recentFile);
                    if (!recentFile.exists() || table == null) {
                        TableService.removeRecent(recentFile);
                        GUIController.getInstance().onException(
                                new IOException(Lang.get("error.table_not_found", recentFile.getAbsolutePath())));
                    } else {
                        GUIController.getInstance().setTable(table);
                    }
                });
                openRecentMenu.add(recentItem);
            }
            fileMenu.add(openRecentMenu);
        }

        JMenuItem openUrlItem = new JMenuItem(Lang.get("menu.file.open_url"));
        openUrlItem.setAccelerator(
                Shortcuts.getKeyStroke(Shortcuts.KEY_FILE_OPEN_URL, Shortcuts.DEFAULT_FILE_OPEN_URL));
        openUrlItem.addActionListener(e -> GUIController.getInstance().onOpenUrlDialog());
        fileMenu.add(openUrlItem);

        fileMenu.addSeparator();

        JMenuItem settingsItem = new JMenuItem(Lang.get("menu.file.settings"));
        settingsItem.setAccelerator(
                Shortcuts.getKeyStroke(Shortcuts.KEY_VIEW_SETTINGS, Shortcuts.DEFAULT_VIEW_SETTINGS));
        settingsItem.addActionListener(e -> {
            Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
            new SettingsDialog(parentFrame).setVisible(true);
        });
        fileMenu.add(settingsItem);

        fileMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem(Lang.get("menu.file.exit"));
        exitItem.setAccelerator(Shortcuts.getKeyStroke(Shortcuts.KEY_FILE_EXIT, Shortcuts.DEFAULT_FILE_EXIT));
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);

        return fileMenu;
    }

    private JMenu buildViewMenu() {
        JMenu viewMenu = new JMenu(Lang.get("menu.view"));
        viewMenu.setMnemonic(KeyEvent.VK_V);

        JMenu panelsMenu = new JMenu(Lang.get("menu.view.panels"));
        JMenuItem table = new JMenuItem(Lang.get("menu.view.panels.table"));
        table.addActionListener(e -> {
            boolean isVisible = GUIController.getInstance().getMainView().getTablePanel().isVisible();
            GUIController.getInstance().getMainView().getTablePanel()
                    .setVisible(!isVisible);
            if (!isVisible) {
                GUIController.getInstance().getMainView().resetSplit();
            }
        });
        panelsMenu.add(table);

        panelsMenu.addSeparator();

        JMenuItem notebook = new JMenuItem(Lang.get("menu.view.panels.notebook"));
        notebook.addActionListener(e -> {
            boolean isVisible = GUIController.getInstance().getMainView().getReportPanel().isVisible();
            GUIController.getInstance().getMainView().getReportPanel()
                    .setVisible(!isVisible);

            if (!isVisible) {
                GUIController.getInstance().getMainView().resetSplit();
            }
        });
        panelsMenu.add(notebook);

        JMenuItem notebookSettings = new JMenuItem(Lang.get("menu.view.panels.notebook_settings"));
        notebookSettings.addActionListener(e -> {
            boolean isVisible = GUIController.getInstance().getMainView().getReportPanel().getSettingView().isVisible();
            GUIController.getInstance().getMainView().getReportPanel().getSettingView()
                    .setVisible(
                            !isVisible);
            if (!isVisible) {
                GUIController.getInstance().getMainView().getReportPanel().resetSplit();
            }
        });
        panelsMenu.add(notebookSettings);

        viewMenu.add(panelsMenu);

        viewMenu.addSeparator();

        JMenu split = new JMenu(Lang.get("menu.view.split"));

        JMenuItem orientation = new JMenuItem(Lang.get("menu.view.split.orientation_main"));
        orientation.addActionListener(e -> {
            GUIController.getInstance().getMainView().toogleSplitOrientation();
        });
        split.add(orientation);
        JMenuItem notebookOrientation = new JMenuItem(Lang.get("menu.view.split.orientation_notebook"));
        notebookOrientation.addActionListener(e -> {
            GUIController.getInstance().getMainView().getReportPanel().toogleSplitOrientation();
        });
        split.add(notebookOrientation);
        split.addSeparator();

        JMenuItem resetMainSplit = new JMenuItem(Lang.get("menu.view.split.reset_main"));
        resetMainSplit.addActionListener(e -> GUIController.getInstance().getMainView().resetSplit());
        split.add(resetMainSplit);

        JMenuItem resetNotebookSplit = new JMenuItem(Lang.get("menu.view.split.reset_notebook"));
        resetNotebookSplit
                .addActionListener(e -> GUIController.getInstance().getMainView().getReportPanel().resetSplit());
        split.add(resetNotebookSplit);

        JMenuItem resetSplit = new JMenuItem(Lang.get("menu.view.split.reset"));
        resetSplit.addActionListener(e -> {
            GUIController.getInstance().getMainView().getReportPanel().getSettingView().setVisible(true);
            GUIController.getInstance().getMainView().getReportPanel().setVisible(true);
            GUIController.getInstance().getMainView().getTablePanel().setVisible(true);

            GUIController.getInstance().getMainView().resetSplit();
            GUIController.getInstance().getMainView().getReportPanel().resetSplit();

            GUIController.getInstance().getMainView().setSplitOrientation(JSplitPane.HORIZONTAL_SPLIT);
            GUIController.getInstance().getMainView().getReportPanel().setSplitOrientation(JSplitPane.VERTICAL_SPLIT);
        });
        split.add(resetSplit);

        viewMenu.add(split);
        return viewMenu;
    }

    private JMenu buildHelpMenu() {
        JMenu helpMenu = new JMenu(Lang.get("menu.help"));
        helpMenu.setMnemonic(KeyEvent.VK_H);

        JMenu languagesMenu = new JMenu(Lang.get("menu.help.languages"));

        for (String lang : Lang.getSupportedLanguages()) {
            Locale local = Locale.forLanguageTag(lang);

            String displayName = local.getDisplayLanguage(local);
            displayName = displayName.substring(0, 1).toUpperCase() + displayName.substring(1);

            JMenuItem languageItem = new JMenuItem(displayName + " (" + local.getLanguage().toUpperCase() + ")");
            languageItem.addActionListener(e -> GUIController.getInstance().onLanguageChange(local));
            languagesMenu.add(languageItem);
        }

        helpMenu.add(languagesMenu);

        helpMenu.addSeparator();

        JMenuItem documentationItem = new JMenuItem(Lang.get("menu.help.documentation"));
        documentationItem.setAccelerator(
                Shortcuts.getKeyStroke(Shortcuts.KEY_HELP_DOCUMENTATION, Shortcuts.DEFAULT_HELP_DOCUMENTATION));
        documentationItem.addActionListener(e -> GUIController.getInstance().onOpenDocDialog());
        helpMenu.add(documentationItem);

        JMenuItem aboutItem = new JMenuItem(Lang.get("menu.help.about"));
        aboutItem.addActionListener(e -> GUIController.getInstance().onOpenAboutDialog());
        helpMenu.add(aboutItem);

        return helpMenu;
    }
}