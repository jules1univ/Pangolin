package fr.univrennes.istic.l2gen.application.gui.main;

import javax.swing.*;

import fr.univrennes.istic.l2gen.application.core.TaskStatus;
import fr.univrennes.istic.l2gen.application.core.config.Lang;
import fr.univrennes.istic.l2gen.application.gui.dialog.task.TaskEntry;
import fr.univrennes.istic.l2gen.application.gui.dialog.task.TaskPanel;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class BottomBar extends JPanel {

    private final JLabel statusLabel;
    private final JLabel nameLabel;
    private final JLabel rowsLabel;
    private final JLabel colsLabel;
    private final JLabel sumLabel;
    private final JLabel avgLabel;
    private final JLabel medLabel;
    private final JLabel minLabel;
    private final JLabel maxLabel;

    private final JProgressBar loadingBar;

    private final JLabel taskCountLabel;
    private final List<TaskEntry> taskEntries = new ArrayList<>();
    private TaskPanel taskPanel;
    private ComponentListener windowComponentListener;

    public BottomBar() {
        setLayout(new BorderLayout(0, 0));
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Separator.foreground")));
        setPreferredSize(new Dimension(0, 26));

        statusLabel = new JLabel("");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 11f));
        add(statusLabel, BorderLayout.WEST);

        nameLabel = createInfoLabel("");
        rowsLabel = createInfoLabel("");
        colsLabel = createInfoLabel("");
        sumLabel = createInfoLabel("");
        avgLabel = createInfoLabel("");
        medLabel = createInfoLabel("");
        minLabel = createInfoLabel("");
        maxLabel = createInfoLabel("");

        nameLabel.setVisible(false);
        rowsLabel.setVisible(false);
        colsLabel.setVisible(false);
        sumLabel.setVisible(false);
        avgLabel.setVisible(false);
        medLabel.setVisible(false);
        minLabel.setVisible(false);
        maxLabel.setVisible(false);

        JPanel labelsRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        labelsRow.setOpaque(false);
        labelsRow.add(nameLabel);
        labelsRow.add(makeSeparator());
        labelsRow.add(rowsLabel);
        labelsRow.add(makeSeparator());
        labelsRow.add(colsLabel);
        labelsRow.add(makeSeparator());
        labelsRow.add(sumLabel);
        labelsRow.add(makeSeparator());
        labelsRow.add(avgLabel);
        labelsRow.add(makeSeparator());
        labelsRow.add(medLabel);
        labelsRow.add(makeSeparator());
        labelsRow.add(minLabel);
        labelsRow.add(makeSeparator());
        labelsRow.add(maxLabel);

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        centerPanel.add(labelsRow, new GridBagConstraints());
        add(centerPanel, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 4));
        rightPanel.setOpaque(false);

        taskCountLabel = new JLabel("");
        taskCountLabel.setFont(taskCountLabel.getFont().deriveFont(Font.PLAIN, 11f));
        taskCountLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        taskCountLabel.setVisible(false);
        taskCountLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        taskCountLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                toggleTaskPanel();
            }
        });

        loadingBar = new JProgressBar();
        loadingBar.setIndeterminate(false);
        loadingBar.setPreferredSize(new Dimension(100, 14));
        loadingBar.setVisible(false);
        loadingBar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loadingBar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                toggleTaskPanel();
            }
        });

        rightPanel.add(taskCountLabel);
        rightPanel.add(loadingBar);
        add(rightPanel, BorderLayout.EAST);
    }

    public void setStatus(String message) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(message));
    }

    public void setTableInfo(String name, int rows, int cols) {
        SwingUtilities.invokeLater(() -> {
            nameLabel.setText(name);
            rowsLabel.setText(Lang.get("status.rows_count", rows));
            colsLabel.setText(Lang.get("status.cols_count", cols));
            nameLabel.setVisible(true);
            rowsLabel.setVisible(true);
            colsLabel.setVisible(true);
        });
    }

    public void setColumnStats(Optional<String> min, Optional<String> max, Optional<String> avg, Optional<String> med,
            Optional<String> sum) {
        SwingUtilities.invokeLater(() -> {
            sum.ifPresentOrElse(value -> {
                sumLabel.setText(Lang.get("status.sum", value));
                sumLabel.setVisible(true);
            }, () -> sumLabel.setVisible(false));
            avg.ifPresentOrElse(value -> {
                avgLabel.setText(Lang.get("status.avg", value));
                avgLabel.setVisible(true);
            }, () -> avgLabel.setVisible(false));
            med.ifPresentOrElse(value -> {
                medLabel.setText(Lang.get("status.median", value));
                medLabel.setVisible(true);
            }, () -> medLabel.setVisible(false));
            min.ifPresentOrElse(value -> {
                minLabel.setText(Lang.get("status.min", value));
                minLabel.setVisible(true);
            }, () -> minLabel.setVisible(false));
            max.ifPresentOrElse(value -> {
                maxLabel.setText(Lang.get("status.max", value));
                maxLabel.setVisible(true);
            }, () -> maxLabel.setVisible(false));
        });
    }

    public void clearColumnStats() {
        SwingUtilities.invokeLater(() -> {
            sumLabel.setVisible(false);
            avgLabel.setVisible(false);
            minLabel.setVisible(false);
            maxLabel.setVisible(false);
        });
    }

    public void setLoading(boolean isLoading) {
        SwingUtilities.invokeLater(() -> {
            loadingBar.setIndeterminate(isLoading);
            loadingBar.setVisible(isLoading);
            if (!isLoading) {
                dismissTaskPanel();
            }
            revalidate();
            repaint();
        });
    }

    public void clearTasks() {
        SwingUtilities.invokeLater(() -> {
            taskEntries.clear();
            refreshTaskCountLabel();
            if (taskPanel != null) {
                taskPanel.refresh(taskEntries);
                repositionTaskPanel();
            }
        });
    }

    public String addTask(String name, TaskStatus status) {
        String taskId = UUID.randomUUID().toString();
        SwingUtilities.invokeLater(() -> {
            taskEntries.add(new TaskEntry(taskId, name, status));
            refreshTaskCountLabel();
            if (taskPanel != null) {
                taskPanel.refresh(taskEntries);
                repositionTaskPanel();
            }
        });
        return taskId;
    }

    public void updateTask(String taskId, String name, TaskStatus status) {
        SwingUtilities.invokeLater(() -> {
            for (int index = 0; index < taskEntries.size(); index++) {
                if (taskEntries.get(index).id().equals(taskId)) {
                    taskEntries.set(index, new TaskEntry(taskId, name, status));
                    break;
                }
            }
            refreshTaskCountLabel();
            if (taskPanel != null) {
                taskPanel.refresh(taskEntries);
                repositionTaskPanel();
            }
        });
    }

    public void updateTaskStatus(String taskId, TaskStatus status) {
        SwingUtilities.invokeLater(() -> {
            for (int index = 0; index < taskEntries.size(); index++) {
                TaskEntry entry = taskEntries.get(index);
                if (entry.id().equals(taskId)) {
                    taskEntries.set(index, new TaskEntry(taskId, entry.name(), status));
                    break;
                }
            }
            refreshTaskCountLabel();
            if (taskPanel != null) {
                taskPanel.refresh(taskEntries);
                repositionTaskPanel();
            }
        });
    }

    private void removeTask(String taskId) {
        SwingUtilities.invokeLater(() -> {
            taskEntries.removeIf(entry -> entry.id().equals(taskId));
            if (taskEntries.isEmpty()) {
                loadingBar.setVisible(false);
                dismissTaskPanel();
            }
            refreshTaskCountLabel();
            if (taskPanel != null) {
                taskPanel.refresh(taskEntries);
                repositionTaskPanel();
            }
        });
    }

    private void refreshTaskCountLabel() {
        int runningCount = (int) taskEntries.stream()
                .filter(entry -> entry.status() == TaskStatus.RUNNING)
                .count();
        if (runningCount > 0) {
            taskCountLabel.setText(Lang.get("status.tasks_running", runningCount));
            taskCountLabel.setVisible(true);
        } else {
            taskCountLabel.setVisible(false);
        }
    }

    private void toggleTaskPanel() {
        if (taskPanel != null) {
            dismissTaskPanel();
            return;
        }

        JRootPane rootPane = SwingUtilities.getRootPane(this);
        if (rootPane == null) {
            return;
        }

        JLayeredPane layeredPane = rootPane.getLayeredPane();

        taskPanel = new TaskPanel(this::removeTask);
        taskPanel.refresh(taskEntries);

        Dimension fixedSize = taskPanel.getFixedSize(taskEntries.size());
        taskPanel.setSize(fixedSize);
        taskPanel.setPreferredSize(fixedSize);

        repositionTaskPanel();

        layeredPane.add(taskPanel, JLayeredPane.POPUP_LAYER);
        layeredPane.revalidate();
        layeredPane.repaint();

        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        if (parentWindow != null) {
            windowComponentListener = new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent event) {
                    repositionTaskPanel();
                }

                @Override
                public void componentMoved(ComponentEvent event) {
                    repositionTaskPanel();
                }
            };
            parentWindow.addComponentListener(windowComponentListener);
        }

        layeredPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                if (taskPanel != null) {
                    Rectangle taskPanelBounds = taskPanel.getBounds();
                    if (!taskPanelBounds.contains(event.getPoint())) {
                        dismissTaskPanel();
                        layeredPane.removeMouseListener(this);
                    }
                } else {
                    layeredPane.removeMouseListener(this);
                }
            }
        });
    }

    private void repositionTaskPanel() {
        if (taskPanel == null) {
            return;
        }

        JRootPane rootPane = SwingUtilities.getRootPane(this);
        if (rootPane == null) {
            return;
        }

        JLayeredPane layeredPane = rootPane.getLayeredPane();
        Dimension fixedSize = taskPanel.getFixedSize(taskEntries.size());

        Point loadingBarInRoot = SwingUtilities.convertPoint(loadingBar, 0, 0, layeredPane);
        int panelX = loadingBarInRoot.x + loadingBar.getWidth() - fixedSize.width;
        int panelY = loadingBarInRoot.y - fixedSize.height;

        taskPanel.setBounds(panelX, panelY, fixedSize.width, fixedSize.height);
        layeredPane.revalidate();
        layeredPane.repaint();
    }

    private void dismissTaskPanel() {
        if (taskPanel == null) {
            return;
        }

        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        if (parentWindow != null && windowComponentListener != null) {
            parentWindow.removeComponentListener(windowComponentListener);
            windowComponentListener = null;
        }

        JRootPane rootPane = SwingUtilities.getRootPane(this);
        if (rootPane != null) {
            JLayeredPane layeredPane = rootPane.getLayeredPane();
            layeredPane.remove(taskPanel);
            layeredPane.revalidate();
            layeredPane.repaint();
        }

        taskPanel = null;
    }

    private JLabel createInfoLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 11f));
        label.setForeground(UIManager.getColor("Label.disabledForeground"));
        label.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        return label;
    }

    private JSeparator makeSeparator() {
        JSeparator separator = new JSeparator(JSeparator.VERTICAL);
        separator.setPreferredSize(new Dimension(1, 14));
        return separator;
    }
}