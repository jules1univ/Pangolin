package fr.univrennes.istic.l2gen.application.core.services.export;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.stream.Stream;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

import fr.univrennes.istic.l2gen.application.core.config.Log;
import fr.univrennes.istic.l2gen.application.core.lang.Lang;
import fr.univrennes.istic.l2gen.application.core.notebook.NoteBookChart;
import fr.univrennes.istic.l2gen.application.core.notebook.NoteBookImage;
import fr.univrennes.istic.l2gen.application.core.notebook.NoteBookText;
import fr.univrennes.istic.l2gen.application.core.notebook.NoteBookValue;
import fr.univrennes.istic.l2gen.application.core.services.notebook.NoteBookService;
import fr.univrennes.istic.l2gen.application.gui.dialog.export.ExportFormat;

public final class ExportService {

    private static final ExportTheme DEFAULT_THEME = ExportTheme.LINEN;
    private static final String DEFAULT_TITLE = "report";
    private static final Pattern STYLE_FILL_HEX8 = Pattern.compile("fill\\s*:\\s*#([0-9a-fA-F]{8})");
    private static final Pattern STYLE_STROKE_HEX8 = Pattern.compile("stroke\\s*:\\s*#([0-9a-fA-F]{8})");
    private static final Pattern ATTR_FILL_HEX8 = Pattern.compile("fill=(\\\"|')#([0-9a-fA-F]{8})\\1");
    private static final Pattern ATTR_STROKE_HEX8 = Pattern.compile("stroke=(\\\"|')#([0-9a-fA-F]{8})\\1");

    private ExportService() {
    }

    public static String exportHTML(String title, boolean includeTitle) {
        List<NoteBookValue> values = NoteBookService.getValues();
        return buildHtmlPreview(resolveTitle(title), includeTitle, values);
    }

    public static void export(Path file, ExportFormat format, String title, boolean includeTitleBox)
            throws Exception {
        if (file == null || format == null) {
            return;
        }

        String resolvedTitle = resolveTitle(title);
        String fileBaseName = stripExtension(file.getFileName().toString());
        String safeBaseName = sanitizeFileBaseName(fileBaseName);
        Path zipPath = ensureZipExtension(file);

        Path tempDir = Files.createTempDirectory("pangol1-export-");
        try {
            Path imagesDir = tempDir.resolve("images");
            Path chartsDir = tempDir.resolve("charts");
            Files.createDirectories(imagesDir);
            Files.createDirectories(chartsDir);

            List<NoteBookValue> values = NoteBookService.getValues();
            boolean rasterizeCharts = requiresRasterizedSvg(format);
            List<AssetEntry> assets = collectAssets(values, imagesDir, chartsDir, rasterizeCharts);

            String html = buildHtmlExport(resolvedTitle, includeTitleBox, assets, format);
            Path htmlFile = tempDir.resolve(safeBaseName + ".html");
            Files.writeString(htmlFile, html, StandardCharsets.UTF_8);

            Path documentPath;
            if (format == ExportFormat.MD) {
                String markdown = buildMarkdown(resolvedTitle, includeTitleBox, assets);
                documentPath = tempDir.resolve(safeBaseName + ".md");
                Files.writeString(documentPath, markdown, StandardCharsets.UTF_8);
            } else if (format == ExportFormat.HTML) {
                documentPath = htmlFile;
            } else {
                documentPath = tempDir.resolve(safeBaseName + "." + format.getExtension());
                convertHtml(htmlFile, documentPath);
            }

            writeZip(zipPath, documentPath, imagesDir, chartsDir);
        } finally {
            deleteRecursively(tempDir);
        }
    }

    private static String buildHtmlPreview(String title, boolean includeTitle, List<NoteBookValue> values) {
        StringBuilder body = new StringBuilder();

        if (values.isEmpty()) {
            body.append("<div class=\"nb-block nb-empty\">")
                    .append(htmlText("report.export.empty"))
                    .append("</div>");
        } else {
            for (NoteBookValue value : values) {
                appendPreviewBlock(body, value);
            }
        }

        return wrapHtmlDocument(title, includeTitle, body.toString());
    }

    private static String buildHtmlExport(String title, boolean includeTitle, List<AssetEntry> assets,
            ExportFormat format) {
        StringBuilder body = new StringBuilder();

        if (assets.isEmpty()) {
            body.append("<div class=\"nb-block nb-empty\">")
                    .append(htmlText("report.export.empty"))
                    .append("</div>");
        } else {
            for (AssetEntry entry : assets) {
                appendExportBlock(body, entry, format);
            }
        }

        return wrapHtmlDocument(title, includeTitle, body.toString());
    }

