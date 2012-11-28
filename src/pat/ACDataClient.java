/*
 * Author: Bhuman Soni
 * Email: bhumansoni@hotmail.com
 */
package pat;

/**
 *
 * @author lordbhuman
 */
public interface ACDataClient {
    //some definition of samples
    
    public void initializeClient();    
    public void initializeSamples();
    public boolean initializeUser(String username,String password);
    public void uploadExperimentData(PatComponent uploadDataComponent,boolean sendEmailNotication);
    //public void initializeInstruments()throws org.codehaus.jettison.json.JSONException;
    
    
}
