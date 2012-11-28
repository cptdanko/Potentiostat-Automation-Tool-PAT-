/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pat;

import java.util.Enumeration;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

/**
 *
 * @author bsoni
 */
public class DCATTreeNode extends DefaultMutableTreeNode 
{
    public enum NODE_TYPE {PROJECT,SAMPLE,EXPERIMENT,OWNERSHIP,NOICON};
    private NODE_TYPE type;
    public void setType(NODE_TYPE t)
    {
        this.type = t;
    }
    public NODE_TYPE getType()
    {
        return this.type;
    }
   public DCATTreeNode(Object value)
   {
       super(value);
   }
}
