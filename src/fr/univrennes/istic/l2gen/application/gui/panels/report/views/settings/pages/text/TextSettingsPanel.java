package fr.univrennes.istic.l2gen.application.gui.panels.report.views.settings.pages.text;

import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JColorChooser;
import javax.swing.text.rtf.RTFEditorKit;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.MutableAttributeSet;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;

import fr.univrennes.istic.l2gen.application.core.config.Ico;
import fr.univrennes.istic.l2gen.application.core.config.Lang;
import fr.univrennes.istic.l2gen.application.core.notebook.NoteBookText;
import fr.univrennes.istic.l2gen.application.core.notebook.NoteBookValue;
import fr.univrennes.istic.l2gen.application.gui.panels.report.views.settings.SettingSectionPanel;
import fr.univrennes.istic.l2gen.application.gui.panels.report.views.settings.pages.IReportSettingPanel;

public final class TextSettingsPanel extends SettingSectionPanel implements IReportSettingPanel {
    private final JTextPane contentArea;
    private final RTFEditorKit rtfKit = new RTFEditorKit();

    private static final String[] FONT_SIZES = { "8", "10", "12", "14", "16", "18", "24", "36" };

    public TextSettingsPanel() {
        super(Lang.get("report.settings.text"));

        contentArea = new JTextPane();
        contentArea.setEditorKit(rtfKit);

        JToolBar toolbar = buildToolbar();

        addRow(toolbar);
        addRow(contentArea);
    }

    private JToolBar buildToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        JButton boldButton = new JButton(Ico.get("icons/bold.svg"));
        boldButton.setFont(boldButton.getFont().deriveFont(Font.BOLD));
        boldButton.setToolTipText(Lang.get("report.settings.text.bold"));
        boldButton.addActionListener(event -> applyBold());

        JButton italicButton = new JButton(Ico.get("icons/italic.svg"));
        italicButton.setFont(italicButton.getFont().deriveFont(Font.ITALIC));
        italicButton.setToolTipText(Lang.get("report.settings.text.italic"));
        italicButton.addActionListener(event -> applyItalic());

        JButton underlineButton = new JButton(Ico.get("icons/underlined.svg"));
        underlineButton.setToolTipText(Lang.get("report.settings.text.underline"));
        underlineButton.addActionListener(event -> applyUnderline());

        JButton strikethroughButton = new JButton(Ico.get("icons/strikethrough.svg"));
        strikethroughButton.setToolTipText(Lang.get("report.settings.text.strikethrough"));
        strikethroughButton.addActionListener(event -> applyStrikethrough());

        JButton alignLeftButton = new JButton(Ico.get("icons/alignleft.svg"));
        alignLeftButton.setToolTipText(Lang.get("report.settings.text.alignleft"));
        alignLeftButton.addActionListener(event -> applyAlignment(StyleConstants.ALIGN_LEFT));

        JButton alignCenterButton = new JButton(Ico.get("icons/aligncenter.svg"));
        alignCenterButton.setToolTipText(Lang.get("report.settings.text.aligncenter"));
        alignCenterButton.addActionListener(event -> applyAlignment(StyleConstants.ALIGN_CENTER));

        JButton alignRightButton = new JButton(Ico.get("icons/alignright.svg"));
        alignRightButton.setToolTipText(Lang.get("report.settings.text.alignright"));
        alignRightButton.addActionListener(event -> applyAlignment(StyleConstants.ALIGN_RIGHT));

        JButton textColorButton = new JButton(Ico.get("icons/textcolor.svg"));
        textColorButton.setToolTipText(Lang.get("report.settings.text.textcolor"));
        textColorButton.addActionListener(event -> applyTextColor());

        JButton backgroundColorButton = new JButton(Ico.get("icons/textbackground.svg"));
        backgroundColorButton.setToolTipText(Lang.get("report.settings.text.backgroundcolor"));
        backgroundColorButton.addActionListener(event -> applyBackgroundColor());

        List<String> fontFamilies = new ArrayList<>();
        for (String font : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            fontFamilies.add(font);
        }
        JComboBox<String> fontFamilyCombo = new JComboBox<>(fontFamilies.toArray(new String[0]));
        fontFamilyCombo.setToolTipText(Lang.get("report.settings.text.fontfamily"));
        fontFamilyCombo.addActionListener(event -> {
            String selectedFamily = (String) fontFamilyCombo.getSelectedItem();
            if (selectedFamily != null) {
                applyFontFamily(selectedFamily);
            }
        });

        JComboBox<String> fontSizeCombo = new JComboBox<>(FONT_SIZES);
        fontSizeCombo.setSelectedItem("12");
        fontSizeCombo.setToolTipText(Lang.get("report.settings.text.fontsize"));
        fontSizeCombo.addActionListener(event -> {
            String selectedSize = (String) fontSizeCombo.getSelectedItem();
            if (selectedSize != null) {
                try {
                    applyFontSize(Integer.parseInt(selectedSize));
                } catch (NumberFormatException ignored) {
                }
            }
        });

