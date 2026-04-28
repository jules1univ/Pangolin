package fr.univrennes.istic.l2gen.application.gui.dialog.export;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

@FunctionalInterface
interface ExportDocumentListener extends DocumentListener {
    public void update(DocumentEvent event);

    @Override
    default void insertUpdate(DocumentEvent event) {
        update(event);
    }

    @Override
    default void removeUpdate(DocumentEvent event) {
        update(event);
    }

    @Override
    default void changedUpdate(DocumentEvent event) {
        update(event);
    }
}