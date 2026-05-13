package fr.univrennes.istic.l2gen.application.gui.dialog.export;

import org.junit.Test;

import fr.univrennes.istic.l2gen.application.core.services.export.ExportTheme;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Component;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ExportDialogTest {

    @Test
    public void testBuildFormAndTitleAndThemeHandling() throws Exception {
        ExportDialog dialog = newDialogShell();

        JPanel form = invokePanel(dialog, "buildFormPanel");
        assertEquals(8, form.getComponentCount());
        assertNotNull(getFieldTyped(dialog, "formatBox", JComboBox.class));
        assertNotNull(getFieldTyped(dialog, "themeBox", JComboBox.class));
        assertNotNull(getFieldTyped(dialog, "titleField", JTextField.class));
        assertNotNull(getFieldTyped(dialog, "includeTitleBox", JCheckBox.class));
        assertNotNull(getFieldTyped(dialog, "openAfterExportBox", JCheckBox.class));

        JTextField titleField = getFieldTyped(dialog, "titleField", JTextField.class);
        titleField.setText("");
        invokeVoid(dialog, "updateFileTitle");
        assertFalse(titleField.getText().isBlank());

        titleField.setText("Mon export");
        invokeVoid(dialog, "updateFileTitle");
        assertEquals("Mon export", titleField.getText());

        JComboBox<ExportTheme> themeBox = getFieldTyped(dialog, "themeBox", JComboBox.class);
        themeBox.setSelectedItem(ExportTheme.SANDSTONE);
        invokeVoid(dialog, "applyThemeSelection");
        assertEquals("sandstone", System.getProperty(ExportTheme.PROPERTY_KEY));

        themeBox.setSelectedItem(null);
        invokeVoid(dialog, "applyThemeSelection");
        assertEquals(null, System.getProperty(ExportTheme.PROPERTY_KEY));
    }

    @Test
    public void testRefreshPreviewOnChangeRegistersListeners() throws Exception {
        ExportDialog dialog = newDialogShell();

        JTextField titleField = new JTextField(30);
        JComboBox<ExportFormat> formatBox = new JComboBox<>(ExportFormat.values());
        JComboBox<ExportTheme> themeBox = new JComboBox<>(ExportTheme.values());
        JCheckBox includeTitleBox = new JCheckBox();
        JCheckBox openAfterExportBox = new JCheckBox();

        setField(dialog, "titleField", titleField);
        setField(dialog, "formatBox", formatBox);
        setField(dialog, "themeBox", themeBox);
        setField(dialog, "includeTitleBox", includeTitleBox);
        setField(dialog, "openAfterExportBox", openAfterExportBox);
        setFieldValue(dialog, "titleField", titleField);
        setFieldValue(dialog, "formatBox", formatBox);
        setFieldValue(dialog, "themeBox", themeBox);
        setFieldValue(dialog, "includeTitleBox", includeTitleBox);
        setFieldValue(dialog, "openAfterExportBox", openAfterExportBox);

        int titleListenersBefore = ((javax.swing.text.AbstractDocument) titleField.getDocument())
                .getDocumentListeners().length;
        int formatListenersBefore = formatBox.getActionListeners().length;
        int themeListenersBefore = themeBox.getActionListeners().length;

        invokeVoid(dialog, "refreshPreviewOnChange");

        assertEquals(titleListenersBefore + 1,
                ((javax.swing.text.AbstractDocument) titleField.getDocument()).getDocumentListeners().length);
        assertEquals(formatListenersBefore + 1, formatBox.getActionListeners().length);
        assertEquals(themeListenersBefore + 1, themeBox.getActionListeners().length);
        assertEquals(1, includeTitleBox.getActionListeners().length);
        assertEquals(0, openAfterExportBox.getActionListeners().length);
    }

    @Test
    public void testPublicSignatureStillExists() throws Exception {
        assertTrue(JDialog.class.isAssignableFrom(ExportDialog.class));
        assertHasMethod("showDialog", Component.class);
    }

    private static ExportDialog newDialogShell() throws Exception {
        ExportDialog dialog = allocate(ExportDialog.class);
        setFieldValue(dialog, "formatBox", new JComboBox<>(ExportFormat.values()));
        setFieldValue(dialog, "themeBox", new JComboBox<>(ExportTheme.values()));
        setFieldValue(dialog, "titleField", new JTextField(30));
        setFieldValue(dialog, "includeTitleBox", new JCheckBox());
        setFieldValue(dialog, "openAfterExportBox", new JCheckBox());
        setFieldValue(dialog, "previewPanel", null);
        setFieldValue(dialog, "webView", null);
        return dialog;
    }

    private static void assertHasMethod(String name, Class<?>... types) {
        try {
            Method method = ExportDialog.class.getMethod(name, types);
            assertNotNull(method);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    private static JPanel invoke(ExportDialog dialog, String methodName) throws Exception {
        Method method = ExportDialog.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        return (JPanel) method.invoke(dialog);
    }

    private static JPanel invokePanel(Object dialog, String methodName) throws Exception {
        Method method = dialog.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return (JPanel) method.invoke(dialog);
    }

    private static void invokeVoid(Object dialog, String methodName) throws Exception {
        Method method = dialog.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(dialog);
    }

    @SuppressWarnings("unchecked")
    private static <T> T getFieldTyped(Object target, String name, Class<T> clazz) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (T) field.get(target);
    }

    private static void setFieldValue(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        try {
            field.set(target, value);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            unsafe().putObject(target, unsafe().objectFieldOffset(field), value);
        }
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        unsafe().putObject(target, unsafe().objectFieldOffset(field), value);
    }

    private static <T> T getField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (T) field.get(target);
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        return (T) unsafe().allocateInstance(type);
    }

    private static sun.misc.Unsafe unsafe() throws Exception {
        Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (sun.misc.Unsafe) field.get(null);
    }
}
