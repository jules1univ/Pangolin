package fr.univrennes.istic.l2gen.application.gui.dialog.task;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import fr.univrennes.istic.l2gen.application.core.TaskStatus;
import fr.univrennes.istic.l2gen.application.core.config.Lang;

public final class TaskPanel extends JPanel {

    private static final int DONE_REMOVAL_DELAY_MS = 3000;
    private static final int FIXED_WIDTH = 320;
    private static final int ROW_HEIGHT = 46;
    private static final int MAX_VISIBLE_ROWS = 6;
    private static final int HEADER_HEIGHT = 32;
    private static final int SEPARATOR_HEIGHT = 1;

    private final JPanel taskListPanel;
    private final JLabel taskCountLabel;
    private final Consumer<String> onTaskRemove;
    private final Set<String> scheduledForRemoval = new HashSet<>();

    public TaskPanel(Consumer<String> onTaskRemove) {
        this.onTaskRemove = onTaskRemove;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIManager.getColor("Separator.foreground"), 1),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));
        setBackground(UIManager.getColor("Panel.background"));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(new EmptyBorder(6, 10, 6, 10));
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(Lang.get("task.title"));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 11f));

        taskCountLabel = new JLabel("");
        taskCountLabel.setFont(taskCountLabel.getFont().deriveFont(Font.PLAIN, 11f));
        taskCountLabel.setForeground(UIManager.getColor("Label.disabledForeground"));

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(taskCountLabel, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);
        add(new JSeparator(JSeparator.HORIZONTAL), BorderLayout.CENTER);

        taskListPanel = new JPanel();
        taskListPanel.setLayout(new BoxLayout(taskListPanel, BoxLayout.Y_AXIS));
        taskListPanel.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(taskListPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        add(scrollPane, BorderLayout.SOUTH);
    }

    public void refresh(List<TaskEntry> entries) {
        scheduleRemovalForDoneEntries(entries);

        taskCountLabel.setText(entries.size() + " task" + (entries.size() != 1 ? "s" : ""));
        taskListPanel.removeAll();

        for (int index = 0; index < entries.size(); index++) {
            taskListPanel.add(buildTaskRow(entries.get(index)));
            if (index < entries.size() - 1) {
                taskListPanel.add(new JSeparator(JSeparator.HORIZONTAL));
            }
        }

        int visibleRows = Math.min(Math.max(entries.size(), 1), MAX_VISIBLE_ROWS);
        int listHeight = visibleRows * ROW_HEIGHT + (visibleRows - 1) * SEPARATOR_HEIGHT;
        taskListPanel.setPreferredSize(new Dimension(FIXED_WIDTH, listHeight));

        revalidate();
        repaint();
    }

    public Dimension getFixedSize(int entryCount) {
        int visibleRows = Math.min(Math.max(entryCount, 1), MAX_VISIBLE_ROWS);
        int listHeight = visibleRows * ROW_HEIGHT + (visibleRows - 1) * SEPARATOR_HEIGHT;
        return new Dimension(FIXED_WIDTH, HEADER_HEIGHT + SEPARATOR_HEIGHT + listHeight);
    }

    private void scheduleRemovalForDoneEntries(List<TaskEntry> entries) {
        for (TaskEntry entry : entries) {
            if ((entry.status() == TaskStatus.SUCCESS || entry.status() == TaskStatus.FAILED)
                    && !scheduledForRemoval.contains(entry.id())) {
                scheduledForRemoval.add(entry.id());
                String taskId = entry.id();
                Timer removalTimer = new Timer(DONE_REMOVAL_DELAY_MS, event -> onTaskRemove.accept(taskId));
                removalTimer.setRepeats(false);
                removalTimer.start();
            }
        }
    }

    private JPanel buildTaskRow(TaskEntry entry) {
        JPanel rowPanel = new JPanel(new BorderLayout(0, 4));
        rowPanel.setBorder(new EmptyBorder(6, 10, 6, 10));
        rowPanel.setOpaque(false);
        rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));

        int availableTextWidth = FIXED_WIDTH - 20 - 60;
        ScrollingLabel taskNameLabel = new ScrollingLabel(entry.name(), availableTextWidth);
        taskNameLabel.setFont(taskNameLabel.getFont().deriveFont(Font.PLAIN, 12f));

        JLabel statusBadge = createStatusBadge(entry.status());

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        topRow.add(taskNameLabel, BorderLayout.WEST);
        topRow.add(statusBadge, BorderLayout.EAST);

        JProgressBar miniBar = new JProgressBar();
        miniBar.setPreferredSize(new Dimension(0, 3));
        miniBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 3));
        miniBar.setBorderPainted(false);

        switch (entry.status()) {
            case RUNNING -> miniBar.setIndeterminate(true);
            case PENDING -> {
                miniBar.setIndeterminate(false);
                miniBar.setValue(0);
            }
            case SUCCESS, FAILED -> {
                miniBar.setIndeterminate(false);
                miniBar.setValue(100);
            }
        }

        rowPanel.add(topRow, BorderLayout.NORTH);
        rowPanel.add(miniBar, BorderLayout.SOUTH);

        return rowPanel;
    }

    private JLabel createStatusBadge(TaskStatus status) {
        String badgeText = switch (status) {
            case RUNNING -> "running";
            case PENDING -> "pending";
            case SUCCESS -> "done";
            case FAILED -> "failed";
        };

        Color badgeBackground = switch (status) {
            case RUNNING -> new Color(219, 234, 254);
            case PENDING -> UIManager.getColor("Panel.background");
            case SUCCESS -> new Color(209, 250, 229);
            case FAILED -> new Color(254, 226, 226);
        };

        Color badgeForeground = switch (status) {
            case RUNNING -> new Color(30, 64, 175);
            case PENDING -> UIManager.getColor("Label.disabledForeground");
            case SUCCESS -> new Color(6, 95, 70);
            case FAILED -> new Color(153, 27, 27);
        };

        JLabel badge = new JLabel(badgeText) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D graphics2d = (Graphics2D) graphics.create();
                graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics2d.setColor(getBackground());
                graphics2d.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                graphics2d.dispose();
                super.paintComponent(graphics);
            }
        };

        badge.setFont(badge.getFont().deriveFont(Font.PLAIN, 10f));
        badge.setBackground(badgeBackground);
        badge.setForeground(badgeForeground);
        badge.setOpaque(false);
        badge.setBorder(new EmptyBorder(1, 6, 1, 6));

        return badge;
    }
}