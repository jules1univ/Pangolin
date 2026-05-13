package fr.univrennes.istic.l2gen.visustats.view.datagroup;

import fr.univrennes.istic.l2gen.geometry.IShape;
import fr.univrennes.istic.l2gen.geometry.Point;
import fr.univrennes.istic.l2gen.svg.interfaces.field.SVGField;
import fr.univrennes.istic.l2gen.svg.interfaces.tag.SVGTag;
import fr.univrennes.istic.l2gen.visustats.data.DataGroup;
import fr.univrennes.istic.l2gen.visustats.view.datagroup.axis.DataAxisView;
import fr.univrennes.istic.l2gen.visustats.view.dataset.AreaDataSetView;
import fr.univrennes.istic.l2gen.visustats.view.dataset.IDataSetView;

@SVGTag("g")
public class AreaDataGroupView extends AbstractDataGroupView {

    @SVGField("data-maxheight")
    private double maxHeight;

    @SVGField("data-point-spacing")
    private double pointSpacing;

    @SVGField("data-point-radius")
    private double pointRadius;

    @SVGField("data-stacked")
    private boolean stacked;

    public AreaDataGroupView(DataGroup data, Point center, double spacing, double pointSpacing, double maxHeight,
            double pointRadius, boolean stacked, boolean horizontalLegend) {
        super(data, center, spacing, horizontalLegend);
        this.pointSpacing = pointSpacing;
        this.maxHeight = maxHeight;
        this.pointRadius = pointRadius;
        this.stacked = stacked;
        this.update();
    }

    @Override
    protected double getTotalElementsHeight() {
        return this.maxHeight;
    }

    @Override
    protected double getTotalElementsWidth() {
        if (this.stacked) {
            return this.getElementWidth();
        }
        return super.getTotalElementsWidth();
    }

    @Override
    protected double getElementWidth() {
        int maxSize = this.data.maxSize();
        if (maxSize == 0) {
            return 0;
        }
        double plotWidth = Math.max(0, maxSize - 1) * this.pointSpacing;
        return plotWidth + this.pointRadius * 2;
    }

    @Override
    protected IDataSetView createElement(Point position) {
        Point elementCenter = this.stacked ? (Point) this.center.copy() : position;

        AreaDataSetView view = new AreaDataSetView(elementCenter, this.pointSpacing, this.maxHeight,
                this.pointRadius);
        view.setScaleMax(this.data.max());

        if (this.stacked) {
            view.setFillAlpha(120);
        }

        return view;
    }

    @Override
    public boolean isAxisEnabled() {
        return true;
    }

    @Override
    protected IShape getAxisElement() {
        return new DataAxisView(this.center, this.getTotalElementsWidth(), this.getTotalElementsHeight(),
                this.spacing, this.data.max(), this.axisStepCount, this.axisScaleType, this.showXAxis,
                this.xAxisLabel, this.showYAxis, this.yAxisLabel);
    }
}
