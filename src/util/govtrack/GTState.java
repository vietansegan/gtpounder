package util.govtrack;

import core.AbstractObject;

/**
 *
 * @author vietan
 */
public class GTState extends AbstractObject {

    private int statecode; // 2 digit ICPSR State Code
    private String statename;

    public GTState(String id) {
        super(id);
    }

    public int getStatecode() {
        return statecode;
    }

    public void setStatecode(int statecode) {
        this.statecode = statecode;
    }

    public String getStatename() {
        return statename;
    }

    public void setStatename(String statename) {
        this.statename = statename;
    }

    @Override
    public String toString() {
        return this.id + " (" + this.statecode + ", " + this.statename + ")";
    }
}
