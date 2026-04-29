package fr.univrennes.istic.l2gen.application.core.services.export;

import java.util.Locale;

import fr.univrennes.istic.l2gen.application.core.lang.Lang;

public enum ExportTheme {
    LINEN("report.export.theme.linen", "theme-linen", "linen"),
    INK("report.export.theme.ink", "theme-ink", "ink"),
    SAGE("report.export.theme.sage", "theme-sage", "sage"),
    EMBER("report.export.theme.ember", "theme-ember", "ember"),
    HARBOR("report.export.theme.harbor", "theme-harbor", "harbor"),
    SANDSTONE("report.export.theme.sandstone", "theme-sandstone", "sandstone");

    public static final String PROPERTY_KEY = "pangol1.export.theme";

    private final String labelKey;
    private final String cssClass;
    private final String id;

    ExportTheme(String labelKey, String cssClass, String id) {
        this.labelKey = labelKey;
        this.cssClass = cssClass;
        this.id = id;
    }

    public String getLabel() {
        return Lang.get(labelKey);
    }

    public String getCssClass() {
        return cssClass;
    }

    public String getId() {
        return id;
    }

    public static ExportTheme fromId(String raw) {
        if (raw == null || raw.isBlank()) {
            return LINEN;
        }

        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (ExportTheme theme : values()) {
            if (theme.id.equals(normalized)
                    || theme.cssClass.equalsIgnoreCase(normalized)
                    || theme.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return theme;
            }
        }

        return LINEN;
    }
}
