/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pat;

/**
 *
 * @author CptDanko
 */
public class Instrument 
{
    private int id;
    private String name;
    private String identifier;
    
    
    public Instrument(int id,String nme)
    {
        this.id = id;
        this.name = nme;       
    }
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    @Override
    public String toString()
    {
        return this.getName().trim();
    }
}
