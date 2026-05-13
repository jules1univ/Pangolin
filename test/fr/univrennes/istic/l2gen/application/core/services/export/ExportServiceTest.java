package fr.univrennes.istic.l2gen.application.core.services.export;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;

import fr.univrennes.istic.l2gen.application.core.notebook.NoteBookChart;
import fr.univrennes.istic.l2gen.application.core.notebook.NoteBookImage;
import fr.univrennes.istic.l2gen.application.core.notebook.NoteBookText;
import fr.univrennes.istic.l2gen.application.core.services.notebook.NoteBookService;
import fr.univrennes.istic.l2gen.visustats.view.DataViewType;
import fr.univrennes.istic.l2gen.visustats.view.datagroup.axis.DataAxisViewScaleType;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests pour ExportService
 */
public class ExportServiceTest {

    @Before
    @After
    public void clearNotebookValues() throws Exception {
        clearStaticList("values");
        clearStaticList("undoStack");
        clearStaticList("redoStack");
    }

    @Test
    public void testExportHtmlPreviewWithTextImageAndChart() throws Exception {
        Path tempImage = createTempPng();
        try {
            NoteBookService.add(new NoteBookText("Bonjour export"));
            NoteBookService.add(new NoteBookImage(tempImage.toFile()));
            NoteBookService.add(createChartWithCachedSvg());

            String html = ExportService.exportHTML("Mon rapport", true);

            assertTrue(html.contains("Mon rapport"));
            assertTrue(html.contains("nb-text"));
            assertTrue(html.contains("Bonjour export"));
            assertTrue(html.contains("nb-image"));
            assertTrue(html.contains("data:image/png;base64,"));
            assertTrue(html.contains("nb-chart"));
            assertTrue(html.contains("<svg"));
        } finally {
            Files.deleteIfExists(tempImage);
        }
    }

    @Test
    public void testExportHtmlPreviewEmptyNotebook() {
        String html = ExportService.exportHTML("Vide", true);

        assertTrue(html.contains("Vide"));
        assertTrue(html.contains("class=\"nb-block nb-empty\""));
        assertFalse(html.contains("class=\"nb-block nb-text\""));
        assertFalse(html.contains("class=\"nb-block nb-image\""));
        assertFalse(html.contains("class=\"nb-block nb-chart\""));
    }

    private static Path createTempPng() throws Exception {
        Path temp = Files.createTempFile("export-service-test-", ".png");
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, new Color(255, 0, 0, 255).getRGB());
        ImageIO.write(image, "png", temp.toFile());
        return temp;
    }

    private static NoteBookChart createChartWithCachedSvg() throws Exception {
        NoteBookChart chart = new NoteBookChart(
                DataViewType.BAR,
                "Titre chart",
                false,
                true,
                false,
                3,
                4,
                DataAxisViewScaleType.LINEAR,
                true,
                "Axe X",
                true,
                "Axe Y",
                null,
                Optional.empty(),
                0,
                0,
                true,
                false,
                new ArrayList<>());

        Field cachedField = NoteBookChart.class.getDeclaredField("cachedSVG");
        cachedField.setAccessible(true);
        cachedField.set(chart, "<svg><rect width='10' height='10'/></svg>");

        Field labelsField = NoteBookChart.class.getDeclaredField("labels");
        labelsField.setAccessible(true);
        List<String> labels = new ArrayList<>();
        labels.add("Serie 1");
        labelsField.set(chart, labels);

        return chart;
    }

    private static void clearStaticList(String fieldName) throws Exception {
        Field field = NoteBookService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        Object value = field.get(null);
        if (value instanceof List<?> list) {
            list.clear();
        }
    }
}
