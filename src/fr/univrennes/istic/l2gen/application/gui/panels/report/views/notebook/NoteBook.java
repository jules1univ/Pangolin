package fr.univrennes.istic.l2gen.application.gui.panels.report.views.notebook;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.KeyStroke;
import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;

import fr.univrennes.istic.l2gen.application.core.icon.Ico;
import fr.univrennes.istic.l2gen.application.core.lang.Lang;
import fr.univrennes.istic.l2gen.application.core.notebook.NoteBookValue;
import fr.univrennes.istic.l2gen.application.core.services.notebook.NoteBookService;
import fr.univrennes.istic.l2gen.application.gui.dialog.export.ExportDialog;
import fr.univrennes.istic.l2gen.application.gui.GUIController;

public class NoteBook extends JPanel {

    private final DefaultListModel<NoteBookValue> model = new DefaultListModel<>();
    private final JList<NoteBookValue> list = new JList<>(model);

    private final JPopupMenu menu = new JPopupMenu();
    private final JMenuItem moveUpItem = new JMenuItem(Lang.get("report.menu.move_up"));
    private final JMenuItem moveDownItem = new JMenuItem(Lang.get("report.menu.move_down"));
    private final JMenuItem editItem = new JMenuItem(Lang.get("report.menu.edit"));
    private final JMenuItem deleteItem = new JMenuItem(Lang.get("report.menu.delete"));

    private final JToolBar toolBar = new JToolBar();
    private final JButton undoButton = new JButton(Lang.get("report.toolbar.undo"), Ico.get("icons/undo.svg"));
    private final JButton redoButton = new JButton(Lang.get("report.toolbar.redo"), Ico.get("icons/redo.svg"));
    private final JButton exportButton = new JButton(Lang.get("report.toolbar.export"), Ico.get("icons/export.svg"));

    public NoteBook() {
        build();
    }

    private void build() {
        setLayout(new BorderLayout());

        NoteBookCellRenderer cellRenderer = new NoteBookCellRenderer(this);
        list.setCellRenderer(cellRenderer);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight(-1);
        list.setVisibleRowCount(-1);
        list.setLayoutOrientation(JList.VERTICAL);
        list.setDragEnabled(true);
        list.setDropMode(DropMode.INSERT);
        list.setTransferHandler(new NoteBookDragDrop(list, model));

        buildContextMenu();
        buildToolBar();
        registerShortcuts();

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                showContextMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                showContextMenu(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = list.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        NoteBookValue value = model.get(index);
                        SwingUtilities.invokeLater(
                                () -> {
                                    GUIController.getInstance().getMainView().getReportPanel().getSettingView()
                                            .editNoteBookValue(value, index);
                                });
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);

        add(toolBar, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        refresh();
    }

    private void buildContextMenu() {
        moveUpItem.addActionListener(event -> {
            int index = list.getSelectedIndex();
            if (index > 0) {
                NoteBookService.move(index, index - 1);
                refresh();
                list.setSelectedIndex(index - 1);
            }
        });
        menu.add(moveUpItem);
        moveDownItem.addActionListener(event -> {
            int index = list.getSelectedIndex();
            if (index >= 0 && index < model.size() - 1) {
                NoteBookService.move(index, index + 1);
                refresh();
                list.setSelectedIndex(index + 1);
            }
        });
        menu.add(moveDownItem);

        menu.addSeparator();

        editItem.addActionListener(event -> {
            int index = list.getSelectedIndex();
            if (index >= 0) {
                NoteBookValue value = model.get(index);
                GUIController.getInstance().getMainView().getReportPanel().getSettingView()
                        .editNoteBookValue(value, index);
            }
        });

        menu.add(editItem);
        menu.addSeparator();

        deleteItem.addActionListener(event -> deleteSelectedValue());
        menu.add(deleteItem);
    }

    private void buildToolBar() {
        toolBar.setFloatable(false);

        undoButton.addActionListener(event -> {
            NoteBookService.undo();
            GUIController.getInstance().getMainView().getReportPanel().refresh();
        });

        redoButton.addActionListener(event -> {
            NoteBookService.redo();
            GUIController.getInstance().getMainView().getReportPanel().refresh();
        });

        exportButton.addActionListener(event -> ExportDialog.showDialog(this));

        toolBar.add(undoButton);
        toolBar.add(redoButton);
        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(exportButton);
    }

    private void registerShortcuts() {
        int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        list.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, menuMask), "notebook.undo");
        list.getActionMap().put("notebook.undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!NoteBookService.canUndo()) {
                    return;
                }
                NoteBookService.undo();
                GUIController.getInstance().getMainView().getReportPanel().refresh();
            }
        });

        list.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, menuMask), "notebook.redo");
        list.getActionMap().put("notebook.redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!NoteBookService.canRedo()) {
                    return;
                }
                NoteBookService.redo();
                GUIController.getInstance().getMainView().getReportPanel().refresh();
            }
        });
    }

    private void showContextMenu(MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }

        int index = list.locationToIndex(e.getPoint());
        if (index < 0) {
            return;
        }

        list.setSelectedIndex(index);
        menu.show(list, e.getX(), e.getY());
    }

    private void deleteSelectedValue() {
        int index = list.getSelectedIndex();
        if (index < 0) {
            return;
        }

        model.remove(index);
        NoteBookService.remove(index);
    }

    public void refresh() {
        model.clear();
        for (NoteBookValue value : NoteBookService.getValues()) {
            model.addElement(value);
        }
        updateUndoRedoButtons();
    }

    private void updateUndoRedoButtons() {
        undoButton.setEnabled(NoteBookService.canUndo());
        redoButton.setEnabled(NoteBookService.canRedo());
    }

}