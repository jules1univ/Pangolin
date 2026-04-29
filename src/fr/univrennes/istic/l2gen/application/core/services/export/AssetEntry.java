package fr.univrennes.istic.l2gen.application.core.services.export;

import fr.univrennes.istic.l2gen.application.core.notebook.NoteBookValue;

public final class AssetEntry {
    private final NoteBookValue value;
    private String imagePath;
    private String chartSvgPath;
    private String chartRasterPath;

    public AssetEntry(NoteBookValue value) {
        this.value = value;
    }

    public NoteBookValue getValue() {
        return value;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getChartPath() {
        return chartSvgPath;
    }

    public void setChartPath(String chartSvgPath) {
        this.chartSvgPath = chartSvgPath;
    }

    public String getChartImagePath() {
        return chartRasterPath;
    }

    public void setChartImagePath(String chartRasterPath) {
        this.chartRasterPath = chartRasterPath;
    }
}