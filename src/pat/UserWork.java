/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pat;

import java.util.List;
/*
 * The purpose of this class is to represent the hirerarcy of the data
 * Author: Bhuman Soni
 * Email: bhumansoni@hotmail.com
 */
public class UserWork {
    abstract class Common{
        int id;
        String name;

        public String getName() {
            return name;
        }
        public void setName(String name)
        {
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }
        @Override
        public String toString()
        {
            return this.getName();
        }
    }
    
    class Dataset{
        String [] dataItems;

        public String[] getDataItems() {
            return dataItems;
        }

        public void setDataItems(String[] dataItems) {
            this.dataItems = dataItems;
        }
        
    }
    
    class Sample extends Common{        
        //int id;
        Dataset dataset;

        public Dataset getDataset() {
            return dataset;
        }
        /*public int getId() {
            return id;
        }*/
        public void setDataset(Dataset dataset) {
            this.dataset = dataset;
        }
        /*public void setId(int id) {
            this.id = id;
        }*/        
    }
    class Experiment extends Common{        
        List<Sample> samples;

        public void setSamples(List<Sample> samples) {
            this.samples = samples;
        }

        public List<Sample> getSamples() {
            return samples;
        }       
    }
    
    class Project extends Common
    {
        List<Experiment> experiments;

        public List<Experiment> getExperiments() {
            return experiments;
        }

        public void setExperiments(List<Experiment> experiments) {
            this.experiments = experiments;
        }      
    }

    public List<Project> getProjects() {
        return projects;
    }
    public String getRepresentationType() {
        return representationType;
    }
    public void setProjects(List<Project> projects) {
        this.projects = projects;
    }
    public void setRepresentationType(String representationType) {
        this.representationType = representationType;
    }
    
    public UserWork(PROJECT_RELATION relation)
    {
        if(relation.equals(PROJECT_RELATION.OWNER))
        {
            this.representationType ="owner";
        }
        else
        {
            this.representationType="collaborator";
        }
    }
    
    enum PROJECT_RELATION{OWNER,COLLABORATOR};
    String representationType;
    List<Project> projects;    
}
