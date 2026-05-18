package fr.univrennes.istic.l2gen.application.gui.dialog.quickstart.askuser;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import fr.univrennes.istic.l2gen.application.core.config.Lang;
import fr.univrennes.istic.l2gen.application.gui.main.MainView;

public final class QuickStartDialog extends JDialog {

    private static final int CAROUSEL_WIDTH = 480;
    private static final int CAROUSEL_HEIGHT = 270;
    private static final int AUTO_ADVANCE_DELAY_MS = 4000;
    private static final String[] IMAGE_PATHS = {
            "/quickstart/main_sample.png",
            "/quickstart/quickstart_step2.png",
            "/quickstart/filter_add.png",
            "/quickstart/report_chart_setting.png",
            "/quickstart/export_theme.png",
    };

    private final Runnable onLaunch;
    final List<Image> carouselImages = new ArrayList<>();
    int currentImageIndex = 0;
    private CarouselPanel carouselPanel;
    private DotIndicatorPanel dotPanel;
    private Timer autoAdvanceTimer;

    public static void showDialog(MainView parent, Runnable onLaunch) {
        if (parent == null) {
            return;
        }

        Runnable show = () -> {
            Frame frame = parent instanceof Frame ? (Frame) parent : null;
            QuickStartDialog dialog = new QuickStartDialog(frame, onLaunch);
            dialog.setVisible(true);
        };

        if (SwingUtilities.isEventDispatchThread()) {
            show.run();
        } else {
            SwingUtilities.invokeLater(show);
        }
    }

    private QuickStartDialog(Frame parent, Runnable onLaunch) {
        super(parent, Lang.get("quickstart.dialog.title"), true);
        this.onLaunch = onLaunch;
        loadImages();
        build();
        pack();
        setLocationRelativeTo(parent);
        startAutoAdvance();
    }

    private void loadImages() {
        for (String path : IMAGE_PATHS) {
            URL imageUrl = getClass().getResource(path);
            if (imageUrl != null) {
                ImageIcon icon = new ImageIcon(imageUrl);
                Image scaled = icon.getImage().getScaledInstance(
                        CAROUSEL_WIDTH, CAROUSEL_HEIGHT, Image.SCALE_SMOOTH);
                carouselImages.add(scaled);
            }
        }
    }

    private void build() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        if (!carouselImages.isEmpty()) {
            root.add(buildCarouselSection(), BorderLayout.NORTH);
        }

        JLabel message = new JLabel(toHtml(Lang.get("quickstart.dialog.message")));
        message.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        root.add(message, BorderLayout.CENTER);

        root.add(buildFooter(), BorderLayout.SOUTH);
        setContentPane(root);

        bindArrowKeys(root);

        setMinimumSize(new Dimension(CAROUSEL_WIDTH + 24, getPreferredSize().height));
        setResizable(false);
    }

    private JPanel buildCarouselSection() {
        JPanel section = new JPanel(new BorderLayout(0, 6));

        carouselPanel = new CarouselPanel(this);
        carouselPanel.setPreferredSize(new Dimension(CAROUSEL_WIDTH, CAROUSEL_HEIGHT));

        JButton prevButton = buildNavButton("<");
        JButton nextButton = buildNavButton(">");

        prevButton.addActionListener(event -> navigateTo(currentImageIndex - 1));
        nextButton.addActionListener(event -> navigateTo(currentImageIndex + 1));

        JPanel carouselRow = new JPanel(new BorderLayout());
        carouselRow.add(prevButton, BorderLayout.WEST);
        carouselRow.add(carouselPanel, BorderLayout.CENTER);
        carouselRow.add(nextButton, BorderLayout.EAST);

        dotPanel = new DotIndicatorPanel(carouselImages.size());

        section.add(carouselRow, BorderLayout.CENTER);
        section.add(dotPanel, BorderLayout.SOUTH);

        return section;
    }

    private JButton buildNavButton(String label) {
        JButton button = new JButton(label);
        button.setPreferredSize(new Dimension(32, CAROUSEL_HEIGHT));
        button.setFocusable(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFont(button.getFont().deriveFont(16f));
        button.setForeground(new Color(100, 100, 100));
        return button;
    }

    private JPanel buildFooter() {
        JButton startButton = new JButton(Lang.get("quickstart.dialog.start"));
        JButton laterButton = new JButton(Lang.get("quickstart.dialog.later"));

        startButton.addActionListener(event -> {
            stopAutoAdvance();
            dispose();
            if (onLaunch != null) {
                onLaunch.run();
            }
        });
        laterButton.addActionListener(event -> {
            stopAutoAdvance();
            dispose();
        });

        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.X_AXIS));

        footer.add(laterButton);
        footer.add(Box.createHorizontalGlue());
        footer.add(startButton);

        getRootPane().setDefaultButton(startButton);
        return footer;
    }

    private void bindArrowKeys(JPanel root) {
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "prevImage");
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "nextImage");

        root.getActionMap().put("prevImage", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                navigateTo(currentImageIndex - 1);
            }
        });
        root.getActionMap().put("nextImage", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                navigateTo(currentImageIndex + 1);
            }
        });
    }

    private void navigateTo(int targetIndex) {
        if (carouselImages.isEmpty()) {
            return;
        }
        int imageCount = carouselImages.size();
        currentImageIndex = ((targetIndex % imageCount) + imageCount) % imageCount;
        carouselPanel.repaint();
        dotPanel.setActive(currentImageIndex);
        restartAutoAdvance();
    }

    private void startAutoAdvance() {
        if (carouselImages.size() <= 1) {
            return;
        }
        autoAdvanceTimer = new Timer(AUTO_ADVANCE_DELAY_MS, event -> navigateTo(currentImageIndex + 1));
        autoAdvanceTimer.setRepeats(true);
        autoAdvanceTimer.start();
    }

    private void stopAutoAdvance() {
        if (autoAdvanceTimer != null) {
            autoAdvanceTimer.stop();
        }
    }

    private void restartAutoAdvance() {
        stopAutoAdvance();
        startAutoAdvance();
    }

    private String toHtml(String text) {
        if (text == null) {
            return "";
        }
        String escaped = text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>");
        return "<html><div style='width:360px;'>" + escaped + "</div></html>";
    }
}