    private static void appendPreviewBlock(StringBuilder body, NoteBookValue value) {
        if (value instanceof NoteBookText text) {
            body.append("<div class=\"nb-block nb-text\">");
            text.exportHTML(body);
            body.append("</div>");
            return;
        }

        if (value instanceof NoteBookImage image) {
            body.append("<div class=\"nb-block nb-image\">");
            image.exportHTML(body);
            body.append("</div>");
            return;
        }

        if (value instanceof NoteBookChart chart) {
            body.append("<div class=\"nb-block nb-chart\">");
            chart.exportHTML(body);
            body.append("</div>");
        }
    }

    private static void appendExportBlock(StringBuilder body, AssetEntry entry, ExportFormat format) {
        NoteBookValue value = entry.getValue();
        if (value instanceof NoteBookText text) {
            body.append("<div class=\"nb-block nb-text\">");
            text.exportHTML(body);
            body.append("</div>");
            return;
        }

        if (value instanceof NoteBookImage) {
            if (entry.getImagePath() == null) {
                body.append("<div class=\"nb-block nb-warning\">")
                        .append(htmlText("report.export.missing_image"))
                        .append("</div>");
                return;
            }
            body.append("<div class=\"nb-block nb-image\"><img src=\"")
                    .append(entry.getImagePath())
                    .append("\" alt=\"")
                    .append(htmlText("report.export.alt.image"))
                    .append("\" /></div>");
            return;
        }

        if (value instanceof NoteBookChart) {
            String chartPath = entry.getChartImagePath() != null && requiresRasterizedSvg(format)
                    ? entry.getChartImagePath()
                    : entry.getChartPath();
            if (chartPath == null) {
                body.append("<div class=\"nb-block nb-warning\">")
                        .append(htmlText("report.export.missing_chart"))
                        .append("</div>");
                return;
            }
            body.append("<div class=\"nb-block nb-chart\"><img src=\"")
                    .append(chartPath)
                    .append("\" alt=\"")
                    .append(htmlText("report.export.alt.chart"))
                    .append("\" /></div>");
        }
    }

    private static String buildMarkdown(String title, boolean includeTitle, List<AssetEntry> assets) {
        StringBuilder markdown = new StringBuilder();
        if (includeTitle && !title.isBlank()) {
            markdown.append("# ").append(title).append("\n\n");
        }

        if (assets.isEmpty()) {
            markdown.append("_").append(Lang.get("report.export.empty")).append("._\n");
        } else {
            for (AssetEntry entry : assets) {
                NoteBookValue value = entry.getValue();
                if (value instanceof NoteBookText text) {
                    markdown.append(text.getText()).append("\n\n");
                } else if (value instanceof NoteBookImage) {
                    if (entry.getImagePath() == null) {
                        markdown.append("_").append(Lang.get("report.export.missing_image"))
                                .append("._\n\n");
                    } else {
                        markdown.append("![")
                                .append(Lang.get("report.export.alt.image"))
                                .append("](")
                                .append(entry.getImagePath())
                                .append(")\n\n");
                    }
                } else if (value instanceof NoteBookChart) {
                    String chartPath = entry.getChartPath() != null
                            ? entry.getChartPath()
                            : entry.getChartImagePath();
                    if (chartPath == null) {
                        markdown.append("_").append(Lang.get("report.export.missing_chart"))
                                .append("._\n\n");
                    } else {
                        markdown.append("![")
                                .append(Lang.get("report.export.alt.chart"))
                                .append("](")
                                .append(chartPath)
                                .append(")\n\n");
                    }
                }
            }
        }

        return markdown.toString();
    }

    private static String wrapHtmlDocument(String title, boolean includeTitle, String bodyHtml) {
        String template = loadHtmlTemplate();
        String header = includeTitle ? buildHeaderHtml(title) : "";

        if (template == null || template.isBlank()) {
            return buildFallbackHtml(title, includeTitle, bodyHtml);
        }

        return template
                .replace("{{TITLE}}", escapeHtml(title))
                .replace("{{THEME_CLASS}}", resolveThemeClass())
                .replace("{{LANG}}", escapeHtml(resolveLangTag()))
                .replace("{{HEADER}}", header)
                .replace("{{BODY}}", bodyHtml);
    }

