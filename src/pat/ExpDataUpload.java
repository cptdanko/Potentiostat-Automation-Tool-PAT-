/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pat;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.JFrame;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jdesktop.application.ResourceMap;

/**
 *
 * @author lordbhuman
 */
public class ExpDataUpload extends PatComponent
{
    public ExpDataUpload(Logger logger,JFrame frame,ResourceMap resources)
    {
        super(logger,frame,resources);  
    }
    
    public void setMetaData(String sampId,
                            String instrId,
                            String dsName,
                            Map<String,String> mdMap,
                            List<File> filenames,
                            //String instrName,
                            String comments,
                            int projId,
                            int expId)
    {
        this.sampleId = sampId;
        this.instrumentId = instrId;
        this.datasetName = dsName;        
        /*this.cathode = cathode;
        this.anode = anode;
        this.electrolyteMedium = elecMed;
        this.refElectrode = refElec;
        this.filenames = filenames;
        this.medConc = medConc;*/
        //this.instrumentName = instrName;
        this.metaDataMap = mdMap;
        this.dataPopulated = true;
        this.coments = comments;
        this.projId = projId;
        this.expId = expId;
        this.filenames = filenames;
    }
    
    public String toJSONString()
    {
        JSONObject dataset = new JSONObject();
        JSONObject uploadData = new JSONObject();
        if(isDataPopulated())
        {
            try{
            /*uploadData.put("name", this.datasetName);
            uploadData.put("sample_id", Integer.toString(this.sampleId));
            uploadData.put("instrument_id",Integer.toString(this.instrumentId));*/
            
            JSONArray files = new JSONArray();
            int i=1;
            for(File file:this.filenames)
            {
                JSONObject jFile = new JSONObject();
                jFile.put("file_"+i,file.getName());
                files.put(jFile);
                i++;
            }
            uploadData.put("files", files);
            
            uploadData.put("name", this.datasetName);
            uploadData.put("instrument_id",this.instrumentId);
            uploadData.put("sample_id", this.sampleId);
            

            
            JSONObject metadata = new JSONObject();
            
            
            //metadata.put("Instrument Name", this.instrumentName);
            
            Iterator it =this.metaDataMap.entrySet().iterator();
            while(it.hasNext())
            {
                Map.Entry<String, String> pairs = (Map.Entry<String, String>)it.next();
                metadata.put(pairs.getKey(), pairs.getValue());
            }
            /*if(!this.electrolyteMedium.equals(""))
            {
                metadata.put("Electrolyte Medium", this.electrolyteMedium);
            }
            if(!this.medConc.equals(""))
            {
                metadata.put("Medium Concentration", this.medConc);
            }
            if(!this.refElectrode.equals(""))               
            {
                metadata.put("Reference Electrode", this.refElectrode);
            }
            if(!this.anode.equals(""))
            {
                metadata.put("Counter Electrode", this.anode);
            }
            
            if(!this.cathode.equals(""))
            {
                metadata.put("Working Electrode", this.cathode);
            }*/
            if(!this.coments.equals(""))
            {
                metadata.put("Comments", this.coments);
            }
            this.setMetaData(metadata);
            uploadData.put("metadata", metadata);
            
            //now we deal with the filenames
            
            dataset.put("dataset", uploadData);
            }catch(JSONException jse)
            {
                log.severe("Error while creating the json metadata");
                log.severe(jse.getMessage());
            }
        }
        else
        {
            //give a meaningfull message about how we should only really reach 
            //this place if we have populated the thing properly.
        }
        
        return dataset.toString();
    }

    public JSONObject getMetaData() {
        return metaData;
    }

    public void setMetaData(JSONObject metaData) {
        this.metaData = metaData;
    }

    public List<File> getFilenames() {
        return filenames;
    }

    
    public List<File> getFiles()
    {
        return this.filenames;
    }   
    public boolean isDataPopulated() {
        return dataPopulated;
    }

    public String getDatasetName() {
        return datasetName;
    }

   

    public String getInstrumentId() {
        return instrumentId;
    }

    /*public String getInstrumentName() {
        return instrumentName;
    }*/

   
    public String getSampleId() {
        return sampleId;
    }

    public String getComents() {
        return coments;
    }

    public void setComents(String coments) {
        this.coments = coments;
    }

    public int getExpId() {
        return expId;
    }

    public int getProjId() {
        return projId;
    }
    
    private boolean dataPopulated = false;   
    private String sampleId;
    private String instrumentId;
    private String datasetName;
    
    //private String instrumentName;
    /*private String electrolyteMedium;
    private String medConc;
    private String cathode;
    private String anode;
    private String refElectrode;  */
    private Map<String,String> metaDataMap;
    
    private List<File> filenames;
    private JSONObject metaData;
    private String coments;
    private int projId;
    private int expId;
}
