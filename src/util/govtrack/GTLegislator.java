package util.govtrack;

import core.AbstractObject;
import java.util.ArrayList;

/**
 *
 * @author vietan
 */
public class GTLegislator extends AbstractObject {

    public static final String REP = "rep";
    public static final String SEN = "sen";
    public static final String NAME = "name";
    public static final String ICPSRID = "icpsrid";
    public static final String CONVOTEID = "convoteid";
    private String lastname;
    private String firstname;
    private String middlename;
    private String party;
    private String state;
    private int district;
    private String type; // rep or sen
    private ArrayList<String> debateIds;

    public GTLegislator(String id, String lname, String fname, String mname,
            String type,
            String party, String state) {
        super(id);
        this.lastname = lname;
        this.firstname = fname;
        this.middlename = mname;
        this.type = type;
        this.party = party;
        this.state = state;
    }

    public GTLegislator(String id, String lname, String fname, String mname,
            String party, String state, int district) {
        super(id);
        this.lastname = lname;
        this.firstname = fname;
        this.middlename = mname;
        this.party = party;
        this.state = state;
        this.district = district;
    }

    public void addDebateId(String sid) {
        if (debateIds == null) {
            debateIds = new ArrayList<String>();
        }
        debateIds.add(sid);
    }

    public ArrayList<String> getDebateIds() {
        return this.debateIds;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        if (this.properties.containsKey(NAME)) {
            return this.properties.get(NAME);
        }
        return this.firstname + " " + (this.middlename == null ? "" : (this.middlename + " ")) + this.lastname;
    }

    public String getLastname() {
        return lastname;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getMiddlename() {
        return middlename;
    }

    public String getParty() {
        return party;
    }

    public String getState() {
        return state;
    }

    public int getDistrict() {
        return this.district;
    }

    public void setDistrict(int district) {
        this.district = district;
    }

    @Override
    public String toString() {
        return this.getName()
                + "\t(" + this.id
                + ", " + this.type
                + ", " + this.party
                + ", " + this.state + "-" + this.district
                + ")";
    }
}