    private static String loadHtmlTemplate() {
        try (InputStream input = ExportService.class.getResourceAsStream("/export/page.html")) {
            if (input == null) {
                Log.debug("Missing export template: /export/page.html");
                return null;
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Log.debug("Failed to read export template", e);
            return null;
        }
    }

    private static String buildHeaderHtml(String title) {
        String safeTitle = escapeHtml(title);
        String headerLabel = htmlText("report.export.header");
        return "    <header class=\"header\">\n"
                + "      <div class=\"sub\">" + headerLabel + "</div>\n"
                + "      <h1>" + safeTitle + "</h1>\n"
                + "    </header>\n";
    }

    private static String buildFallbackHtml(String title, boolean includeTitle, String bodyHtml) {
        StringBuilder html = new StringBuilder();
        html.append("<!doctype html>\n");
        html.append("<html lang=\"").append(escapeHtml(resolveLangTag())).append("\">\n");
        html.append("<head>\n");
        html.append("  <meta charset=\"utf-8\" />\n");
        html.append("  <title>").append(escapeHtml(title)).append("</title>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        if (includeTitle) {
            html.append("<h1>").append(escapeHtml(title)).append("</h1>\n");
        }
        html.append(bodyHtml);
        html.append("</body>\n");
        html.append("</html>\n");
        return html.toString();
    }

    private static List<AssetEntry> collectAssets(List<NoteBookValue> values, Path imagesDir, Path chartsDir,
            boolean rasterizeCharts) throws IOException {
        List<AssetEntry> assets = new ArrayList<>();
        int imageIndex = 1;
        int chartIndex = 1;

        for (NoteBookValue value : values) {
            AssetEntry entry = new AssetEntry(value);
            if (value instanceof NoteBookImage image) {
                entry.setImagePath(copyImage(image, imagesDir, imageIndex++));
            } else if (value instanceof NoteBookChart chart) {
                String svg = chart.getSVG();
                entry.setChartPath(writeChartSvg(svg, chartsDir, chartIndex));
                if (rasterizeCharts) {
                    entry.setChartImagePath(writeChartPng(svg, chartsDir, chartIndex));
                }
                chartIndex++;
            }
            assets.add(entry);
        }

        return assets;
    }

    private static String copyImage(NoteBookImage image, Path imagesDir, int index) throws IOException {
        File sourceFile = image.getPath();
        if (sourceFile == null) {
            return null;
        }

        Path sourcePath = sourceFile.toPath();
        if (!Files.exists(sourcePath)) {
            return null;
        }

        String extension = getExtension(sourceFile.getName());
        if (extension.isBlank()) {
            extension = "png";
        }

        String fileName = String.format(Locale.ROOT, "image-%03d.%s", index,
                extension.toLowerCase(Locale.ROOT));
        Path targetPath = imagesDir.resolve(fileName);
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        return "images/" + fileName;
    }

    private static String writeChartSvg(String svg, Path chartsDir, int index) throws IOException {
        String svgName = String.format(Locale.ROOT, "chart-%03d.svg", index);
        Files.writeString(chartsDir.resolve(svgName), svg, StandardCharsets.UTF_8);
        return "charts/" + svgName;
    }

    private static String writeChartPng(String svg, Path chartsDir, int index) throws IOException {
        String pngName = String.format(Locale.ROOT, "chart-%03d.png", index);
        Path pngPath = chartsDir.resolve(pngName);
        String normalizedSvg = normalizeSvgAlpha(svg);
        renderSvgToPng(normalizedSvg, pngPath);
        return "charts/" + pngName;
    }

    private static void renderSvgToPng(String svg, Path pngPath) throws IOException {
        PNGTranscoder transcoder = new PNGTranscoder();
        try (OutputStream output = Files.newOutputStream(pngPath)) {
            TranscoderInput input = new TranscoderInput(new StringReader(svg));
            TranscoderOutput transcoderOutput = new TranscoderOutput(output);
            transcoder.transcode(input, transcoderOutput);
        } catch (TranscoderException e) {
            throw new IOException("Failed to rasterize SVG", e);
        }
    }

    private static void convertHtml(Path htmlFile, Path outputFile) throws IOException {
        try (OutputStream output = Files.newOutputStream(outputFile)) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withFile(htmlFile.toFile());
            builder.toStream(output);
            builder.run();
        } catch (Exception e) {
            throw new IOException("Failed to convert HTML to PDF", e);
        }
    }

