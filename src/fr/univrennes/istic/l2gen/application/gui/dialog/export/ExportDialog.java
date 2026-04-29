package fr.univrennes.istic.l2gen.application.gui.dialog.export;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.formdev.flatlaf.util.SystemFileChooser;

import fr.univrennes.istic.l2gen.application.core.lang.Lang;
import fr.univrennes.istic.l2gen.application.core.services.export.ExportService;
import fr.univrennes.istic.l2gen.application.core.services.export.ExportTheme;
import fr.univrennes.istic.l2gen.application.gui.GUIController;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebView;

public final class ExportDialog extends JDialog {

    private static final int DIALOG_WIDTH = 840;
    private static final int DIALOG_HEIGHT = 620;

    private final JComboBox<ExportFormat> formatBox = new JComboBox<>(ExportFormat.values());
    private final JComboBox<ExportTheme> themeBox = new JComboBox<>(ExportTheme.values());
    private final JTextField titleField = new JTextField(30);
    private final JCheckBox includeTitleBox = new JCheckBox(Lang.get("report.export.include_title"), true);
    private final JCheckBox openAfterExportBox = new JCheckBox(Lang.get("report.export.open_after"), false);

    private final JFXPanel previewPanel = new JFXPanel();
    private WebView webView;

    private final JButton exportButton = new JButton(Lang.get("report.export.action"));
    private final JButton cancelButton = new JButton(Lang.get("report.export.cancel"));

    public ExportDialog(Frame parentFrame) {
        super(parentFrame, Lang.get("report.export.title"), true);
        setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
        setMinimumSize(new Dimension(DIALOG_WIDTH, DIALOG_HEIGHT));
        setLocationRelativeTo(parentFrame);
        build();
    }

    private void build() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel form = buildFormPanel();
        root.add(form, BorderLayout.NORTH);

        JPanel previewWrapper = new JPanel(new BorderLayout());
        previewWrapper.setBorder(BorderFactory.createTitledBorder(Lang.get("report.export.preview")));
        previewWrapper.add(previewPanel, BorderLayout.CENTER);
        root.add(previewWrapper, BorderLayout.CENTER);

        Platform.runLater(() -> {
            webView = new WebView();
            webView.setContextMenuEnabled(false);
            previewPanel.setScene(new Scene(webView));
            updatePreview();
        });

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        cancelButton.addActionListener(event -> dispose());
        exportButton.addActionListener(event -> exportSelection());
        footer.add(cancelButton);
        footer.add(exportButton);

        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);

        refreshPreviewOnChange();
        updateFileTitle();
    }

    private JPanel buildFormPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;

        form.add(new JLabel(Lang.get("report.export.format")), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        formatBox.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value != null ? value.getLabel() : "");
            if (isSelected) {
                label.setOpaque(true);
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            }
            return label;
        });
        form.add(formatBox, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 0;
        form.add(new JLabel(Lang.get("report.export.theme")), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        themeBox.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value != null ? value.getLabel() : "");
            if (isSelected) {
                label.setOpaque(true);
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            }
            return label;
        });
        themeBox.setSelectedItem(ExportTheme.fromId(System.getProperty(ExportTheme.PROPERTY_KEY)));
        form.add(themeBox, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 0;
        form.add(new JLabel(Lang.get("report.export.file_title")), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(titleField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        form.add(includeTitleBox, gbc);

        gbc.gridy++;
        form.add(openAfterExportBox, gbc);

        return form;
    }

    private void refreshPreviewOnChange() {
        titleField.getDocument().addDocumentListener((ExportDocumentListener) e -> updatePreview());
        includeTitleBox.addActionListener(event -> updatePreview());
        themeBox.addActionListener(event -> updatePreview());
        formatBox.addActionListener(event -> updateFileTitle());
    }

    private void updateFileTitle() {
        if (!titleField.getText().isBlank()) {
            return;
        }
        titleField.setText(Lang.get("report.export.default_title"));
    }

    private void exportSelection() {
        ExportFormat format = (ExportFormat) formatBox.getSelectedItem();
        if (format == null) {
            return;
        }

        applyThemeSelection();

        String baseName = titleField.getText().trim();
        if (baseName.isBlank()) {
            baseName = Lang.get("report.export.default_title");
        }

        File file = showSaveDialog(baseName, "zip");
        if (file == null) {
            return;
        }

        try {
            ExportService.export(file.toPath(), format, baseName, includeTitleBox.isSelected());
            if (openAfterExportBox.isSelected() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }

            dispose();
        } catch (Exception e) {
            GUIController.getInstance().onOpenExceptionDialog(e);
        }
    }

    private File showSaveDialog(String baseName, String extension) {
        SystemFileChooser chooser = new SystemFileChooser();
        chooser.setDialogTitle(Lang.get("report.export.save.title"));
        chooser.setFileSelectionMode(SystemFileChooser.FILES_ONLY);
        chooser.setSelectedFile(new File(baseName + "." + extension));

        int result = chooser.showSaveDialog(this);
        if (result != SystemFileChooser.APPROVE_OPTION) {
            return null;
        }

        File file = chooser.getSelectedFile();
        if (file.exists()) {
            int overwrite = JOptionPane.showConfirmDialog(this,
                    Lang.get("report.export.overwrite"),
                    Lang.get("report.export.title"),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (overwrite != JOptionPane.YES_OPTION) {
                return null;
            }
        }

        return file;
    }

    private void updatePreview() {
        applyThemeSelection();
        String htmlContent = ExportService.exportHTML(titleField.getText(), includeTitleBox.isSelected());
        Platform.runLater(() -> {
            if (webView != null) {
                webView.getEngine().loadContent(htmlContent, "text/html");
            }
        });
    }

    private void applyThemeSelection() {
        ExportTheme theme = (ExportTheme) themeBox.getSelectedItem();
        if (theme == null) {
            System.clearProperty(ExportTheme.PROPERTY_KEY);
            return;
        }
        System.setProperty(ExportTheme.PROPERTY_KEY, theme.getId());
    }

    public static void showDialog(Component parent) {
        Frame frame = JOptionPane.getFrameForComponent(parent);
        SwingUtilities.invokeLater(() -> new ExportDialog(frame).setVisible(true));
    }
}