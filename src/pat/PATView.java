/*
 * Author: Bhuman Soni
 * Email: bhumansoni@hotmail.com
 * PATView.java
 */

package pat;

import java.awt.Component;
import java.awt.event.ActionEvent;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import java.io.File;
import java.io.IOException;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import java.awt.Desktop;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JMenuItem;
import javax.swing.JTextField;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.codehaus.jettison.json.JSONException;
import pat.PatCLIENT.CookieKeeper;
/**
 * The application's main frame.
 */
public class PATView extends FrameView {
    
    //remove the method below in the production version
    public static void print(Object a)
    {
        System.out.println(a);
    }
    public PATView(SingleFrameApplication app) 
    {
        super(app);
        
        initComponents();
        // status bar initialization - message timeout, idle icon and busy animation, etc
        try
        {
            FileHandler handler = new FileHandler("PatLOG.txt");
            properties = getResourceMap();
            PATView.log = Logger.getLogger(PATView.class.getName());
            PATView.log.addHandler(handler);
            patClient = new PatCLIENT(PATView.log,this.getFrame(),properties);
        }
        catch(IOException io)
        {
            Notify.error(this.getFrame(),"Program Initialize Error", io.getMessage());
        }
                
        idleIcon = properties.getIcon("StatusBar.idleIcon");
        patTabs.setEnabledAt(this.patTabs.getTabCount()-1, false);
        patTabs.setSelectedIndex(0);
        
        fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        generatedFiles = new ArrayList<String>();        
        this.runProgBtn.setEnabled(false);
        this.cancelRuningProgBtn.setEnabled(false);
        this.finishAndUploadBtn.setEnabled(false);
        
        this.metadataLbl1.setVisible(false);
        this.metadata1.setVisible(false);
        this.metadataLbl2.setVisible(false);
        this.metadata2.setVisible(false);
        this.metadataLbl3.setVisible(false);
        this.metadata3.setVisible(false);
        this.metadataLbl4.setVisible(false);
        this.metadata4.setVisible(false);
        this.metadataLbl5.setVisible(false);
        this.metadata5.setVisible(false);
        changeMetadata(getExistingConfigFields());
    }    
    
