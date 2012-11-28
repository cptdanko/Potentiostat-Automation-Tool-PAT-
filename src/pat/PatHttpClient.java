/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pat;

import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jdesktop.application.ResourceMap;



class PatHttpClient extends PatComponent
{

    private String destURL;
    private String sampleID;
    private String instrumentID;
    private ArrayList<String> attachments;
    private Map<String, File> fileMap;
    BufferedWriter writer;
    
    public PatHttpClient (Logger logger,JFrame frame,ResourceMap resources)
    {
        super(logger,frame,resources);
    }
    public void setHttpClient(  String destURL,
                        String sampleID,
                        String instrumentID,
                        ArrayList<String> attachments)
    {
        this.destURL      = destURL;
        this.sampleID     = sampleID;
        this.instrumentID = instrumentID;
        this.attachments  = attachments;
        this.log.info(destURL);
        this.log.info("sample id:"+sampleID);
        this.log.info("instr id:"+instrumentID);
        this.log.info("attachments:"+attachments.toString());
        
    }

    public void createDataset(HttpClient client,String dsName,JSONObject metaData
                            ,boolean sendEmail,ExpDataUpload expData) 
    {
        try 
        {
            JSONObject json = buildDatasetJSON(dsName,metaData);
            
            PostMethod postMethod = new PostMethod(destURL + "/api/datasets");
            List<Part> partList = new ArrayList<Part>();
            
            this.log.info("preparing to build the String part of the multi-part upload");
            StringPart spart = new StringPart("dataset", json.toString());
            spart.setContentType("application/json");
            spart.setCharSet("utf-8");
            
            partList.add(spart);
            this.log.info("String part built and added");
            
            for (String id : fileMap.keySet()) {
                FilePart filePart = new FilePart(id, fileMap.get(id));
                this.log.info("file part added:"+id+":"+fileMap.get(id));
                partList.add(filePart);
            }

            Part[] parts = partList.toArray(new Part[] {});
            MultipartRequestEntity entity =
                new MultipartRequestEntity(parts, postMethod.getParams());
            postMethod.setRequestEntity(entity);
            this.log.info("Multi-part entity set and now about to do an upload");
            
            int returnStatusCode = client.executeMethod(postMethod);
            this.log.info("Upload response code:"+returnStatusCode);
            this.log.info("proj id:"+expData.getProjId());
            this.log.info("exp id:"+expData.getExpId());
            //sendEmail(client,dsName,expData);
            if(returnStatusCode ==  HttpStatus.SC_CREATED)
            {
                if(sendEmail)
                {
                    sendEmail(client,dsName,expData);
                }
                Notify.info(this.currentFrame, 
                        "Successfull Creation", 
                        "The dataset "+dsName +" has been created");
            }
            else if(returnStatusCode ==  HttpStatus.SC_CONFLICT)
            {
                Notify.info(this.currentFrame, 
                        "Duplicate entry", 
                        "The dataset "+dsName +" already exists, so the following dataset name will"
                        + "be used "+dsName+"(2)");
                dsName +="(2)";
                createDataset(client,dsName,metaData,sendEmail,expData);                
            }
            else if(returnStatusCode ==  HttpStatus.SC_BAD_REQUEST)
            {
                Notify.error(this.currentFrame, 
                        "Dataset not created", 
                        "There maybe something wrong with your sample id or instrument id, contact admin for further clarification");
            }
            else
            {
                
            }
            
        }catch (Exception e) {
            
        }
    }
    private JSONObject buildDatasetJSON(String dsName,JSONObject metaData)
        throws JSONException
    {
        JSONArray filesJSON = new JSONArray();
        fileMap = new HashMap<String, File>();
        int sequence = 0;

        for (String filename : attachments)
        {
            String id = "file_" + Integer.toString(++sequence);
            File file = new File(filename);
            JSONObject jsonObj = new JSONObject();
            jsonObj.put(id, file.getName());
            filesJSON.put(jsonObj);
            fileMap.put(id, new File(filename));
        }

        JSONObject json = new JSONObject();
        json.put("files", filesJSON);
        json.put("sample_id", sampleID);
        json.put("instrument_id", instrumentID);
        json.put("name", dsName);
        json.put("metadata", metaData);
        return json;
    }
    public void sendEmail(HttpClient client,String dsName,ExpDataUpload expData)
    {
         try{ 
            JSONObject userJSON  = new JSONObject();
            String message =  "The dataset "+dsName+" has been created "
                    + "please logon to ACData to check your results! You can check your results at \n "
                    +"http://www.researchdata.unsw.edu.au/projects/"+expData.getProjId();
            if(expData.getExpId() != -99)
            {
                message+="/experiments/"+expData.getExpId()+"/samples/"+expData.getSampleId();
            }
            else{
                message+="/samples/"+expData.getSampleId();
            }
            
            userJSON.put("subject", "Your experiment has finished");
            userJSON.put("message", message);
            
            PostMethod postMethod = new PostMethod(destURL + "/api/message");
            postMethod.setRequestEntity(
                new StringRequestEntity(userJSON.toString(),
                                        "application/json",
                                        null)
            );
            int returnStatusCode = client.executeMethod(postMethod);
            
            log.log(Level.INFO, "Send email response"+ returnStatusCode);
            
            if (returnStatusCode != HttpStatus.SC_CREATED) 
            {
                Notify.error(this.currentFrame, 
                        this.resources.getString("sendEmail.Error.header", String.class), 
                        this.resources.getString("sendEmail.Error.message", String.class));
                
            }
         
        }
        catch(Exception e) {
            Notify.error(this.currentFrame, "JSON error", "Error creating the data");
        }
    }
    public void doDsCreate(String username,
    						String password,
    						String url,
    						String sample_id,
    						String instrument_id,
    						ArrayList<String> filenames,
                                                String dsName,
                                                JSONObject metaData,
                                                HttpClient session,
                                                boolean sendEmail,
                                                ExpDataUpload expData)
    {
        this.setHttpClient(url,sample_id, instrument_id, filenames);
       
        this.createDataset(session,dsName,metaData,sendEmail,expData);
    }
}
