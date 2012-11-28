/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pat;

/**
 *
 * @author lordbhuman
 */
public class Sample {
    
    private String name;
    private int id;

    public Sample(String name,int id)
    {
        this.name = name;
        this.id = id;
    }
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public String toString()
    {
        return this.name;
    }
            
}
