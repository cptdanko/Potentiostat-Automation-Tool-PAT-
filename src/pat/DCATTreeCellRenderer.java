/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pat;

import java.awt.Component;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import pat.DCATTreeNode.NODE_TYPE;

/**
 *
 * @author bsoni
 */
public class DCATTreeCellRenderer extends DefaultTreeCellRenderer {

    @Override
    public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean sel,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus) {

        if (value instanceof DCATTreeNode) {
            DCATTreeNode node = (DCATTreeNode) value;
            if (node.getType() == NODE_TYPE.EXPERIMENT) {
                Icon icon = new ImageIcon(getClass().getResource("/pat/resources/experiment.png"));
                
                //setLeafIcon(icon);
                setIcon(icon);

            } else if (node.getType() == NODE_TYPE.SAMPLE) {
                Icon icon = new ImageIcon(getClass().getResource("/pat/resources/sample.png"));
                //setLeafIcon(icon);
                setIcon(icon);
            } else if (node.getType() == NODE_TYPE.PROJECT) {
                Icon icon = new ImageIcon(getClass().getResource("/pat/resources/project.png"));
                //setLeafIcon(icon);
                setIcon(icon);
            }
            else if(node.getType() ==NODE_TYPE.OWNERSHIP)
            {
                Icon icon = new ImageIcon(getClass().getResource("/pat/resources/ownership.png"));
                //setLeafIcon(icon);
                setIcon(icon);
            }
            else if(node.getType() ==NODE_TYPE.NOICON)
            {
                return super.getTreeCellRendererComponent(
                tree, value,
                sel, expanded, leaf,
                row,
                hasFocus);
            }

        }
        String stringValue = tree.convertValueToText(value, sel,
                expanded, leaf, row, hasFocus);

        this.hasFocus = hasFocus;
        setText(stringValue);
        if (sel) {
            setForeground(getTextSelectionColor());
        } else {
            setForeground(getTextNonSelectionColor());
        }

        if (!tree.isEnabled()) {
            setEnabled(false);
        } else {
            setEnabled(true);
        }
        setComponentOrientation(tree.getComponentOrientation());
        selected = sel;
        return this;
    }
}
