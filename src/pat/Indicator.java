/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pat;
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import org.jdesktop.application.ResourceMap;

/**
 *
 * @author lordbhuman
 */
public class Indicator extends PatComponent implements Runnable
{
    private JLabel lbl;    
    private boolean keepThreadAlive;   

    public void setKeepThreadAlive(boolean keepThreadAlive) {
        this.keepThreadAlive = keepThreadAlive;
    }
    
    public Indicator(Logger logger,JFrame frame,ResourceMap resources,JLabel label)    
    {
        super(logger,frame,resources);
        this.lbl = label;
        this.keepThreadAlive = true;
    }
    public void run()
    {
        //System.out.println("in the run method");
        int i = 0;
        try
        {
            while(keepThreadAlive)
            {
                Icon icon = this.resources.getIcon("StatusBar.busyIcons[" + i + "]");                
                this.lbl.setIcon(icon); 
                Thread.sleep(80);
                i++;
                if(i==15)
                {
                    i=0;
                }
            }
        }catch(Exception e)
        {                 
            this.lbl.setIcon(this.resources.getIcon("StatusBar.idleIcon"));
            this.log.severe("Error in doing the progress animation icon");
            this.log.severe(e.getMessage());
            e.printStackTrace();
        }
    }
}
