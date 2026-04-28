package fr.univrennes.istic.l2gen.application.gui.dialog.export;

import fr.univrennes.istic.l2gen.application.core.lang.Lang;

public enum ExportFormat {
    HTML("report.export.format.html", "html"),
    TXT("report.export.format.txt", "txt"),
    MD("report.export.format.md", "md"),
    PDF("report.export.format.pdf", "pdf");

    private final String displayName;
    private final String fileExtension;

    ExportFormat(String displayNameKey, String fileExtension) {
        this.displayName = Lang.get(displayNameKey);
        this.fileExtension = fileExtension;
    }

    public String getLabel() {
        return displayName;
    }

    public String getExtension() {
        return fileExtension;
    }
}