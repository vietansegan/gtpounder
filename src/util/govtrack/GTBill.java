package util.govtrack;

import core.AbstractObject;
import java.util.ArrayList;

/**
 *
 * @author vietan
 */
public class GTBill extends AbstractObject {
    public static final String MAJOR_TOPIC = "major";
    public static final String MINOR_TOPIC = "minor";

    private final String type;
    private final int number;
    private String title;
    private String summary;
    private String text;
    private String officialTitle;
    private ArrayList<String> debateIds;
    private ArrayList<String> rollIds;
    private ArrayList<String> subjects; // labels

    public GTBill(String type, int number) {
        super(type + "-" + number);
        this.type = type;
        this.number = number;
        this.subjects = new ArrayList<String>();
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public String getText() {
        return this.text;
    }

    public void addSubject(String s) {
        this.subjects.add(s);
    }

    public ArrayList<String> getSubjects() {
        return this.subjects;
    }

    public void setSubjects(ArrayList<String> subjects) {
        this.subjects = subjects;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append(this.getId()).append(":\t").append(title);
        return str.toString();
    }

    public void addRollId(String rid) {
        if (rollIds == null) {
            rollIds = new ArrayList<String>();
        }
        rollIds.add(rid);
    }

    public ArrayList<String> getRollIds() {
        return this.rollIds;
    }

    public void addDebateId(String sid) {
        if (debateIds == null) {
            debateIds = new ArrayList<String>();
        }
        debateIds.add(sid);
    }

    public ArrayList<String> getSpeechIds() {
        return this.debateIds;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setOfficialTitle(String officialTitle) {
        this.officialTitle = officialTitle;
    }

    public String getType() {
        return type;
    }

    public void setSummary(String s) {
        this.summary = s;
    }

    public String getSummary() {
        return this.summary;
    }

    public int getNumber() {
        return number;
    }

    public String getTitle() {
        return title;
    }

    public String getOfficialTitle() {
        return officialTitle;
    }
}
