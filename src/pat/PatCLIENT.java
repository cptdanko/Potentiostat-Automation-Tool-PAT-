/*
 * Author: Bhuman Soni
 * Email: bhumansoni@hotmail.com
 */
package pat;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.UriBuilder;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONTokener;
import org.jdesktop.application.ResourceMap;
import pat.DCATTreeNode.NODE_TYPE;


/**
 *Maybe we can make this a business object which is alive for the duration of 
 * the experiment.
 * An object that is maintained in memory as long as the program lives.
 * @author Bhuman
 */
public class PatCLIENT extends PatComponent implements ACDataClient
{
    /**
     * The cookie sent by the request to the ACData website would be valid
     * for 60 mins. So the client needs to keep re-querying the ACData url
     * at regular intervals. Maybe 30 or 40 mins in the final release of this
     * application.
     * 
     */
    class CookieKeeper implements Runnable
    {   
        private final PatCLIENT client;
        public boolean stopThread = false;
        //BufferedWriter writer;
        public CookieKeeper(PatCLIENT client)
        {
            //this.username = user;
            //this.password = pass;
            this.client = client;
            //writer = new BufferedWriter(new FileWriter("CookieLog.txt"));
        }        
        synchronized void stopThread()
        {
            this.stopThread = true;
        }
        public void run(){
            while(!stopThread){
                try{
               /*for now , for testing purposes lets keep the polling time to 
                * approximately 1 minute.
                */
                    Thread.sleep((1000*60));
                    client.maintainSession();
                   // client.log.log(Level.INFO, "Cookie Polled at:{0}", new Date().toString());                    
                }
                catch(Exception ie)
                {
                    client.log.log(Level.SEVERE, "Error{0}", ie.getMessage());                    
                }
            }            
        }        
    }
    
    public PatCLIENT(Logger logger,JFrame frame,ResourceMap resources)
    {
        super(logger,frame,resources);        
        initializeClient();
    }    
    
