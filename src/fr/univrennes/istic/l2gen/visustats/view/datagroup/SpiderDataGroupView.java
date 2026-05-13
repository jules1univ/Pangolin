package fr.univrennes.istic.l2gen.visustats.view.datagroup;

import fr.univrennes.istic.l2gen.geometry.IShape;
import fr.univrennes.istic.l2gen.geometry.Point;
import fr.univrennes.istic.l2gen.svg.interfaces.field.SVGField;
import fr.univrennes.istic.l2gen.svg.interfaces.tag.SVGTag;
import fr.univrennes.istic.l2gen.visustats.data.DataGroup;
import fr.univrennes.istic.l2gen.visustats.view.dataset.IDataSetView;
import fr.univrennes.istic.l2gen.visustats.view.dataset.SpiderDataSetView;

@SVGTag("g")
public class SpiderDataGroupView extends AbstractDataGroupView {

    @SVGField("data-radius")
    private double radius;

    @SVGField("data-point-radius")
    private double pointRadius;

    @SVGField("data-grid-levels")
    private int gridLevels;

    @SVGField("data-stacked")
    private boolean stacked;

    public SpiderDataGroupView(DataGroup data, Point center, double spacing, double radius,
            boolean horizontalLegend) {
        this(data, center, spacing, radius, 4, 4, true, horizontalLegend);
    }

    public SpiderDataGroupView(DataGroup data, Point center, double spacing, double radius, double pointRadius,
            boolean horizontalLegend) {
        this(data, center, spacing, radius, pointRadius, 4, true, horizontalLegend);
    }

    public SpiderDataGroupView(DataGroup data, Point center, double spacing, double radius, double pointRadius,
            int gridLevels, boolean stacked, boolean horizontalLegend) {
        super(data, center, spacing, horizontalLegend);
        this.radius = radius;
        this.pointRadius = pointRadius;
        this.gridLevels = Math.max(1, gridLevels);
        this.stacked = stacked;
        this.update();
    }

    @Override
    protected double getTotalElementsWidth() {
        if (this.stacked) {
            return this.getElementWidth();
        }
        return super.getTotalElementsWidth();
    }

    @Override
    protected double getTotalElementsHeight() {
        return this.radius * 2 + this.pointRadius * 2;
    }

    @Override
    protected double getElementWidth() {
        return this.radius * 2 + this.pointRadius * 2;
    }

    @Override
    protected IDataSetView createElement(Point position) {
        Point elementCenter = this.stacked ? (Point) this.center.copy() : position;
        return new SpiderDataSetView(elementCenter, this.radius, this.pointRadius, this.gridLevels);
    }

    @Override
    public boolean isAxisEnabled() {
        return false;
    }

    @Override
    protected IShape getAxisElement() {
        return null;
    }
}
