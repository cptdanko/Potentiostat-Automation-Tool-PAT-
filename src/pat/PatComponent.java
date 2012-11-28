/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pat;

import java.util.logging.Logger;
import javax.swing.JFrame;
import org.jdesktop.application.ResourceMap;

/**
 *
 * @author lordbhuman
 */
public abstract class PatComponent 
{
    Logger log;
    JFrame currentFrame;
    ResourceMap resources;
    
    
    public PatComponent(Logger logger,JFrame frame,ResourceMap resources)
    {
        this.log = logger;
        this.currentFrame = frame;
        this.resources = resources;
    }
}