    public final void initializeClient() 
    {
        ClientConfig config = new DefaultClientConfig();
        client = Client.create(config);
        webResource = client.resource(UriBuilder.fromPath(ACDATA_ENDPOINT).build());
        
    }
    public synchronized void maintainSession()
    {
        this.webResource.path("api").path("keepalive").cookie(this.sessionCookie);  
    }
    //instead of the userwork project, what we can return is a DefaultMutableTreeNode
    private DCATTreeNode convertProjectsFromJSON(JSONArray ownerProjects,String ownerShipType)
            throws JSONException,IOException
    {
        //BufferedWriter writer = new BufferedWriter(new FileWriter("jsonLOGGING.txt"));
        List<UserWork.Project> projects = new ArrayList<UserWork.Project>();
        
        //this one will most likely come from an outside source during method call
        DCATTreeNode ownerShipNode = new DCATTreeNode(ownerShipType);
        ownerShipNode.setType(NODE_TYPE.OWNERSHIP);
        for(int i=0;i< ownerProjects.length();i++)
        {
            JSONObject jProj = (JSONObject) ownerProjects.get(i);
            
            /*now each of the above have the name of the project i.e. the project object
            * followed by the data on experiments and samples
            */
            UserWork participationType = new UserWork(UserWork.PROJECT_RELATION.OWNER);
            UserWork.Project project = participationType.new Project();
            project.setName(jProj.getString("name"));//set the name of the project
            project.setId(jProj.getInt("id"));
            //this.log.info("project id:"+project.getId());
            DCATTreeNode projectNode = new DCATTreeNode(project);
            //DefaultMutableTreeNode projectNode = new DefaultMutableTreeNode(project);
            projectNode.setType(DCATTreeNode.NODE_TYPE.PROJECT);
            //so lets get all the experiments data
            JSONArray experimentsForProjs = (JSONArray)jProj.get("experiments");
            for(int j=0;j<experimentsForProjs.length();j++)
            {
                JSONObject jExperiments = (JSONObject)experimentsForProjs.get(j);
                UserWork.Experiment exp = participationType.new Experiment();
                exp.setId(jExperiments.getInt("id"));
                exp.setName(jExperiments.getString("name"));
               
                DCATTreeNode exprNode = new DCATTreeNode(exp);
                exprNode.setType(NODE_TYPE.EXPERIMENT);
                JSONArray jExpSamples = (JSONArray)jExperiments.get("samples");
                for(int k=0; k < jExpSamples.length();k++)
                {
                    JSONObject jSample = (JSONObject)jExpSamples.get(k);
                    Sample sample  =new Sample(jSample.getString("name"),jSample.getInt("id"));
                    DCATTreeNode sampleNode = new DCATTreeNode(sample);
                    sampleNode.setType(NODE_TYPE.SAMPLE);
                    exprNode.add(sampleNode);
                    JSONArray jDSList = (JSONArray)jSample.get("datasets");
                    String []dsItems = new String[jDSList.length()];
                    for(int l=0;l<jDSList.length();l++)
                    {
                        dsItems[l]=jDSList.getString(l);
                    }
                }        
                projectNode.add(exprNode);
            }
            JSONArray orphanSamples = (JSONArray)jProj.get("samples");
            //this.log.info("orphan samples:"+orphanSamples);
            for(int j =0;j<orphanSamples.length();j++)
            {
                JSONObject jSample = (JSONObject)orphanSamples.get(j);
                Sample sample  =new Sample(jSample.getString("name"),jSample.getInt("id"));
                DCATTreeNode sampleNode = new DCATTreeNode(sample);
                sampleNode.setType(NODE_TYPE.SAMPLE);
                projectNode.add(sampleNode);
            }
           // project.setExperiments(experiments);
            projects.add(project);
            ownerShipNode.add(projectNode);
        }
       // writer.close();
        return ownerShipNode;
    }
    public synchronized void initializeSamples()
    {
        List<UserWork> projectsAndExps = new ArrayList<UserWork>();
       
        this.userDataNode = new DCATTreeNode("Ownership Type");
        this.userDataNode.setType(NODE_TYPE.NOICON);
        //get the samples
        String userWorkDone = webResource.path("api").path("samples.json").cookie(this.sessionCookie).get(String.class);
        try
        {
            JSONTokener tokens = new JSONTokener(userWorkDone);
            //first we get a big object of all the work the user has done
            JSONObject respObj = new JSONObject(tokens);
            //than we get the Array of all the samples                          
            //now the projectObject basically contains 2 things,
            //the projects which i have owned and the projects that i
            //have collborated on
            JSONObject jProjects = (JSONObject)respObj.get("projects");    
            
            //each owner project has a JSONObject which lists the experiments 
            //as well as the samples inherent to the project
            JSONArray jOwnerProjects =(JSONArray) jProjects.get("owner");
            JSONArray jCollaboratorProjects =(JSONArray) jProjects.get("collaborator");
            
            List<JSONObject> object = new ArrayList<JSONObject>();
            //always remember the hirerarchy projects->experiments->samples
            //this.log.info("Overall project string:"+jOwnerProjects.toString());
            DCATTreeNode ownerNode= convertProjectsFromJSON(jOwnerProjects,"owner");
            DCATTreeNode collaboratorNode= convertProjectsFromJSON(jCollaboratorProjects,"collaborator");
            this.userDataNode.add(ownerNode);
            this.userDataNode.add(collaboratorNode);
        }catch(JSONException jse)
        {
            this.log.log(Level.SEVERE, jse.getMessage());
        }
        catch(IOException io)
        {
            this.log.log(Level.SEVERE, io.getMessage());
        }
    }
    public String[] getInstrumentNamesList()throws JSONException
    {
        if(this.jInstruments ==null|| this.jInstruments.length() <1)
        {
            String instrumentsData =webResource.path("api").path("instruments.json").cookie(this.sessionCookie).get(String.class);
            JSONTokener instTokener = new JSONTokener(instrumentsData);
            JSONObject jAllInstruments = new JSONObject(instTokener); 
            this.jInstruments= jAllInstruments.getJSONObject("instruments");
        }
        JSONArray instrNames = jInstruments.names();
        String []instrLs = new String[jInstruments.length()];
        for(int i=0;i<instrNames.length();i++)
        {
            instrLs[i]= instrNames.getString(i);
        }
        return instrLs;
        
    }
    public void initializeInstruments(String instrumentName)throws JSONException
    {
        this.instruments = new ArrayList<Instrument>();
        JSONArray chemInstruments = jInstruments.getJSONArray(instrumentName);
        for(int i=0;i < chemInstruments.length();i++)
        {
            JSONObject jInstrument = chemInstruments.getJSONObject(i);
            
            Instrument instrument = new Instrument(jInstrument.getInt("id"),
                                              jInstrument.getString("name"));
            this.instruments.add(instrument);
        }

    }
    public synchronized void setSessionCookie(NewCookie cookie)
    {
        this.sessionCookie = cookie;
    }
    public synchronized boolean initializeUser(String username,String password) 
    {
        this.username = username;
        this.password = password;
        boolean isAuthenticated = false;        
        JSONObject user = new JSONObject();
        JSONObject login = new JSONObject();        
        
        try{
            login.put("login", username);
            login.put("password", password);
            user.put("user",login);            
        }catch(org.codehaus.jettison.json.JSONException e)
        {
            log.log(Level.SEVERE, "Error conversting the login details to JSON data");
            log.log(Level.INFO, e.getMessage());
        }
        ClientResponse response = null;
        
        
        try{
            
        response = this.webResource.path("users")
                                .path("sign_in.json")
                                .type(MediaType.APPLICATION_JSON)
                                .post(ClientResponse.class,user.toString());
       
        
        }catch(Exception e)
        {
           
            log.log(Level.SEVERE,"Failed to connect to the ACData, are you sure you are connected to the internet?");
            log.severe(e.getMessage());
            log.severe(e.getLocalizedMessage());
            return false;
        }
        
        if(response.getClientResponseStatus()!=Status.UNAUTHORIZED)
        {  
            this.sessionCookie = response.getCookies().get(0);            
            isAuthenticated = true;
        }       
        return isAuthenticated;
    }

   
    public synchronized void httpClientLogin(String username,String password)
    {
          try
          {
            JSONObject signinJSON = new JSONObject();
            JSONObject userJSON  = new JSONObject();

            userJSON.put("login", username);
            userJSON.put("password", password);
            signinJSON.put("user", userJSON);
            PostMethod postMethod = new PostMethod(this.ACDATA_ENDPOINT + "/users/sign_in.json");
            postMethod.setRequestEntity(
                new StringRequestEntity(signinJSON.toString(),
                                        "application/json",
                                        null)
            );

            this.hcSession = new HttpClient();
            int returnStatusCode = this.hcSession.executeMethod(postMethod);
            
            if (returnStatusCode != HttpStatus.SC_CREATED) 
            {
                Notify.error(this.currentFrame, 
                        this.resources.getString("patView.loginUnSuccessful.header", String.class), 
                        this.resources.getString("patView.loginUnSuccessful.message", String.class));                
            }
        }
        catch(Exception e) 
        {
            Notify.error(this.currentFrame, "JSON error", "Error creating the data");
        }
        
    }
    