    public TreeModel populateTree()
    {
        this.treeRoot.setType(DCATTreeNode.NODE_TYPE.NOICON);
        TreeModel model = new DefaultTreeModel(this.treeRoot);        
        return model;
    }    
    @Action
    public void browseFiles()throws IOException
    {
        int returnValue = fileChooser.showOpenDialog(this.getFrame());
        if(returnValue == JFileChooser.APPROVE_OPTION)
        {
            File selectedFile = fileChooser.getSelectedFile();
            pathName = selectedFile.getAbsolutePath();
            //so over here we need to make sure that the path is not too long
            String dirname = selectedFile.getAbsolutePath();
            if(dirname.length()> this.maxDirNameLength)
            {
                dirname = "..."+dirname.substring((dirname.length()-this.maxDirNameLength),dirname.length());                
            }
            this.outputLocation.setText(dirname);
        }
    }
    @Action    
    public void userLogin()throws IOException
    {
        /*if we have an existing cookiekeeper object it means that we are 
         * logging in for the second time. So its best we stop it running first
        */
        if(cookieKeeper != null)
        {
            this.cookieThread.interrupt();
            this.cookieKeeper.stopThread();
        }
     
        
        final String userName = this.username.getText();                
        final String pass = new String(this.password.getPassword());         
        print(this.emailConfirmChk.getText());
        if(patClient.initializeUser(userName, pass))
        {
            this.patTabs.setEnabledAt(this.patTabs.getTabCount()-1, true);
            Component loginTab = this.patTabs.getComponentAt(this.patTabs.getTabCount()-1);            
            this.patTabs.setSelectedComponent(loginTab);
            
            patClient.initializeSamples();
            populateTree();
            this.refreshSamples();
            cookieKeeper = patClient.new CookieKeeper(patClient);
            cookieThread = new Thread(cookieKeeper,"CookiePoller");    
            this.cookieThread.setDaemon(true);
            this.cookieThread.start();
            runIndicator = new Indicator(PATView.log,this.getFrame(),properties,this.progressLbl);
            
            try
            {
                this.addInstrumentsToMenu(patClient.getInstrumentNamesList()); 
                        
            
            }catch(JSONException e)
            {
                Notify.error(this.getFrame(), 
                        this.properties.getString("patView.InstrumentsError", String.class),
                        this.properties.getString("patview.InstrumentsError.message", String.class));
                PATView.log.severe(e.getMessage());
            }
            
        }    
        else
        {
            Notify.error(this.getFrame(),
                        properties.getString("patView.loginUnSuccessful.header",String.class), 
                        properties.getString("patView.loginUnSuccessful.message",String.class));
        }
    }
    @Action
    public void showAboutBox() 
    {
        if (aboutBox == null) {
            JFrame mainFrame = PATApp.getApplication().getMainFrame();
            aboutBox = new PATAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        PATApp.getApplication().show(aboutBox);
    }
    @Action
    public void showMDDialogBox() 
    {
        
        if (showMdDialog == null) {
            JFrame mainFrame = PATApp.getApplication().getMainFrame();
            showMdDialog = new Metadata(mainFrame,true);
            showMdDialog.setPatView(this);
            showMdDialog.setLocationRelativeTo(mainFrame);
        }

        PATApp.getApplication().show(showMdDialog);
    }
    public final List<JTextField> getExistingConfigFields()
    {
        String filename="metadata.config";
        List<JTextField> existingMD = new ArrayList<JTextField>();
        File file = new File(filename);
        try
        {
            if(file.exists())
            {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line; 
                int mdCounter = 0;
                while((line =reader.readLine()) !=null)
                {
                    if(mdCounter>4){break;}
                    existingMD.add(new JTextField(line));
                    mdCounter++;
                }
                reader.close();
            }
        }catch(IOException io)
        {
            log.info(io.getMessage());
        }
        return existingMD;
    }
    public final void changeMetadata(List<JTextField> metadataFields)
    {
        int i=1;
        for(JTextField field:metadataFields)
        {
            if(i==1)
            {
                if(field.getText().length() > 0)
                {
                    this.metadataLbl1.setText(field.getText());
                    this.metadataLbl1.setVisible(true);
                    this.metadata1.setVisible(true);
                }
                else{
                    this.metadataLbl1.setVisible(false);
                    this.metadata1.setVisible(false);
                }
            }
            else if(i==2)
            {
                if(field.getText().length() > 0)
                {
                    this.metadataLbl2.setText(field.getText());
                    this.metadataLbl2.setVisible(true);
                    this.metadata2.setVisible(true);
                }
                else
                {
                    this.metadataLbl2.setVisible(false);
                    this.metadata2.setVisible(false);
                }
            }
            else if(i==3)
            {
                if(field.getText().length() > 0)
                {
                    this.metadataLbl3.setText(field.getText());
                    this.metadataLbl3.setVisible(true);
                    this.metadata3.setVisible(true);
                }
                else
                {
                    this.metadataLbl3.setVisible(false);
                    this.metadata3.setVisible(false);
                }
            }
            else if(i==4)
            {
                if(field.getText().length() > 0)
                {
                    this.metadataLbl4.setText(field.getText());
                    this.metadataLbl4.setVisible(true);
                    this.metadata4.setVisible(true);
                }
                else
                {
                    this.metadataLbl4.setVisible(false);
                    this.metadata4.setVisible(false);
                }
            }
            else
            {
                if(field.getText().length() > 0)
                {
                    this.metadataLbl5.setText(field.getText());
                    this.metadataLbl5.setVisible(true);
                    this.metadata5.setVisible(true);
                }
                else
                {
                    this.metadataLbl5.setVisible(false);
                    this.metadata5.setVisible(false);
                }
            }
            i++;

        }
    }
    
    @Action
    public void stopRunningProgramAndUploadData()
    {
        terminateThreadsAndRestoreButtonStates();
        List<File> filesCreated = (List<File>)this.watcher.getListener().getCreatedFiles();
        experimentFinished(filesCreated);
    }
    public void experimentFinished(List<File> files)
    {
        ExpDataUpload expUploadObj = new ExpDataUpload(PATView.log,this.getFrame(),this.properties);                
        String sampId = this.sampleId.getText();
        String[] splits= sampId.split(":");
        sampId = sampId.substring(sampId.lastIndexOf(":") +1);
        Instrument instrument = (Instrument)this.instrumentName.getSelectedItem();        
        int projId =-99;
        int expId =-99;
        for(int i=0;i<splits.length;i+=2)
        {
            if(i == 0){
                projId = Integer.parseInt(splits[1].trim());
            }
            else{
                if("Expr".equals((String)splits[i]))
                {
                    expId = Integer.parseInt(splits[i+1]);
                }                
            }
        }
        Map<String,String> map = new HashMap<String,String>();
        
        if(this.metadata1.getText().length()>0){
            map.put(this.metadataLbl1.getText(), this.metadata1.getText());
        }
        
        if(this.metadata2.getText().length()>0){
            map.put(this.metadataLbl2.getText(), this.metadata2.getText());
        }
        
        if(this.metadata3.getText().length()>0){
            map.put(this.metadataLbl3.getText(), this.metadata3.getText());
        }

        if(this.metadata4.getText().length()>0){
            map.put(this.metadataLbl4.getText(), this.metadata4.getText());
        }

        if(this.metadata5.getText().length()>0){
            map.put(this.metadataLbl5.getText(), this.metadata5.getText());
        }


        
        expUploadObj.setMetaData(sampId, 
                                Integer.toString(instrument.getId()),
                                this.datasetName.getText(),
                                map,
                                files, 
                                this.commentsTxtArea.getText(),
                                projId,
                                expId);
        
        this.patClient.uploadExperimentData(expUploadObj,this.emailConfirmChk.isSelected());
        terminateThreadsAndRestoreButtonStates();
        Notify.info(this.getFrame(), "Upload finished","The experiment data upload has finished");
    }
    @Action
    public void showHelp()throws Exception
    {
       if(Desktop.isDesktopSupported())    
       {
           desktop = Desktop.getDesktop();
           URI uri = new URI("https://www.researchdata.unsw.edu.au/help/index.php/Data_Capture_Automation_Tool");
           desktop.browse(uri);
       }
       else
       {
           Notify.error(this.getFrame(),"OS problem", "Cannot launch the browser on your machine, please ensure you have installed the dependancies");
       }
    }
    @Action
    public void runProgram()throws Exception
    {
        if(!(this.instrumentName.getSelectedItem() instanceof Instrument))
        {
            Notify.error(this.getFrame(), "Validation Error", "Select an instrument before running the experiment");
            return;
        }
        else{
            //once the program is running lets disable the return to the login page
            changeMetaDataFieldState(false);
            this.browseFiles.setEnabled(false);
                    
            this.patTabs.setEnabledAt(0, false);
            this.samplesTree.setEnabled(false);
            this.emailConfirmChk.setEnabled(false);
            this.runProgBtn.setEnabled(false);
            this.cancelRuningProgBtn.setEnabled(true);
            this.finishAndUploadBtn.setEnabled(true);
            this.refreshSamplesBtn.setEnabled(false);
            
            this.datasetName.setEnabled(false);
            this.finishFilename.setEnabled(false);
            this.outputLocation.setEnabled(false);

            this.watcher = new PATFileWatcher(PATView.log,
                                              this.getFrame(),
                                              this.properties,
                                              this,
                                              pathName);
            
            this.watcher.setup(this.finishFilename.getText());        
            long interval =100;
            this.monitor = new FileAlterationMonitor(interval,this.watcher.getObserver());
            this.isProgRunning = true;
            runIndicator.setKeepThreadAlive(true);
            indicatorThread = new Thread(runIndicator);
            
            indicatorThread.start();
            this.monitor.start();        
        }
    }
    public void terminateThreadsAndRestoreButtonStates()
    {
        try
        {
            indicatorThread.interrupt();
            this.runIndicator.setKeepThreadAlive(false);
            this.monitor.stop();
            
        }catch(Exception e)
        {
            PATView.log.severe(e.getMessage());
        }
        changeMetaDataFieldState(true);
        this.isProgRunning = false;
        
        this.cancelRuningProgBtn.setEnabled(false);
        this.finishAndUploadBtn.setEnabled(false);
        this.runProgBtn.setEnabled(true);
        this.patTabs.setEnabledAt(0, true);
        this.browseFiles.setEnabled(true);
        
        this.samplesTree.setEnabled(true);
        this.emailConfirmChk.setEnabled(true);
        this.refreshSamplesBtn.setEnabled(true);
        
        this.datasetName.setEnabled(true);
        this.finishFilename.setEnabled(true);
        this.outputLocation.setEnabled(true);
    }
    @Action
    public void stopProgram()
    {   
        terminateThreadsAndRestoreButtonStates();
        Notify.info(this.getFrame(), "Program Aborted","The Program has been aborted");
    }    
    public void changeMetaDataFieldState(boolean isEditable)
    {
        this.metadata1.setEnabled(isEditable);
        this.metadata2.setEnabled(isEditable);
        this.instrumentName.setEnabled(isEditable);
        this.metadata3.setEnabled(isEditable);
        this.metadata4.setEnabled(isEditable);
        this.metadata5.setEnabled(isEditable);
        this.runProgBtn.setEnabled(isEditable);
        this.emailConfirmChk.setEnabled(isEditable);
        this.commentsTxtArea.setEditable(isEditable);
    }
    public void isMetaDataEditable()
    {
        if(!this.isProgRunning)
        {
            boolean isEditable = false;

            isEditable = this.sampleId.getText().length()>0? true:false;

            if(isEditable)
            {
                isEditable = this.datasetName.getText().length()>0? true:false;
            }        
            if(isEditable)
            {
                isEditable = this.outputLocation.getText().length()>0? true:false;
            }
            if(isEditable)
            {
                isEditable = this.finishFilename.getText().length()>0? true:false;
            }

            changeMetaDataFieldState(isEditable);
        }
    }
    public void addInstrumentsToMenu(String[]instrumentNames)
    {
        
        InstrSelectedListener ls = new InstrSelectedListener(this);
        for(int i=0;i<instrumentNames.length;i++)
        {
            JMenuItem item = new JMenuItem();
            item.setText(instrumentNames[i]);
            item.addActionListener(ls);
            this.instrumentsMenu.add(item);
        }
        this.instrumentsMenu.revalidate();
    }
    private class InstrSelectedListener implements ActionListener
    {
        public PATView view;
        public InstrSelectedListener(PATView vw)
        {
            this.view = vw;
        }
        public void actionPerformed(ActionEvent ev)
        {
            this.view.setInstrumentsCombo(ev.getActionCommand());
        }
    }
    
    public void setInstrumentsCombo(String name)
    {
        try{
        patClient.initializeInstruments(name);   
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        
        if(patClient.getInstruments().size()>0)
        {
            for(Instrument i:patClient.getInstruments())
            {
                model.addElement(i);
            }
        }     
        this.instrumentName.setModel(model);
        
        }catch(Exception e)
        {
            PATView.log.severe("Failed to load the instruments");
            PATView.log.severe(e.getMessage());
        }
    }
    public DefaultComboBoxModel populateInstruments()
    {
        
        String defaultItem[] = {"----------"};
        
        DefaultComboBoxModel model = new DefaultComboBoxModel(defaultItem);
        return model;
    }
    
    /**
     * This badly named method is fired when the refresh button is clicked
     */
    @Action
    public void refreshSamples()
    {
        final DCATTreeNode rootNode = this.treeRoot;
        final PatCLIENT client = this.patClient;
        final javax.swing.JTree tree = this.samplesTree;
        if(client.getUserDataNode() !=null)
        {
            (new Thread()
            {
                @Override
                public void run()
                {
                    rootNode.removeAllChildren();
                    client.initializeSamples();
                    DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                    model.reload();
                    rootNode.add(client.getUserDataNode());
                    model.reload();
                }
            }).start();
        }
        else
        {
            Notify.error(this.getFrame(), "Illegal Action", "You need to logon first before trying this action");
        }
    }
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        patTabs = new javax.swing.JTabbedPane();
        loginPnl = new javax.swing.JPanel();
        username = new javax.swing.JTextField();
        password = new javax.swing.JPasswordField();
        loginLbl = new javax.swing.JLabel();
        loginBtn = new javax.swing.JButton();
        passwordLbl = new javax.swing.JLabel();
        usernameLbl = new javax.swing.JLabel();
        loginInfoLbl = new javax.swing.JLabel();
        acDataLInk = new javax.swing.JLabel();
        patWorkAreaPnl = new javax.swing.JPanel();
        samplesScrollPane = new javax.swing.JScrollPane();
        samplesTree = new javax.swing.JTree();
        refreshSamplesBtn = new javax.swing.JButton();
        sampleIdLbl = new javax.swing.JLabel();
        sampleId = new javax.swing.JTextField();
        sampleDisclaimerLbl = new javax.swing.JLabel();
        datasetName = new javax.swing.JTextField();
        dsNameLbl = new javax.swing.JLabel();
        dirLbl = new javax.swing.JLabel();
        outputLocation = new javax.swing.JTextField();
        browseFiles = new javax.swing.JButton();
        metadataTabs = new javax.swing.JTabbedPane();
        instrumentNameLbl = new javax.swing.JPanel();
        metadata = new javax.swing.JLabel();
        metadataLbl1 = new javax.swing.JLabel();
        metadataLbl2 = new javax.swing.JLabel();
        metadataLbl3 = new javax.swing.JLabel();
        metadataLbl4 = new javax.swing.JLabel();
        metadata1 = new javax.swing.JTextField();
        metadata2 = new javax.swing.JTextField();
        metadataLbl5 = new javax.swing.JLabel();
        metadata3 = new javax.swing.JTextField();
        metadata4 = new javax.swing.JTextField();
        metadata5 = new javax.swing.JTextField();
        instrumentName = new javax.swing.JComboBox();
        commentsPnl = new javax.swing.JPanel();
        commentsLbl = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        commentsTxtArea = new javax.swing.JTextArea();
        runProgBtn = new javax.swing.JButton();
        cancelRuningProgBtn = new javax.swing.JButton();
        emailConfirmChk = new javax.swing.JCheckBox();
        emailConfirmationLbl = new javax.swing.JLabel();
        mandatoryFieldsLbl = new javax.swing.JLabel();
        acDataLinkLbl = new javax.swing.JLabel();
        dirLbl1 = new javax.swing.JLabel();
        finishFilename = new javax.swing.JTextField();
        progressLbl = new javax.swing.JLabel();
        finishAndUploadBtn = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        potentioStatLogo = new javax.swing.JTextArea();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        instrumentsMenu = new javax.swing.JMenu();
        jMenu3 = new javax.swing.JMenu();
        jMenuItem3 = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(pat.PATApp.class).getContext().getResourceMap(PATView.class);
        mainPanel.setBackground(resourceMap.getColor("mainPanel.background")); // NOI18N
        mainPanel.setName("mainPanel"); // NOI18N

        patTabs.setBorder(new javax.swing.border.LineBorder(resourceMap.getColor("patTabs.border.lineColor"), 1, true)); // NOI18N
        patTabs.setName("patTabs"); // NOI18N

        loginPnl.setBackground(resourceMap.getColor("loginPnl.background")); // NOI18N
        loginPnl.setName("loginPnl"); // NOI18N
        loginPnl.setPreferredSize(new java.awt.Dimension(628, 426));

        username.setName("username"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(pat.PATApp.class).getContext().getActionMap(PATView.class, this);
        password.setAction(actionMap.get("userLogin")); // NOI18N
        password.setName("password"); // NOI18N

        loginLbl.setFont(resourceMap.getFont("loginLbl.font")); // NOI18N
        loginLbl.setText(resourceMap.getString("loginLbl.text")); // NOI18N
        loginLbl.setName("loginLbl"); // NOI18N

        loginBtn.setAction(actionMap.get("userLogin")); // NOI18N
        loginBtn.setFont(resourceMap.getFont("loginBtn.font")); // NOI18N
        loginBtn.setText(resourceMap.getString("loginBtn.text")); // NOI18N
        loginBtn.setName("loginBtn"); // NOI18N

        passwordLbl.setFont(resourceMap.getFont("passwordLbl.font")); // NOI18N
        passwordLbl.setLabelFor(password);
        passwordLbl.setText(resourceMap.getString("passwordLbl.text")); // NOI18N
        passwordLbl.setName("passwordLbl"); // NOI18N

        usernameLbl.setFont(resourceMap.getFont("usernameLbl.font")); // NOI18N
        usernameLbl.setText(resourceMap.getString("usernameLbl.text")); // NOI18N
        usernameLbl.setName("usernameLbl"); // NOI18N

        loginInfoLbl.setFont(resourceMap.getFont("loginInfoLbl.font")); // NOI18N
        loginInfoLbl.setText(resourceMap.getString("loginInfoLbl.text")); // NOI18N
        loginInfoLbl.setName("loginInfoLbl"); // NOI18N

        acDataLInk.setFont(resourceMap.getFont("acDataLInk.font")); // NOI18N
        acDataLInk.setForeground(resourceMap.getColor("acDataLInk.foreground")); // NOI18N
        acDataLInk.setText(resourceMap.getString("acDataLInk.text")); // NOI18N
        acDataLInk.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        acDataLInk.setName("acDataLInk"); // NOI18N
        acDataLInk.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                acDataLinkLblMouseClicked(evt);
            }
        });

        org.jdesktop.layout.GroupLayout loginPnlLayout = new org.jdesktop.layout.GroupLayout(loginPnl);
        loginPnl.setLayout(loginPnlLayout);
        loginPnlLayout.setHorizontalGroup(
            loginPnlLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(loginPnlLayout.createSequentialGroup()
                .add(loginPnlLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(loginPnlLayout.createSequentialGroup()
                        .add(160, 160, 160)
                        .add(loginPnlLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(loginLbl, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 67, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(loginInfoLbl)
                            .add(loginPnlLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                                .add(org.jdesktop.layout.GroupLayout.LEADING, loginBtn, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .add(org.jdesktop.layout.GroupLayout.LEADING, loginPnlLayout.createSequentialGroup()
                                    .add(loginPnlLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                        .add(passwordLbl, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 73, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(usernameLbl))
                                    .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                    .add(loginPnlLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                                        .add(password)
                                        .add(username, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 171, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))))
                    .add(acDataLInk))
                .addContainerGap(340, Short.MAX_VALUE))
        );
        loginPnlLayout.setVerticalGroup(
            loginPnlLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(loginPnlLayout.createSequentialGroup()
                .addContainerGap()
                .add(acDataLInk)
                .add(65, 65, 65)
                .add(loginLbl)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(loginInfoLbl)
                .add(18, 18, 18)
                .add(loginPnlLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(usernameLbl)
                    .add(username, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(11, 11, 11)
                .add(loginPnlLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(passwordLbl)
                    .add(password, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(18, 18, 18)
                .add(loginBtn)
                .addContainerGap(312, Short.MAX_VALUE))
        );

        patTabs.addTab(resourceMap.getString("loginPnl.TabConstraints.tabTitle"), loginPnl); // NOI18N

        patWorkAreaPnl.setBackground(resourceMap.getColor("patWorkAreaPnl.background")); // NOI18N
        patWorkAreaPnl.setName("patWorkAreaPnl"); // NOI18N

        samplesScrollPane.setBorder(null);
        samplesScrollPane.setName("samplesScrollPane"); // NOI18N

        samplesTree.setBorder(javax.swing.BorderFactory.createTitledBorder(null, resourceMap.getString("samplesTree.border.title"), javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.DEFAULT_POSITION, resourceMap.getFont("samplesTree.border.titleFont"))); // NOI18N
        samplesTree.setModel(populateTree());
        samplesTree.setCellRenderer(new DCATTreeCellRenderer());
        samplesTree.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        samplesTree.setName("samplesTree"); // NOI18N
        samplesTree.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                samplesTreeValueChanged(evt);
            }
        });
        samplesScrollPane.setViewportView(samplesTree);

        refreshSamplesBtn.setAction(actionMap.get("refreshSamples")); // NOI18N
        refreshSamplesBtn.setFont(resourceMap.getFont("refreshSamplesBtn.font")); // NOI18N
        refreshSamplesBtn.setText(resourceMap.getString("refreshSamplesBtn.text")); // NOI18N
        refreshSamplesBtn.setToolTipText(resourceMap.getString("refreshSamplesBtn.toolTipText")); // NOI18N
        refreshSamplesBtn.setActionCommand(resourceMap.getString("refreshSamplesBtn.actionCommand")); // NOI18N
        refreshSamplesBtn.setName("refreshSamplesBtn"); // NOI18N

        sampleIdLbl.setFont(resourceMap.getFont("sampleIdLbl.font")); // NOI18N
        sampleIdLbl.setText(resourceMap.getString("sampleIdLbl.text")); // NOI18N
        sampleIdLbl.setName("sampleIdLbl"); // NOI18N

        sampleId.setText(resourceMap.getString("sampleId.text")); // NOI18N
        sampleId.setEnabled(false);
        sampleId.setName("sampleId"); // NOI18N
        sampleId.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                sampleIdFocusLost(evt);
            }
        });
        sampleId.addInputMethodListener(new java.awt.event.InputMethodListener() {
            public void caretPositionChanged(java.awt.event.InputMethodEvent evt) {
                sampleIdCaretPositionChanged(evt);
            }
            public void inputMethodTextChanged(java.awt.event.InputMethodEvent evt) {
            }
        });

        sampleDisclaimerLbl.setFont(resourceMap.getFont("sampleDisclaimerLbl.font")); // NOI18N
        sampleDisclaimerLbl.setText(resourceMap.getString("sampleDisclaimerLbl.text")); // NOI18N
        sampleDisclaimerLbl.setName("sampleDisclaimerLbl"); // NOI18N

        datasetName.setText(resourceMap.getString("datasetName.text")); // NOI18N
        datasetName.setToolTipText(resourceMap.getString("datasetName.toolTipText")); // NOI18N
        datasetName.setName("datasetName"); // NOI18N
        datasetName.addCaretListener(new javax.swing.event.CaretListener() {
            public void caretUpdate(javax.swing.event.CaretEvent evt) {
                datasetNameCaretUpdate(evt);
            }
        });
        datasetName.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                datasetNameFocusLost(evt);
            }
        });

        dsNameLbl.setFont(resourceMap.getFont("dsNameLbl.font")); // NOI18N
        dsNameLbl.setText(resourceMap.getString("dsNameLbl.text")); // NOI18N
        dsNameLbl.setName("dsNameLbl"); // NOI18N

        dirLbl.setFont(resourceMap.getFont("dirLbl.font")); // NOI18N
        dirLbl.setText(resourceMap.getString("dirLbl.text")); // NOI18N
        dirLbl.setName("dirLbl"); // NOI18N

        outputLocation.setEditable(false);
        outputLocation.setText(resourceMap.getString("outputLocation.text")); // NOI18N
        outputLocation.setToolTipText(resourceMap.getString("outputLocation.toolTipText")); // NOI18N
        outputLocation.setName("outputLocation"); // NOI18N
        outputLocation.addCaretListener(new javax.swing.event.CaretListener() {
            public void caretUpdate(javax.swing.event.CaretEvent evt) {
                outputLocationCaretUpdate(evt);
            }
        });

        browseFiles.setAction(actionMap.get("browseFiles")); // NOI18N
        browseFiles.setFont(resourceMap.getFont("browseFiles.font")); // NOI18N
        browseFiles.setText(resourceMap.getString("browseFiles.text")); // NOI18N
        browseFiles.setName("browseFiles"); // NOI18N

        metadataTabs.setName("metadataTabs"); // NOI18N

        instrumentNameLbl.setBackground(resourceMap.getColor("instrumentNameLbl.background")); // NOI18N
        instrumentNameLbl.setName("instrumentNameLbl"); // NOI18N

        metadata.setFont(resourceMap.getFont("metadata.font")); // NOI18N
        metadata.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        metadata.setText(resourceMap.getString("metadata.text")); // NOI18N
        metadata.setName("metadata"); // NOI18N

        metadataLbl1.setFont(resourceMap.getFont("metadataLbl1.font")); // NOI18N
        metadataLbl1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        metadataLbl1.setText(resourceMap.getString("metadataLbl1.text")); // NOI18N
        metadataLbl1.setName("metadataLbl1"); // NOI18N

        metadataLbl2.setFont(resourceMap.getFont("metadataLbl2.font")); // NOI18N
        metadataLbl2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        metadataLbl2.setText(resourceMap.getString("metadataLbl2.text")); // NOI18N
        metadataLbl2.setName("metadataLbl2"); // NOI18N

        metadataLbl3.setFont(resourceMap.getFont("metadataLbl3.font")); // NOI18N
        metadataLbl3.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        metadataLbl3.setText(resourceMap.getString("metadataLbl3.text")); // NOI18N
        metadataLbl3.setName("metadataLbl3"); // NOI18N

        metadataLbl4.setFont(resourceMap.getFont("metadataLbl4.font")); // NOI18N
        metadataLbl4.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        metadataLbl4.setText(resourceMap.getString("metadataLbl4.text")); // NOI18N
        metadataLbl4.setName("metadataLbl4"); // NOI18N

        metadata1.setColumns(15);
        metadata1.setToolTipText(resourceMap.getString("metadata1.toolTipText")); // NOI18N
        metadata1.setEnabled(false);
        metadata1.setName("metadata1"); // NOI18N

        metadata2.setColumns(15);
        metadata2.setToolTipText(resourceMap.getString("metadata2.toolTipText")); // NOI18N
        metadata2.setEnabled(false);
        metadata2.setName("metadata2"); // NOI18N

        metadataLbl5.setFont(resourceMap.getFont("metadataLbl5.font")); // NOI18N
        metadataLbl5.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        metadataLbl5.setText(resourceMap.getString("metadataLbl5.text")); // NOI18N
        metadataLbl5.setName("metadataLbl5"); // NOI18N

        metadata3.setColumns(15);
        metadata3.setText(resourceMap.getString("metadata3.text")); // NOI18N
        metadata3.setToolTipText(resourceMap.getString("metadata3.toolTipText")); // NOI18N
        metadata3.setEnabled(false);
        metadata3.setName("metadata3"); // NOI18N

        metadata4.setColumns(15);
        metadata4.setText(resourceMap.getString("metadata4.text")); // NOI18N
        metadata4.setToolTipText(resourceMap.getString("metadata4.toolTipText")); // NOI18N
        metadata4.setEnabled(false);
        metadata4.setName("metadata4"); // NOI18N

        metadata5.setColumns(15);
        metadata5.setText(resourceMap.getString("metadata5.text")); // NOI18N
        metadata5.setToolTipText(resourceMap.getString("metadata5.toolTipText")); // NOI18N
        metadata5.setEnabled(false);
        metadata5.setName("metadata5"); // NOI18N

        instrumentName.setModel(populateInstruments());
        instrumentName.setToolTipText(resourceMap.getString("instrumentName.toolTipText")); // NOI18N
        instrumentName.setEnabled(false);
        instrumentName.setName("instrumentName"); // NOI18N

        org.jdesktop.layout.GroupLayout instrumentNameLblLayout = new org.jdesktop.layout.GroupLayout(instrumentNameLbl);
        instrumentNameLbl.setLayout(instrumentNameLblLayout);
        instrumentNameLblLayout.setHorizontalGroup(
            instrumentNameLblLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, instrumentNameLblLayout.createSequentialGroup()
                .add(instrumentNameLblLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(instrumentNameLblLayout.createSequentialGroup()
                        .add(10, 10, 10)
                        .add(instrumentNameLblLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(metadataLbl3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 128, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(metadataLbl5, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 169, Short.MAX_VALUE)
                            .add(metadataLbl4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 120, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(metadataLbl2))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED))
                    .add(org.jdesktop.layout.GroupLayout.LEADING, instrumentNameLblLayout.createSequentialGroup()
                        .add(19, 19, 19)
                        .add(instrumentNameLblLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(metadataLbl1)
                            .add(metadata, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 117, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                .add(instrumentNameLblLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(instrumentNameLblLayout.createSequentialGroup()
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(instrumentName, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 245, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, metadata2)
                    .add(metadata5)
                    .add(metadata4)
                    .add(metadata3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 171, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, metadata1))
                .add(214, 214, 214))
        );
        instrumentNameLblLayout.setVerticalGroup(
            instrumentNameLblLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(instrumentNameLblLayout.createSequentialGroup()
                .add(31, 31, 31)
                .add(instrumentNameLblLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(metadata)
                    .add(instrumentName, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(instrumentNameLblLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(metadata1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(metadataLbl1))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(instrumentNameLblLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(metadata2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(metadataLbl2))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(instrumentNameLblLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(metadataLbl3)
                    .add(metadata3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(8, 8, 8)
                .add(instrumentNameLblLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(metadataLbl4)
                    .add(metadata4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(instrumentNameLblLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(metadataLbl5)
                    .add(metadata5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(76, Short.MAX_VALUE))
        );

        metadataTabs.addTab(resourceMap.getString("instrumentNameLbl.TabConstraints.tabTitle"), instrumentNameLbl); // NOI18N

        commentsPnl.setBackground(resourceMap.getColor("commentsPnl.background")); // NOI18N
        commentsPnl.setName("commentsPnl"); // NOI18N

        commentsLbl.setFont(resourceMap.getFont("commentsLbl.font")); // NOI18N
        commentsLbl.setText(resourceMap.getString("commentsLbl.text")); // NOI18N
        commentsLbl.setName("commentsLbl"); // NOI18N

        jScrollPane3.setName("jScrollPane3"); // NOI18N

        commentsTxtArea.setColumns(20);
        commentsTxtArea.setEditable(false);
        commentsTxtArea.setFont(resourceMap.getFont("commentsTxtArea.font")); // NOI18N
        commentsTxtArea.setRows(5);
        commentsTxtArea.setName("commentsTxtArea"); // NOI18N
        jScrollPane3.setViewportView(commentsTxtArea);

        org.jdesktop.layout.GroupLayout commentsPnlLayout = new org.jdesktop.layout.GroupLayout(commentsPnl);
        commentsPnl.setLayout(commentsPnlLayout);
        commentsPnlLayout.setHorizontalGroup(
            commentsPnlLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(commentsPnlLayout.createSequentialGroup()
                .addContainerGap()
                .add(commentsPnlLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jScrollPane3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 622, Short.MAX_VALUE)
                    .add(commentsLbl))
                .addContainerGap())
        );
        commentsPnlLayout.setVerticalGroup(
            commentsPnlLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(commentsPnlLayout.createSequentialGroup()
                .addContainerGap()
                .add(commentsLbl)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 216, Short.MAX_VALUE)
                .addContainerGap())
        );

        metadataTabs.addTab(resourceMap.getString("commentsPnl.TabConstraints.tabTitle"), commentsPnl); // NOI18N

        runProgBtn.setAction(actionMap.get("runProgram")); // NOI18N
        runProgBtn.setFont(resourceMap.getFont("runProgBtn.font")); // NOI18N
        runProgBtn.setText(resourceMap.getString("runProgBtn.text")); // NOI18N
        runProgBtn.setName("runProgBtn"); // NOI18N

        cancelRuningProgBtn.setAction(actionMap.get("stopProgram")); // NOI18N
        cancelRuningProgBtn.setFont(resourceMap.getFont("cancelRuningProgBtn.font")); // NOI18N
        cancelRuningProgBtn.setText(resourceMap.getString("cancelRuningProgBtn.text")); // NOI18N
        cancelRuningProgBtn.setName("cancelRuningProgBtn"); // NOI18N

        emailConfirmChk.setBackground(resourceMap.getColor("emailConfirmChk.background")); // NOI18N
        emailConfirmChk.setText(resourceMap.getString("emailConfirmChk.text")); // NOI18N
        emailConfirmChk.setToolTipText(resourceMap.getString("emailConfirmChk.toolTipText")); // NOI18N
        emailConfirmChk.setEnabled(false);
        emailConfirmChk.setName("emailConfirmChk"); // NOI18N

        emailConfirmationLbl.setFont(resourceMap.getFont("emailConfirmationLbl.font")); // NOI18N
        emailConfirmationLbl.setText(resourceMap.getString("emailConfirmationLbl.text")); // NOI18N
        emailConfirmationLbl.setName("emailConfirmationLbl"); // NOI18N

        mandatoryFieldsLbl.setText(resourceMap.getString("mandatoryFieldsLbl.text")); // NOI18N
        mandatoryFieldsLbl.setName("mandatoryFieldsLbl"); // NOI18N

        acDataLinkLbl.setFont(resourceMap.getFont("acDataLinkLbl.font")); // NOI18N
        acDataLinkLbl.setForeground(resourceMap.getColor("acDataLinkLbl.foreground")); // NOI18N
        acDataLinkLbl.setText(resourceMap.getString("acDataLinkLbl.text")); // NOI18N
        acDataLinkLbl.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        acDataLinkLbl.setName("acDataLinkLbl"); // NOI18N
        acDataLinkLbl.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                acDataLinkLblMouseClicked(evt);
            }
        });

        dirLbl1.setFont(resourceMap.getFont("dirLbl1.font")); // NOI18N
        dirLbl1.setText(resourceMap.getString("dirLbl1.text")); // NOI18N
        dirLbl1.setName("dirLbl1"); // NOI18N

        finishFilename.setText(resourceMap.getString("finishFilename.text")); // NOI18N
        finishFilename.setName("finishFilename"); // NOI18N
        finishFilename.addCaretListener(new javax.swing.event.CaretListener() {
            public void caretUpdate(javax.swing.event.CaretEvent evt) {
                finishFilenameCaretUpdate(evt);
            }
        });
        finishFilename.addInputMethodListener(new java.awt.event.InputMethodListener() {
            public void caretPositionChanged(java.awt.event.InputMethodEvent evt) {
                finishFilenameCaretPositionChanged(evt);
            }
            public void inputMethodTextChanged(java.awt.event.InputMethodEvent evt) {
            }
        });

        progressLbl.setIcon(resourceMap.getIcon("progressLbl.icon")); // NOI18N
        progressLbl.setText(resourceMap.getString("progressLbl.text")); // NOI18N
        progressLbl.setName("progressLbl"); // NOI18N

        finishAndUploadBtn.setAction(actionMap.get("stopRunningProgramAndUploadData")); // NOI18N
        finishAndUploadBtn.setText(resourceMap.getString("finishAndUploadBtn.text")); // NOI18N
        finishAndUploadBtn.setName("finishAndUploadBtn"); // NOI18N

        org.jdesktop.layout.GroupLayout patWorkAreaPnlLayout = new org.jdesktop.layout.GroupLayout(patWorkAreaPnl);
        patWorkAreaPnl.setLayout(patWorkAreaPnlLayout);
        patWorkAreaPnlLayout.setHorizontalGroup(
            patWorkAreaPnlLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(patWorkAreaPnlLayout.createSequentialGroup()
                .add(patWorkAreaPnlLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, patWorkAreaPnlLayout.createSequentialGroup()
                        .addContainerGap()
                        .add(refreshSamplesBtn, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 174, Short.MAX_VALUE))
                    .add(samplesScrollPane, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 184, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(patWorkAreaPnlLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(patWorkAreaPnlLayout.createSequentialGroup()
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(patWorkAreaPnlLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(patWorkAreaPnlLayout.createSequentialGroup()
                                .add(patWorkAreaPnlLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                                    .add(org.jdesktop.layout.GroupLayout.LEADING, patWorkAreaPnlLayout.createSequentialGroup()
                                        .add(acDataLinkLbl, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 123, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .add(mandatoryFieldsLbl, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 204, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                    .add(patWorkAreaPnlLayout.createSequentialGroup()
                                        .add(patWorkAreaPnlLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                            .add(patWorkAreaPnlLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                                                .add(sampleDisclaimerLbl)
                                                .add(patWorkAreaPnlLayout.createSequentialGroup()
                                                    .add(patWorkAreaPnlLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                                        .add(dsNameLbl)
                                                        .add(dirLbl))
                                                    .add(18, 18, 18)
                                                    .add(patWorkAreaPnlLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                                                        .add(outputLocation)
                                                        .add(datasetName, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 281, Short.MAX_VALUE)
                                                        .add(finishFilename)))
                                                .add(patWorkAreaPnlLayout.createSequentialGroup()
                                                    .add(sampleIdLbl)
                                                    .add(37, 37, 37)
                                                    .add(sampleId)))
                                            .add(dirLbl1))
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(browseFiles)
                                        .add(11, 11, 11)))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 177, Short.MAX_VALUE))
                            .add(metadataTabs, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 647, Short.MAX_VALUE)
                            .add(patWorkAreaPnlLayout.createSequentialGroup()
                                .add(runProgBtn, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 82, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(finishAndUploadBtn, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 118, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 69, Short.MAX_VALUE)
                                .add(cancelRuningProgBtn)
                                .add(18, 18, 18)
                                .add(emailConfirmChk)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(emailConfirmationLbl, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 249, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(25, 25, 25))))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, patWorkAreaPnlLayout.createSequentialGroup()
                        .add(632, 632, 632)
                        .add(progressLbl, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 25, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        patWorkAreaPnlLayout.setVerticalGroup(
            patWorkAreaPnlLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(patWorkAreaPnlLayout.createSequentialGroup()
                .add(patWorkAreaPnlLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(patWorkAreaPnlLayout.createSequentialGroup()
                        .add(11, 11, 11)
                        .add(patWorkAreaPnlLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(mandatoryFieldsLbl)
                            .add(acDataLinkLbl))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(patWorkAreaPnlLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(sampleIdLbl)
                            .add(sampleId, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(sampleDisclaimerLbl)
                        .add(9, 9, 9)
                        .add(patWorkAreaPnlLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(datasetName, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(dsNameLbl))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(patWorkAreaPnlLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(patWorkAreaPnlLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                .add(outputLocation, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(dirLbl))
                            .add(browseFiles))
                        .add(patWorkAreaPnlLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(patWorkAreaPnlLayout.createSequentialGroup()
                                .add(14, 14, 14)
                                .add(dirLbl1))
                            .add(patWorkAreaPnlLayout.createSequentialGroup()
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(finishFilename, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                    .add(samplesScrollPane, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 435, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(patWorkAreaPnlLayout.createSequentialGroup()
                        .addContainerGap(216, Short.MAX_VALUE)
                        .add(metadataTabs, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 287, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(6, 6, 6)))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 19, Short.MAX_VALUE)
                .add(patWorkAreaPnlLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(progressLbl, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 16, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(patWorkAreaPnlLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                        .add(cancelRuningProgBtn)
                        .add(finishAndUploadBtn)
                        .add(runProgBtn))
                    .add(emailConfirmChk)
                    .add(emailConfirmationLbl)
                    .add(refreshSamplesBtn)))
        );

        refreshSamplesBtn.getAccessibleContext().setAccessibleName(resourceMap.getString("refreshSamplesBtn.AccessibleContext.accessibleName")); // NOI18N
        browseFiles.getAccessibleContext().setAccessibleName(resourceMap.getString("jButton1.AccessibleContext.accessibleName")); // NOI18N

        patTabs.addTab(resourceMap.getString("patWorkAreaPnl.TabConstraints.tabTitle"), patWorkAreaPnl); // NOI18N

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        potentioStatLogo.setBackground(resourceMap.getColor("potentioStatLogo.background")); // NOI18N
        potentioStatLogo.setColumns(20);
        potentioStatLogo.setEditable(false);
        potentioStatLogo.setFont(resourceMap.getFont("potentioStatLogo.font")); // NOI18N
        potentioStatLogo.setForeground(resourceMap.getColor("potentioStatLogo.foreground")); // NOI18N
        potentioStatLogo.setRows(5);
        potentioStatLogo.setText(resourceMap.getString("potentioStatLogo.text")); // NOI18N
        potentioStatLogo.setName("potentioStatLogo"); // NOI18N
        jScrollPane2.setViewportView(potentioStatLogo);

        org.jdesktop.layout.GroupLayout mainPanelLayout = new org.jdesktop.layout.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 871, Short.MAX_VALUE)
            .add(mainPanelLayout.createSequentialGroup()
                .add(13, 13, 13)
                .add(patTabs, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 848, Short.MAX_VALUE)
                .addContainerGap())
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(mainPanelLayout.createSequentialGroup()
                .add(jScrollPane2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 91, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(18, 18, 18)
                .add(patTabs, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 581, Short.MAX_VALUE))
        );

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        instrumentsMenu.setText(resourceMap.getString("instrumentsMenu.text")); // NOI18N
        instrumentsMenu.setName("instrumentsMenu"); // NOI18N
        menuBar.add(instrumentsMenu);

        jMenu3.setAction(actionMap.get("showMDDialogBox")); // NOI18N
        jMenu3.setText(resourceMap.getString("jMenu3.text")); // NOI18N
        jMenu3.setName("jMenu3"); // NOI18N

        jMenuItem3.setAction(actionMap.get("showMDDialogBox")); // NOI18N
        jMenuItem3.setText(resourceMap.getString("jMenuItem3.text")); // NOI18N
        jMenuItem3.setName("jMenuItem3"); // NOI18N
        jMenu3.add(jMenuItem3);

        menuBar.add(jMenu3);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        jMenuItem1.setAction(actionMap.get("showHelp")); // NOI18N
        jMenuItem1.setText(resourceMap.getString("jMenuItem1.text")); // NOI18N
        jMenuItem1.setName("jMenuItem1"); // NOI18N
        helpMenu.add(jMenuItem1);

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        statusPanel.setName("statusPanel"); // NOI18N

        statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        org.jdesktop.layout.GroupLayout statusPanelLayout = new org.jdesktop.layout.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(statusPanelSeparator, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 871, Short.MAX_VALUE)
            .add(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(statusMessageLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 851, Short.MAX_VALUE)
                .add(statusAnimationLabel)
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(statusPanelLayout.createSequentialGroup()
                .add(statusPanelSeparator, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(statusMessageLabel)
                    .add(statusAnimationLabel))
                .add(3, 3, 3))
        );

        jMenuBar1.setName("jMenuBar1"); // NOI18N

        jMenu1.setText(resourceMap.getString("jMenu1.text")); // NOI18N
        jMenu1.setName("jMenu1"); // NOI18N
        jMenuBar1.add(jMenu1);

        jMenu2.setText(resourceMap.getString("jMenu2.text")); // NOI18N
        jMenu2.setName("jMenu2"); // NOI18N
        jMenuBar1.add(jMenu2);

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents

    private void datasetNameFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_datasetNameFocusLost
        
        isMetaDataEditable();
    }//GEN-LAST:event_datasetNameFocusLost

    private void sampleIdFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_sampleIdFocusLost
        isMetaDataEditable();
    }//GEN-LAST:event_sampleIdFocusLost

    private void outputLocationCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_outputLocationCaretUpdate
       isMetaDataEditable();
    }//GEN-LAST:event_outputLocationCaretUpdate
   
    private void acDataLinkLblMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_acDataLinkLblMouseClicked
       try{
           if(Desktop.isDesktopSupported())    
           {
               desktop = Desktop.getDesktop();
               URI uri = new URI(PatCLIENT.ACDATA_ENDPOINT);
               desktop.browse(uri);
           }
           else
           {
               Notify.error(this.getFrame(),"OS problem", "Cannot launch the browser on your machine, please ensure you have installed the dependancies");
           }
       }catch(Exception e)
       {
          log.log(Level.SEVERE,e.getMessage());
       }
    }//GEN-LAST:event_acDataLinkLblMouseClicked

    private void samplesTreeValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_samplesTreeValueChanged
     
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)this.samplesTree.getLastSelectedPathComponent();        
        if(node !=null)
        {
            if(node.isLeaf())
            {
                if(node.getUserObject() instanceof Sample)
                {
                    String text = "";
                    Sample sample = (Sample)node.getUserObject();
                    DefaultMutableTreeNode firstParent =  (DefaultMutableTreeNode)node.getParent();  
                    if(firstParent.getUserObject() instanceof UserWork.Experiment)
                    {
                        DefaultMutableTreeNode projNode =  (DefaultMutableTreeNode)node.getParent().getParent();                        
                        UserWork.Project proj = (UserWork.Project) projNode.getUserObject();
                        text+="Proj:"+proj.getId()+":";
                        
                        
                        UserWork.Experiment exp = (UserWork.Experiment)firstParent.getUserObject();
                        text+="Expr:"+exp.getId()+":";
                    }
                    else
                    {
                        UserWork.Project proj = (UserWork.Project) firstParent.getUserObject();
                        text+="Proj:"+proj.getId()+":";
                        
                    }
                    this.sampleId.setText("");
                    this.sampleId.setText(text+sample.getName()+":"+sample.getId());
                }
            }
        }
    }//GEN-LAST:event_samplesTreeValueChanged

    private void sampleIdCaretPositionChanged(java.awt.event.InputMethodEvent evt) {//GEN-FIRST:event_sampleIdCaretPositionChanged
        // TODO add your handling code here:
        isMetaDataEditable();
    }//GEN-LAST:event_sampleIdCaretPositionChanged

    private void datasetNameCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_datasetNameCaretUpdate
        // TODO add your handling code here:
        isMetaDataEditable();
    }//GEN-LAST:event_datasetNameCaretUpdate

    private void finishFilenameCaretPositionChanged(java.awt.event.InputMethodEvent evt) {//GEN-FIRST:event_finishFilenameCaretPositionChanged
        // TODO add your handling code here:
        isMetaDataEditable();
    }//GEN-LAST:event_finishFilenameCaretPositionChanged

    private void finishFilenameCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_finishFilenameCaretUpdate
        // TODO add your handling code here:
        isMetaDataEditable();
    }//GEN-LAST:event_finishFilenameCaretUpdate
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel acDataLInk;
    private javax.swing.JLabel acDataLinkLbl;
    private javax.swing.JButton browseFiles;
    private javax.swing.JButton cancelRuningProgBtn;
    private javax.swing.JLabel commentsLbl;
    private javax.swing.JPanel commentsPnl;
    private javax.swing.JTextArea commentsTxtArea;
    private javax.swing.JTextField datasetName;
    private javax.swing.JLabel dirLbl;
    private javax.swing.JLabel dirLbl1;
    private javax.swing.JLabel dsNameLbl;
    private javax.swing.JCheckBox emailConfirmChk;
    private javax.swing.JLabel emailConfirmationLbl;
    private javax.swing.JButton finishAndUploadBtn;
    private javax.swing.JTextField finishFilename;
    private javax.swing.JComboBox instrumentName;
    private javax.swing.JPanel instrumentNameLbl;
    private javax.swing.JMenu instrumentsMenu;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JButton loginBtn;
    private javax.swing.JLabel loginInfoLbl;
    private javax.swing.JLabel loginLbl;
    private javax.swing.JPanel loginPnl;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JLabel mandatoryFieldsLbl;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JLabel metadata;
    private javax.swing.JTextField metadata1;
    private javax.swing.JTextField metadata2;
    private javax.swing.JTextField metadata3;
    private javax.swing.JTextField metadata4;
    private javax.swing.JTextField metadata5;
    private javax.swing.JLabel metadataLbl1;
    private javax.swing.JLabel metadataLbl2;
    private javax.swing.JLabel metadataLbl3;
    private javax.swing.JLabel metadataLbl4;
    private javax.swing.JLabel metadataLbl5;
    private javax.swing.JTabbedPane metadataTabs;
    private javax.swing.JTextField outputLocation;
    private javax.swing.JPasswordField password;
    private javax.swing.JLabel passwordLbl;
    private javax.swing.JTabbedPane patTabs;
    private javax.swing.JPanel patWorkAreaPnl;
    private javax.swing.JTextArea potentioStatLogo;
    private javax.swing.JLabel progressLbl;
    private javax.swing.JButton refreshSamplesBtn;
    private javax.swing.JButton runProgBtn;
    private javax.swing.JLabel sampleDisclaimerLbl;
    private javax.swing.JTextField sampleId;
    private javax.swing.JLabel sampleIdLbl;
    private javax.swing.JScrollPane samplesScrollPane;
    private javax.swing.JTree samplesTree;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JTextField username;
    private javax.swing.JLabel usernameLbl;
    // End of variables declaration//GEN-END:variables
   
    //private JTree samplesTree
    private Thread indicatorThread;
    private Indicator runIndicator;
    private String pathName = "";
    private static int maxDirNameLength = 40;
    private FileAlterationMonitor monitor;
    private DCATTreeNode treeRoot = new DCATTreeNode("Projects");
    private PatCLIENT.CookieKeeper cookieKeeper;
    private ResourceMap properties;
    private List<String> generatedFiles;
    //private DCATTreeCellRenderer renderer;
    private PatCLIENT patClient;
    private Desktop desktop;
    private javax.swing.JFileChooser fileChooser;
    private boolean isProgRunning = false;
    private final Timer messageTimer = null;
    private final Timer busyIconTimer = null;   
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;
    private Thread theDirWatcher,cookieThread;
   // private PATFileWatcher watcher;
    public static Logger log;
    private Metadata showMdDialog;
    private JDialog aboutBox;
    private PATFileWatcher watcher;
}
