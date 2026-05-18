package fr.univrennes.istic.l2gen.application.gui.dialog.quickstart.askuser;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

public final class DotIndicatorPanel extends JPanel {

    private static final int DOT_SIZE = 8;
    private static final int DOT_SPACING = 6;
    private static final Color DOT_ACTIVE_COLOR = new Color(57, 130, 200);
    private static final Color DOT_INACTIVE_COLOR = new Color(190, 190, 190);

    private final int dotCount;
    private int activeDotIndex = 0;

    public DotIndicatorPanel(int dotCount) {
        this.dotCount = dotCount;
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setOpaque(false);
        rebuildDots();
    }

    private void rebuildDots() {
        removeAll();
        add(Box.createHorizontalGlue());
        for (int dotIndex = 0; dotIndex < dotCount; dotIndex++) {
            final int index = dotIndex;
            add(new DotShape(index == activeDotIndex));
            if (dotIndex < dotCount - 1) {
                add(Box.createRigidArea(new Dimension(DOT_SPACING, 0)));
            }
        }
        add(Box.createHorizontalGlue());
        revalidate();
        repaint();
    }

    public void setActive(int index) {
        activeDotIndex = index;
        rebuildDots();
    }

    private static class DotShape extends JComponent {
        private final boolean active;

        private DotShape(boolean active) {
            this.active = active;
            setPreferredSize(new Dimension(DOT_SIZE, DOT_SIZE));
            setMaximumSize(new Dimension(DOT_SIZE, DOT_SIZE));
            setMinimumSize(new Dimension(DOT_SIZE, DOT_SIZE));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2d = (Graphics2D) graphics.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(active ? DOT_ACTIVE_COLOR : DOT_INACTIVE_COLOR);
            g2d.fillOval(0, 0, DOT_SIZE, DOT_SIZE);
            g2d.dispose();
        }
    }
}