    public synchronized void httpClientUpload(PatComponent uploadDataComponent,
                                boolean sendEmailNotication)
    {     
        httpClientLogin(this.username,this.password);
        ExpDataUpload dataComponent = (ExpDataUpload)uploadDataComponent;
        PatHttpClient patHttpClient = new PatHttpClient(this.log,this.currentFrame,this.resources);
        
        ArrayList<String> filenames = new ArrayList<String>();
        for(File f:dataComponent.getFilenames())
        {
            filenames.add(f.getAbsolutePath());
        }
        patHttpClient.doDsCreate(username, password, ACDATA_ENDPOINT, 
                           dataComponent.getSampleId(), 
                           dataComponent.getInstrumentId(), 
                           filenames,
                           dataComponent.getDatasetName(),
                           dataComponent.getMetaData(),
                           this.hcSession,
                           sendEmailNotication,
                           dataComponent);

    }
    public synchronized void uploadExperimentData(PatComponent uploadDataComponent,
                                                    boolean sendEmailNotication)
    {
        httpClientUpload(uploadDataComponent,sendEmailNotication);       
    }
    
    public void sendEmail(PatComponent ds)
    {
        JSONObject jSendEmail = new JSONObject();
        ExpDataUpload dataset = (ExpDataUpload)ds;
        try{
        jSendEmail.put("subject", "Experiment Finished");
        /*jSendEmail.put("message", "Your experiment has finished and the dataset "+
                dataset.getDatasetName()+" has been created for sample "+dataset.getSampleId()+
                " with the "+ dataset.getInstrumentName()+"!");*/
        jSendEmail.put("message","Check your ACdata account");
        }catch(JSONException je)
        {
            this.log.severe("Unable to create the dataset");
        }
        PostMethod postMethod;
        try{
         postMethod = new PostMethod(this.ACDATA_ENDPOINT + "/api/message");
            postMethod.setRequestEntity(
                new StringRequestEntity(jSendEmail.toString(),
                                        "application/json",
                                        null)
            );
        
        this.hcSession = new HttpClient();
        int returnStatusCode = this.hcSession.executeMethod(postMethod);
        }catch(Exception e)
        {
            this.log.severe(e.getMessage());
        }
    }
    public List<Instrument> getInstruments() 
    {
        return instruments;
    }
    
    public DefaultMutableTreeNode getUserDataNode() 
    {
        return userDataNode;
    }    
    private WebResource webResource;
    private Client client;
    private NewCookie sessionCookie;
    private DCATTreeNode userDataNode;
    private List<Instrument> instruments = new ArrayList<Instrument>();
    private JSONObject jInstruments;
    private HttpClient hcSession;
    private String username;
    private String password;

           
    //public static final String ACDATA_ENDPOINT = "http://gsw1-unsw-acdata02-vm.intersect.org.au";
    public static final String ACDATA_ENDPOINT = "https://www.researchdata.unsw.edu.au/";
    //public static final String ACDATA_ENDPOINT = "http://gsw1-unsw-acdata01-vm.intersect.org.au/";
}
