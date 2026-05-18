package fr.univrennes.istic.l2gen.application.gui.dialog.quickstart.askuser;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JPanel;

public final class CarouselPanel extends JPanel {
    private final QuickStartDialog quickStartDialog;

    public CarouselPanel(QuickStartDialog quickStartDialog) {
        this.quickStartDialog = quickStartDialog;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (this.quickStartDialog.carouselImages.isEmpty()) {
            return;
        }
        Graphics2D g2d = (Graphics2D) graphics.create();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(this.quickStartDialog.carouselImages.get(this.quickStartDialog.currentImageIndex), 0, 0,
                getWidth(), getHeight(), this);
        g2d.dispose();
    }
}