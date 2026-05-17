package fr.univrennes.istic.l2gen.application.gui.dialog.merge;

import fr.univrennes.istic.l2gen.application.core.config.Lang;
import fr.univrennes.istic.l2gen.application.core.services.table.MergeJoinCondition;
import fr.univrennes.istic.l2gen.application.core.table.DataTable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public final class MergeJoinRow {

    private final MergeDialog owner;
    private JPanel panel;
    private JComboBox<String> leftColumnComboBox;
    private JComboBox<String> rightColumnComboBox;

    public MergeJoinRow(MergeDialog owner, DataTable leftTable, DataTable rightTable) {
        this.owner = owner;
        buildPanel(leftTable, rightTable);
    }

    public MergeJoinRow(MergeDialog owner, DataTable leftTable, DataTable rightTable,
            String preselectedLeftColumn, String preselectedRightColumn) {
        this.owner = owner;
        buildPanel(leftTable, rightTable);
        leftColumnComboBox.setSelectedItem(preselectedLeftColumn);
        rightColumnComboBox.setSelectedItem(preselectedRightColumn);
    }

    private void buildPanel(DataTable leftTable, DataTable rightTable) {
        panel = new JPanel(new BorderLayout(6, 0));
        panel.setBorder(new EmptyBorder(3, 6, 3, 6));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        List<String> leftColumns = leftTable.getColumnNames();
        List<String> rightColumns = rightTable.getColumnNames();

        leftColumnComboBox = new JComboBox<>(leftColumns.toArray(new String[0]));

        JLabel equalsLabel = new JLabel("=", SwingConstants.CENTER);
        equalsLabel.setPreferredSize(new Dimension(20, 26));

        rightColumnComboBox = new JComboBox<>(rightColumns.toArray(new String[0]));

        JButton removeButton = new JButton(UIManager.getIcon("InternalFrame.closeIcon"));
        removeButton.setPreferredSize(new Dimension(26, 26));
        removeButton.setToolTipText(Lang.get("merge.remove_condition"));
        removeButton.addActionListener(event -> owner.removeJoinRow(this));

        JPanel centerSection = new JPanel(new GridLayout(1, 3, 6, 0));
        centerSection.setOpaque(false);
        centerSection.add(leftColumnComboBox);
        centerSection.add(equalsLabel);
        centerSection.add(rightColumnComboBox);

        panel.add(centerSection, BorderLayout.CENTER);
        panel.add(removeButton, BorderLayout.EAST);
    }

    public void updateLeftTable(DataTable newLeftTable) {
        String previousSelection = (String) leftColumnComboBox.getSelectedItem();
        leftColumnComboBox.setModel(new DefaultComboBoxModel<>(newLeftTable.getColumnNames().toArray(new String[0])));
        if (previousSelection != null) {
            leftColumnComboBox.setSelectedItem(previousSelection);
        }
    }

    public void updateRightTable(DataTable newRightTable) {
        String previousSelection = (String) rightColumnComboBox.getSelectedItem();
        rightColumnComboBox.setModel(new DefaultComboBoxModel<>(newRightTable.getColumnNames().toArray(new String[0])));
        if (previousSelection != null) {
            rightColumnComboBox.setSelectedItem(previousSelection);
        }
    }

    public MergeJoinCondition buildCondition(DataTable leftTable, DataTable rightTable) {
        int leftIndex = leftColumnComboBox.getSelectedIndex();
        int rightIndex = rightColumnComboBox.getSelectedIndex();

        if (leftIndex < 0 || rightIndex < 0) {
            return null;
        }

        String leftColumnName = leftTable.getColumnNames().get(leftIndex);
        String leftColumnSqlName = leftTable.getSQLColumnName(leftIndex);
        String rightColumnName = rightTable.getColumnNames().get(rightIndex);
        String rightColumnSqlName = rightTable.getSQLColumnName(rightIndex);

        return new MergeJoinCondition(leftColumnName, leftColumnSqlName, rightColumnName, rightColumnSqlName);
    }

    public JPanel getPanel() {
        return panel;
    }
}