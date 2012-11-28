/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pat;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.JFrame;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.jdesktop.application.ResourceMap;
/**
 *
 * @author bsoni
 */
public class PATFileWatcher extends PatComponent
{
    private String dirPath;
    private File watchDir;
    private FileAlterationObserver observer; 
    private CollectionFileListener listener;
    private PATView view;
    
    
    public PATFileWatcher(Logger logger,JFrame frame,ResourceMap resources,
            PATView vw,String dirToWatch)throws IOException 
    {
        super(logger,frame,resources);
        this.view = vw;
        this.dirPath = dirToWatch;
        
    }
    public void setup(String endFilename)
    {
        watchDir = new File(this.dirPath);
        observer = new FileAlterationObserver(watchDir);
        listener = new CollectionFileListener(false,this,endFilename);
        observer.addListener(listener);
        observer.addListener(new FileAlterationListenerAdaptor());
        try{
            observer.initialize();
        }catch(Exception e)
        {
            this.log.severe("Error initializing the observer");
            this.log.severe(e.getMessage());
        }
    }
    public FileAlterationObserver getObserver()
    {
        return this.observer;
    }
    public CollectionFileListener getListener()
    {
        return this.listener;
    }
    public void watchFinished( List<File> files )
    {
        this.view.experimentFinished(files);
    }
}
