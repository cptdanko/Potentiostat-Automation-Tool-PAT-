/*
 * Author: Bhuman Soni
 * Email: bhumansoni@hotmail.com
 */
package pat;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 *
 * @author lordbhuman
 */
public final class Notify {
    
    public static void error(JFrame frame,String title,String errorMessage)
    {
        JOptionPane.showMessageDialog(frame,errorMessage,title,JOptionPane.ERROR_MESSAGE); 
    }
    public static void info(JFrame frame,String title,String info)
    {
        JOptionPane.showMessageDialog(frame,info,title,JOptionPane.INFORMATION_MESSAGE);
    }
   
}