    private static void writeZip(Path zipPath, Path documentPath, Path imagesDir, Path chartsDir)
            throws IOException {
        Path parent = zipPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            addZipEntry(zip, documentPath, documentPath.getFileName().toString());
            zipDirectory(zip, imagesDir, "images");
            zipDirectory(zip, chartsDir, "charts");
        }
    }

    private static void addZipEntry(ZipOutputStream zip, Path file, String entryName) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        zip.putNextEntry(entry);
        Files.copy(file, zip);
        zip.closeEntry();
    }

    private static void zipDirectory(ZipOutputStream zip, Path dir, String prefix) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }

        try (Stream<Path> stream = Files.walk(dir)) {
            stream.filter(Files::isRegularFile).forEach(path -> {
                String relative = dir.relativize(path).toString().replace(File.separatorChar, '/');
                String entryName = prefix + "/" + relative;
                try {
                    addZipEntry(zip, path, entryName);
                } catch (IOException e) {
                    Log.debug("Failed to add file to zip: " + path, e);
                }
            });
        }
    }

    private static void deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }

        try (Stream<Path> stream = Files.walk(root)) {
            stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    Log.debug("Failed to delete export temp path: " + path, e);
                }
            });
        } catch (IOException e) {
            Log.debug("Failed to clean export directory", e);
        }
    }

    private static String resolveTitle(String title) {
        if (title == null || title.isBlank()) {
            String defaultTitle = Lang.get("report.export.default_title");
            if (defaultTitle != null && !defaultTitle.isBlank()
                    && !(defaultTitle.startsWith("<") && defaultTitle.endsWith(">"))) {
                return defaultTitle;
            }
            return "Report";
        }
        return title.trim();
    }

    private static String resolveThemeClass() {
        String themeId = System.getProperty(ExportTheme.PROPERTY_KEY, DEFAULT_THEME.getId());
        return ExportTheme.fromId(themeId).getCssClass();
    }

    private static String resolveLangTag() {
        String lang = Locale.getDefault().getLanguage();
        if (lang == null || lang.isBlank()) {
            return "en";
        }
        return lang.toLowerCase(Locale.ROOT);
    }

    private static Path ensureZipExtension(Path file) {
        String name = file.getFileName().toString();
        if (name.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            return file;
        }
        return file.resolveSibling(name + ".zip");
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        if (dot <= 0) {
            return name;
        }
        return name.substring(0, dot);
    }

    private static String sanitizeFileBaseName(String name) {
        if (name == null || name.isBlank()) {
            return DEFAULT_TITLE;
        }

        String trimmed = name.trim();
        StringBuilder safe = new StringBuilder();
        boolean lastWasSeparator = false;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            boolean isSafe = (c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || c == '-' || c == '_' || c == '.';
            if (isSafe) {
                safe.append(c);
                lastWasSeparator = false;
            } else if (!lastWasSeparator) {
                safe.append('_');
                lastWasSeparator = true;
            }
        }

        String result = safe.toString();
        if (result.isBlank()) {
            return DEFAULT_TITLE;
        }
        return result;
    }

    private static boolean requiresRasterizedSvg(ExportFormat format) {
        return format != ExportFormat.HTML && format != ExportFormat.MD;
    }

    private static String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1);
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String htmlText(String key) {
        return escapeHtml(Lang.get(key));
    }

    private static String normalizeSvgAlpha(String svg) {
        if (svg == null || svg.isBlank()) {
            return svg;
        }

        String updated = replaceStyleHexAlpha(svg, STYLE_FILL_HEX8, "fill", "fill-opacity");
        updated = replaceStyleHexAlpha(updated, STYLE_STROKE_HEX8, "stroke", "stroke-opacity");
        updated = replaceAttributeHexAlpha(updated, ATTR_FILL_HEX8, "fill", "fill-opacity");
        updated = replaceAttributeHexAlpha(updated, ATTR_STROKE_HEX8, "stroke", "stroke-opacity");
        return updated;
    }

    private static String replaceStyleHexAlpha(String svg, Pattern pattern, String attrName, String opacityAttr) {
        Matcher matcher = pattern.matcher(svg);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            String rgb = "#" + hex.substring(0, 6);
            String opacity = formatOpacity(hex);
            String replacement = attrName + ":" + rgb + ";" + opacityAttr + ":" + opacity;
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String replaceAttributeHexAlpha(String svg, Pattern pattern, String attrName, String opacityAttr) {
        Matcher matcher = pattern.matcher(svg);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(2);
            String rgb = "#" + hex.substring(0, 6);
            String opacity = formatOpacity(hex);
            String replacement = attrName + "=\"" + rgb + "\" "
                    + opacityAttr + "=\"" + opacity + "\"";
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String formatOpacity(String hex) {
        int alpha = Integer.parseInt(hex.substring(6, 8), 16);
        double opacity = alpha / 255.0;
        return String.format(Locale.ROOT, "%.3f", opacity);
    }

    private static final class AssetEntry {
        private final NoteBookValue value;
        private String imagePath;
        private String chartPath;
        private String chartImagePath;

        private AssetEntry(NoteBookValue value) {
            this.value = value;
        }

        private NoteBookValue getValue() {
            return value;
        }

        private String getImagePath() {
            return imagePath;
        }

        private void setImagePath(String imagePath) {
            this.imagePath = imagePath;
        }

        private String getChartPath() {
            return chartPath;
        }

        private void setChartPath(String chartPath) {
            this.chartPath = chartPath;
        }

        private String getChartImagePath() {
            return chartImagePath;
        }

        private void setChartImagePath(String chartImagePath) {
            this.chartImagePath = chartImagePath;
        }
    }
}
