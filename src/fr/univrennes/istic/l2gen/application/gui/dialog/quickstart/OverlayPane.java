package fr.univrennes.istic.l2gen.application.gui.dialog.quickstart;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Supplier;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import fr.univrennes.istic.l2gen.application.core.config.Lang;

public final class OverlayPane extends JComponent {

    private final QuickStart quickStart;
    private final JPanel bubble;
    private final JLabel titleLabel;
    private final JLabel bodyLabel;
    private final JLabel stepLabel;
    private final JButton skipButton;
    private final JButton nextButton;
    private final JButton doneButton;

    private Supplier<Component> targetSupplier;

    public OverlayPane(QuickStart quickStart) {
        this.quickStart = quickStart;
        setLayout(null);
        setOpaque(false);

        titleLabel = new JLabel();
        titleLabel.setFont(titleLabel.getFont().deriveFont(titleLabel.getFont().getStyle() | java.awt.Font.BOLD));

        bodyLabel = new JLabel();

        stepLabel = new JLabel();
        stepLabel.setForeground(UIManager.getColor("Label.disabledForeground"));

        skipButton = new JButton(Lang.get("quickstart.action.skip"));
        nextButton = new JButton(Lang.get("quickstart.action.next"));
        doneButton = new JButton(Lang.get("quickstart.action.done"));

        skipButton.addActionListener(event -> this.quickStart.skip());
        nextButton.addActionListener(event -> this.quickStart.nextStep());
        doneButton.addActionListener(event -> this.quickStart.finish());

        bubble = buildBubble();
        add(bubble);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                event.consume();
            }
        });

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                "quickstart.skip");
        getActionMap().put("quickstart.skip", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                OverlayPane.this.quickStart.skip();
            }
        });
    }

    @Override
    public boolean contains(int x, int y) {
        if (bubble.getBounds().contains(x, y)) {
            return true;
        }

        Rectangle target = getTargetBounds();
        if (target != null && target.contains(x, y)) {
            return false;
        }

        return super.contains(x, y);
    }

    private JPanel buildBubble() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(true);
        panel.setBackground(resolvePanelBackground());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIManager.getColor("Separator.foreground")),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(titleLabel, BorderLayout.WEST);
        header.add(stepLabel, BorderLayout.EAST);

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.add(bodyLabel, BorderLayout.CENTER);

        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.X_AXIS));
        footer.setOpaque(false);

        footer.add(skipButton);
        footer.add(Box.createHorizontalGlue());
        footer.add(nextButton);
        footer.add(doneButton);

        panel.add(header);
        panel.add(Box.createVerticalStrut(6));
        panel.add(body);
        panel.add(Box.createVerticalStrut(10));
        panel.add(footer);
        return panel;
    }

    private Color resolvePanelBackground() {
        Color background = UIManager.getColor("Panel.background");
        if (background == null) {
            background = new Color(250, 250, 250);
        }
        return background;
    }

    public void showStep(Step step, int currentIndex, int totalSteps) {
        targetSupplier = step.targetSupplier();
        titleLabel.setText(step.title());
        bodyLabel.setText(toHtml(step.body()));
        stepLabel.setText(currentIndex + "/" + totalSteps);

        boolean isLast = currentIndex == totalSteps;
        skipButton.setVisible(!isLast);
        nextButton.setVisible(!isLast);
        doneButton.setVisible(isLast);

        bubble.revalidate();
        bubble.repaint();
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
        return "<html><div style='width:" + QuickStart.BUBBLE_MAX_WIDTH + "px;'>" + escaped + "</div></html>";
    }

    private Rectangle getTargetBounds() {
        if (targetSupplier == null) {
            return null;
        }
        Component target = targetSupplier.get();
        if (target == null || !target.isShowing()) {
            return null;
        }
        Rectangle bounds = SwingUtilities.convertRectangle(target.getParent(), target.getBounds(), this);
        bounds.grow(QuickStart.HIGHLIGHT_PADDING, QuickStart.HIGHLIGHT_PADDING);
        return bounds;
    }

    public void updateLayout() {
        if (SwingUtilities.isEventDispatchThread()) {
            layoutBubble();
            repaint();
        } else {
            SwingUtilities.invokeLater(this::updateLayout);
        }
    }

    private void layoutBubble() {
        Dimension size = bubble.getPreferredSize();
        Rectangle target = getTargetBounds();

        int maxX = Math.max(QuickStart.BUBBLE_MARGIN, getWidth() - size.width - QuickStart.BUBBLE_MARGIN);
        int maxY = Math.max(QuickStart.BUBBLE_MARGIN, getHeight() - size.height - QuickStart.BUBBLE_MARGIN);

        int x;
        int y;

        if (target == null) {
            x = Math.max(QuickStart.BUBBLE_MARGIN, (getWidth() - size.width) / 2);
            y = Math.max(QuickStart.BUBBLE_MARGIN, (getHeight() - size.height) / 2);
        } else {
            int spaceRight = getWidth() - (target.x + target.width) - QuickStart.BUBBLE_MARGIN;
            int spaceLeft = target.x - QuickStart.BUBBLE_MARGIN;
            int spaceBelow = getHeight() - (target.y + target.height) - QuickStart.BUBBLE_MARGIN;
            int spaceAbove = target.y - QuickStart.BUBBLE_MARGIN;

            if (spaceRight >= size.width) {
                x = target.x + target.width + QuickStart.BUBBLE_MARGIN;
                y = clamp(target.y, QuickStart.BUBBLE_MARGIN, maxY);
            } else if (spaceLeft >= size.width) {
                x = target.x - size.width - QuickStart.BUBBLE_MARGIN;
                y = clamp(target.y, QuickStart.BUBBLE_MARGIN, maxY);
            } else if (spaceBelow >= size.height) {
                x = clamp(target.x, QuickStart.BUBBLE_MARGIN, maxX);
                y = target.y + target.height + QuickStart.BUBBLE_MARGIN;
            } else if (spaceAbove >= size.height) {
                x = clamp(target.x, QuickStart.BUBBLE_MARGIN, maxX);
                y = target.y - size.height - QuickStart.BUBBLE_MARGIN;
            } else {
                x = Math.max(QuickStart.BUBBLE_MARGIN, (getWidth() - size.width) / 2);
                y = Math.max(QuickStart.BUBBLE_MARGIN, (getHeight() - size.height) / 2);
            }
        }

        bubble.setBounds(x, y, size.width, size.height);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Rectangle target = getTargetBounds();
        RoundRectangle2D.Float hole = null;
        Area overlayArea = new Area(new Rectangle2D.Float(0, 0, getWidth(), getHeight()));
        if (target != null) {
            hole = new RoundRectangle2D.Float(
                    target.x,
                    target.y,
                    target.width,
                    target.height,
                    QuickStart.HIGHLIGHT_RADIUS,
                    QuickStart.HIGHLIGHT_RADIUS);
            overlayArea.subtract(new Area(hole));
        }

        g2.setColor(QuickStart.OVERLAY_COLOR);
        g2.fill(overlayArea);

        if (target != null) {
            g2.setColor(QuickStart.HIGHLIGHT_COLOR);
            g2.setStroke(new BasicStroke(2f));
            g2.draw(hole);
        }

        g2.dispose();
        super.paintComponent(graphics);
    }
}