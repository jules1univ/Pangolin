package fr.univrennes.istic.l2gen.application.gui.dialog.quickstart;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;

import fr.univrennes.istic.l2gen.application.core.config.Config;
import fr.univrennes.istic.l2gen.application.core.config.Lang;
import fr.univrennes.istic.l2gen.application.gui.main.MainView;
import fr.univrennes.istic.l2gen.application.gui.panels.report.ReportPanel;
import fr.univrennes.istic.l2gen.application.gui.panels.report.views.settings.SettingView;
import fr.univrennes.istic.l2gen.application.gui.panels.table.view.data.TableDataView;
import fr.univrennes.istic.l2gen.application.gui.panels.table.view.data.TableToolBar;

public final class QuickStart {

    private static final AtomicBoolean ACTIVE = new AtomicBoolean(false);
    static final int HIGHLIGHT_PADDING = 8;
    static final int HIGHLIGHT_RADIUS = 12;
    static final int BUBBLE_MAX_WIDTH = 280;
    static final int BUBBLE_MARGIN = 12;

    static final Color HIGHLIGHT_COLOR = new Color(57, 183, 99);
    static final Color OVERLAY_COLOR = new Color(0, 0, 0, 170);

    private final MainView mainView;
    private final JLayeredPane layeredPane;
    private final OverlayPane overlay;
    private final List<Step> steps;

    private int stepIndex = 0;
    private ComponentListener resizeListener;
    private AbstractButton autoAdvanceButton;
    private ActionListener autoAdvanceListener;

    public static void maybeStart(MainView mainView) {
        if (mainView == null) {
            return;
        }
        if (!Config.getBoolean("settings.general.quickstart", true)) {
            return;
        }
        if (!ACTIVE.compareAndSet(false, true)) {
            return;
        }

        SwingUtilities.invokeLater(() -> new QuickStart(mainView).start());
    }

    private QuickStart(MainView mainView) {
        this.mainView = mainView;
        this.layeredPane = mainView.getRootPane().getLayeredPane();
        this.overlay = new OverlayPane(this);
        this.steps = buildSteps();
    }

    private void start() {
        if (layeredPane == null) {
            ACTIVE.set(false);
            return;
        }

        overlay.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
        layeredPane.add(overlay, JLayeredPane.POPUP_LAYER);
        layeredPane.revalidate();
        layeredPane.repaint();

        attachResizeListener();
        goToStep(0);
        overlay.requestFocusInWindow();
    }

    private void attachResizeListener() {
        resizeListener = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                updateOverlayBounds();
            }

