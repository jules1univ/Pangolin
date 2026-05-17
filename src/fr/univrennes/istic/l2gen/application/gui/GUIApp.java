package fr.univrennes.istic.l2gen.application.gui;

import fr.univrennes.istic.l2gen.application.core.CoreApp;
import fr.univrennes.istic.l2gen.application.core.config.Config;
import fr.univrennes.istic.l2gen.application.core.config.Lang;
import fr.univrennes.istic.l2gen.application.core.config.Log;
import fr.univrennes.istic.l2gen.application.core.services.table.TableService;
import fr.univrennes.istic.l2gen.application.gui.dialog.settings.pages.AppearanceSettingsPanel;
import fr.univrennes.istic.l2gen.application.gui.main.MainView;
import fr.univrennes.istic.l2gen.application.gui.main.SplashScreen;

import java.time.LocalTime;
import java.util.Collections;
import java.util.Locale;
import java.awt.Font;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;

public final class GUIApp extends CoreApp<GUIController> {

    public GUIApp() {
        super(GUIController.getInstance());
        GUIController.getInstance().setApp(this);
    }

    @Override
    public void start() {
        FlatLaf.setGlobalExtraDefaults(Collections.singletonMap("@accentColor", "#39B763"));

        boolean useFlatLaf = Config.getBoolean("settings.appearance.use_flatlaf", true);
        if (useFlatLaf && !FlatLightLaf.setup()) {
            JOptionPane.showMessageDialog(null,
                    Lang.get("error.init.message"),
                    Lang.get("error.init.title"),
                    JOptionPane.ERROR_MESSAGE);
        }

        String langTag = Config.getString("settings.general.language", Lang.getDefaultLocale().toLanguageTag());
        Locale locale = Locale.forLanguageTag(langTag);
        if (Lang.isSupported(locale)) {
            Lang.setLocale(locale);
        }

        if (useFlatLaf) {
            int hour = LocalTime.now().getHour();
            int minHour = Config.getInt("settings.appearance.auto_start", 18);
            int maxHour = Config.getInt("settings.appearance.auto_end", 6);

            minHour = Math.min(minHour, maxHour);
            maxHour = Math.max(minHour, maxHour);

            int theme = Config.getInt("settings.appearance.theme", 3);

            try {
                switch (theme) {
                    case AppearanceSettingsPanel.THEME_LIGHT -> {
                        UIManager.setLookAndFeel(new FlatLightLaf());
                        Config.DARK_MODE = false;
                    }
                    case AppearanceSettingsPanel.THEME_DARK -> {
                        UIManager.setLookAndFeel(new FlatDarkLaf());
                        Config.DARK_MODE = true;
                    }
                    case AppearanceSettingsPanel.THEME_SYSTEM -> {
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                        Config.DARK_MODE = UIManager.getLookAndFeel() instanceof FlatDarkLaf;
                    }
                    default -> {
                        if (hour >= maxHour && hour < minHour) {
                            UIManager.setLookAndFeel(new FlatDarkLaf());
                            Config.DARK_MODE = true;
                        } else {
                            UIManager.setLookAndFeel(new FlatLightLaf());
                            Config.DARK_MODE = false;
                        }
                    }
                }
            } catch (Exception e) {
                Log.debug("Failed to set Flatlaf theme", e);
            }
        }

        int fontSize = Config.getInt("settings.appearance.font_size", 12);
        String fontFamily = Config.getString("settings.appearance.font_family",
                UIManager.getFont("Label.font").getFamily());

        UIManager.put("defaultFont", new FontUIResource(fontFamily, Font.PLAIN, fontSize));

        SwingUtilities.invokeLater(() -> {
            SplashScreen splash = new SplashScreen();
            splash.setVisible(Config.getBoolean("settings.startup.show_welcome", true));

            new Thread(() -> {
                splash.setStatus(Lang.get("app.loading.recents"));
                TableService.loadRecents();

                splash.setStatus(Lang.get("app.loading.ui"));
                MainView mainView = new MainView(splash);
                this.controller.setMainView(mainView);
                this.controller.onStart();
            }).start();
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            this.controller.onStop();
        }));
    }

    @Override
    public void stop() {
        this.controller.onStop();
        this.controller.getMainView().dispose();
    }

}