        toolbar.add(fontFamilyCombo);
        toolbar.add(fontSizeCombo);
        toolbar.addSeparator();
        toolbar.add(boldButton);
        toolbar.add(italicButton);
        toolbar.add(underlineButton);
        toolbar.add(strikethroughButton);
        toolbar.addSeparator();
        toolbar.add(alignLeftButton);
        toolbar.add(alignCenterButton);
        toolbar.add(alignRightButton);
        toolbar.addSeparator();
        toolbar.add(textColorButton);
        toolbar.add(backgroundColorButton);

        return toolbar;
    }

    private void applyBold() {
        MutableAttributeSet attributes = new SimpleAttributeSet();
        boolean isBold = StyleConstants.isBold(contentArea.getCharacterAttributes());
        StyleConstants.setBold(attributes, !isBold);
        applyCharacterAttributes(attributes);
    }

    private void applyItalic() {
        MutableAttributeSet attributes = new SimpleAttributeSet();
        boolean isItalic = StyleConstants.isItalic(contentArea.getCharacterAttributes());
        StyleConstants.setItalic(attributes, !isItalic);
        applyCharacterAttributes(attributes);
    }

    private void applyUnderline() {
        MutableAttributeSet attributes = new SimpleAttributeSet();
        boolean isUnderline = StyleConstants.isUnderline(contentArea.getCharacterAttributes());
        StyleConstants.setUnderline(attributes, !isUnderline);
        applyCharacterAttributes(attributes);
    }

    private void applyStrikethrough() {
        MutableAttributeSet attributes = new SimpleAttributeSet();
        boolean isStrikethrough = StyleConstants.isStrikeThrough(contentArea.getCharacterAttributes());
        StyleConstants.setStrikeThrough(attributes, !isStrikethrough);
        applyCharacterAttributes(attributes);
    }

    private void applyAlignment(int alignment) {
        MutableAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setAlignment(attributes, alignment);
        StyledDocument document = contentArea.getStyledDocument();
        int selectionStart = contentArea.getSelectionStart();
        int selectionEnd = contentArea.getSelectionEnd();
        document.setParagraphAttributes(selectionStart, selectionEnd - selectionStart, attributes, false);
    }

    private void applyTextColor() {
        Color selectedColor = JColorChooser.showDialog(this, "Couleur du texte", Color.BLACK);
        if (selectedColor != null) {
            MutableAttributeSet attributes = new SimpleAttributeSet();
            StyleConstants.setForeground(attributes, selectedColor);
            applyCharacterAttributes(attributes);
        }
    }

    private void applyBackgroundColor() {
        Color selectedColor = JColorChooser.showDialog(this, "Couleur de fond", Color.WHITE);
        if (selectedColor != null) {
            MutableAttributeSet attributes = new SimpleAttributeSet();
            StyleConstants.setBackground(attributes, selectedColor);
            applyCharacterAttributes(attributes);
        }
    }

    private void applyFontFamily(String fontFamily) {
        MutableAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setFontFamily(attributes, fontFamily);
        applyCharacterAttributes(attributes);
    }

    private void applyFontSize(int fontSize) {
        MutableAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setFontSize(attributes, fontSize);
        applyCharacterAttributes(attributes);
    }

    private void applyCharacterAttributes(MutableAttributeSet attributes) {
        int selectionStart = contentArea.getSelectionStart();
        int selectionEnd = contentArea.getSelectionEnd();
        if (selectionStart != selectionEnd) {
            contentArea.getStyledDocument().setCharacterAttributes(
                    selectionStart,
                    selectionEnd - selectionStart,
                    attributes,
                    false);
        } else {
            contentArea.setCharacterAttributes(attributes, false);
        }
    }

    @Override
    public NoteBookValue createNoteBook() {
        String plain = contentArea.getText();
        String rtfBase64 = null;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            rtfKit.write(baos, contentArea.getDocument(), 0, contentArea.getDocument().getLength());
            byte[] rtfBytes = baos.toByteArray();
            rtfBase64 = Base64.getEncoder().encodeToString(rtfBytes);
        } catch (Exception e) {
        }

        return new NoteBookText(plain, rtfBase64);
    }

    @Override
    public void loadNoteBook(NoteBookValue value) {
        if (value instanceof NoteBookText) {
            NoteBookText text = (NoteBookText) value;

            if (text.getRtf() != null) {
                try {
                    contentArea.setText("");
                    byte[] rtfBytes = Base64.getDecoder().decode(text.getRtf());
                    rtfKit.read(new ByteArrayInputStream(rtfBytes), contentArea.getDocument(), 0);
                } catch (Exception e) {
                    contentArea.setText(text.getText());
                }
            } else {
                contentArea.setText(text.getText());
            }
        }
    }
}