            @Override
            public void componentMoved(ComponentEvent event) {
                updateOverlayBounds();
            }
        };
        layeredPane.addComponentListener(resizeListener);
    }

    private void updateOverlayBounds() {
        overlay.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
        overlay.updateLayout();
    }

    private List<Step> buildSteps() {
        SettingView settingView = mainView.getReportPanel().getSettingView();
        ReportPanel reportPanel = mainView.getReportPanel();
        TableDataView tableView = mainView.getTablePanel().getTableView();
        List<Step> stepList = new ArrayList<>();

        stepList.add(new Step(
                Lang.get("quickstart.step1.title"),
                Lang.get("quickstart.step1.body", Lang.get("report.add.chart")),
                () -> {
                    reportPanel.setVisible(true);
                    settingView.setVisible(true);
                    settingView.showBase();
                },
                settingView::getAddChartButton));

        stepList.add(new Step(
                Lang.get("quickstart.step2.title"),
                Lang.get("quickstart.step2.body", Lang.get("report.settings.data")),
                () -> {
                    reportPanel.setVisible(true);
                    settingView.setVisible(true);
                    settingView.showChartSettings();
                },
                settingView::getChartSettingsPanel));

        stepList.add(new Step(
                Lang.get("quickstart.step3.title"),
                Lang.get("quickstart.step3.body", Lang.get("report.next.add")),
                () -> {
                    reportPanel.setVisible(true);
                    settingView.setVisible(true);
                    settingView.showChartSettings();
                },
                () -> {
                    settingView.getChartNextButton().doClick();
                },
                settingView::getChartNextButton));

        stepList.add(new Step(
                Lang.get("quickstart.step4.title"),
                Lang.get("quickstart.step4.body", Lang.get("report.toolbar.export")),
                () -> {
                    reportPanel.setVisible(true);
                    settingView.setVisible(true);
                    reportPanel.getNoteBook().setVisible(true);
                    reportPanel.refresh();
                },
                reportPanel.getNoteBook()::getExportButton));

        stepList.add(new Step(
                Lang.get("quickstart.step5.title"),
                Lang.get("quickstart.step5.body", Lang.get("table.toolbar.filters")),
                () -> {
                    mainView.getTablePanel().setVisible(true);
                    mainView.resetSplit();
                },
                tableView.getToolBar()::getFilterButton));

        stepList.add(new Step(
                Lang.get("quickstart.step6.title"),
                Lang.get("quickstart.step6.body", Lang.get("table.toolbar.subtable")),
                () -> {
                    mainView.getTablePanel().setVisible(true);
                    mainView.resetSplit();
                },
                tableView.getToolBar()::getSubtableButton));

        stepList.add(new Step(
                Lang.get("quickstart.step7.title"),
                Lang.get("quickstart.step7.body", Lang.get("table.toolbar.mergetable")),
                () -> {
                    mainView.getTablePanel().setVisible(true);
                    mainView.resetSplit();
                },
                tableView.getToolBar()::getMergeButton));

        stepList.add(new Step(
                Lang.get("quickstart.step8.title"),
                Lang.get("quickstart.step8.body"),
                () -> {
                    mainView.getTablePanel().setVisible(true);
                    mainView.resetSplit();
                },
                () -> {
                    detachAutoAdvance();
                    TableToolBar toolBar = tableView.getToolBar();
                    JButton btn = toolBar.getHideEmptyColumnsButton().isVisible() ? toolBar.getHideEmptyColumnsButton()
                            : toolBar.getShowAllColumnsButton();
                    btn.doClick();
                },
                () -> {
                    TableToolBar toolBar = tableView.getToolBar();
                    if (toolBar.getHideEmptyColumnsButton().isVisible()) {
                        return toolBar.getHideEmptyColumnsButton();
                    }
                    return toolBar.getShowAllColumnsButton();
                }));

        stepList.add(new Step(Lang.get("quickstart.step9.title"),
                Lang.get("quickstart.step9.body"),
                () -> {
                    mainView.getTablePanel().setVisible(true);
                    mainView.resetSplit();
                },
                () -> {
                    int columnIndex = 1;
                    SwingUtilities.invokeLater(() -> {
                        tableView.getTableView().getTableHeader().dispatchEvent(new MouseEvent(
                                tableView.getTableView().getTableHeader(),
                                MouseEvent.MOUSE_PRESSED,
                                System.currentTimeMillis(),
                                MouseEvent.BUTTON3_DOWN_MASK,
                                tableView.getTableView().getTableHeader().getHeaderRect(columnIndex).x + 5,
                                tableView.getTableView().getTableHeader().getHeaderRect(columnIndex).y + 5,
                                1,
                                true,
                                MouseEvent.BUTTON3));
                        tableView.getTableView().getTableHeader().dispatchEvent(new MouseEvent(
                                tableView.getTableView().getTableHeader(),
                                MouseEvent.MOUSE_RELEASED,
                                System.currentTimeMillis(),
                                MouseEvent.BUTTON3_DOWN_MASK,
                                tableView.getTableView().getTableHeader().getHeaderRect(columnIndex).x + 5,
                                tableView.getTableView().getTableHeader().getHeaderRect(columnIndex).y + 5,
                                1,
                                true,
                                MouseEvent.BUTTON3));
                        tableView.getTableView().getTableHeader().dispatchEvent(new MouseEvent(
                                tableView.getTableView().getTableHeader(),
                                MouseEvent.MOUSE_CLICKED,
                                System.currentTimeMillis(),
                                MouseEvent.BUTTON3_DOWN_MASK,
                                tableView.getTableView().getTableHeader().getHeaderRect(columnIndex).x + 5,
                                tableView.getTableView().getTableHeader().getHeaderRect(columnIndex).y + 5,
                                1,
                                true,
                                MouseEvent.BUTTON3));
                    });
                },
                () -> {
                    return tableView.getTableView().getTableHeader();
                }));

        stepList.add(new Step(
                Lang.get("quickstart.step10.title"),
                Lang.get("quickstart.step10.body"),
                () -> {
                    mainView.getTablePanel().setVisible(true);
                    mainView.resetSplit();

                    SwingUtilities.invokeLater(() -> {
                        int columnIndex = 1;
                        if (tableView.getColumnContextMenu() != null) {
                            tableView.getColumnContextMenu().show(
                                    tableView.getTableView(),
                                    tableView.getTableView().getTableHeader().getHeaderRect(columnIndex).x + 5,
                                    tableView.getTableView().getTableHeader().getHeaderRect(columnIndex).y + 5);
                        }
                    });
                },
                () -> {
                    if (tableView.getColumnContextMenu() != null) {
                        tableView.getColumnContextMenu().getSortMenu().doClick();
                        Component sortItem = tableView.getColumnContextMenu().getSortMenu().getMenuComponent(0);
                        sortItem.dispatchEvent(new MouseEvent(
                                sortItem,
                                MouseEvent.MOUSE_CLICKED,
                                System.currentTimeMillis(),
                                MouseEvent.BUTTON1_DOWN_MASK,
                                sortItem.getWidth() / 2,
                                sortItem.getHeight() / 2,
                                1,
                                false,
                                MouseEvent.BUTTON1));
                    }
                },
                tableView::getColumnContextMenu));

        return stepList;
    }

    private void goToStep(int index) {
        if (index < 0 || index >= steps.size()) {
            return;
        }
        detachAutoAdvance();
        stepIndex = index;
        Step step = steps.get(stepIndex);
        step.onEnter().run();
        attachAutoAdvance(step, stepIndex);

        SwingUtilities.invokeLater(() -> {
            overlay.showStep(step, stepIndex + 1, steps.size());
            overlay.updateLayout();
        });
    }

    public void nextStep() {
        if (stepIndex >= 0 && stepIndex < steps.size()) {
            Step current = steps.get(stepIndex);
            if (current.onClose() != null) {
                current.onClose().run();
            }
        }
        int nextIndex = stepIndex + 1;
        if (nextIndex < steps.size()) {
            goToStep(nextIndex);
        } else {
            finish();
        }
    }

    public void finish() {
        Config.put("settings.general.quickstart", false);
        dismiss();

        if (stepIndex >= 0 && stepIndex < steps.size()) {
            Step step = steps.get(stepIndex);
            if (step.onClose() != null) {
                step.onClose().run();
            }
        }
    }

    public void skip() {
        Config.put("settings.general.quickstart", false);
        dismiss();
    }

    private void dismiss() {
        detachAutoAdvance();
        if (resizeListener != null) {
            layeredPane.removeComponentListener(resizeListener);
        }
        layeredPane.remove(overlay);
        layeredPane.revalidate();
        layeredPane.repaint();
        ACTIVE.set(false);
    }

    private void attachAutoAdvance(Step step, int index) {
        Component target = step.targetSupplier().get();
        if (!(target instanceof AbstractButton button)) {
            return;
        }

        autoAdvanceButton = button;
        autoAdvanceListener = event -> {
            if (stepIndex != index) {
                return;
            }
            detachAutoAdvance();
            if (index >= steps.size() - 1) {
                finish();
            } else {
                nextStep();
            }
        };
        button.addActionListener(autoAdvanceListener);
    }

    private void detachAutoAdvance() {
        if (autoAdvanceButton != null && autoAdvanceListener != null) {
            autoAdvanceButton.removeActionListener(autoAdvanceListener);
        }
        autoAdvanceButton = null;
        autoAdvanceListener = null;
    }
}
