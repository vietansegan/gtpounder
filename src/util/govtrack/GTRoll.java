package util.govtrack;

import core.AbstractObject;
import java.util.HashMap;

/**
 *
 * @author vietan
 */
public class GTRoll extends AbstractObject<String> {

    public static final String YEA = "+";
    public static final String NAY = "-";
    public static final String NOTVOTING = "0";
    private String where;
    private int roll;
    private String title;
    private long date;
    private String billId;
    private HashMap<String, String> votes;
    private int numYea;
    private int numNay;
    private int numNotVoting;

    public GTRoll(String id) {
        super(id);
        this.votes = new HashMap<String, String>();
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append(getId()).append("\n");
        str.append("where: ").append(where)
                .append(". category: ").append(getProperty("category"))
                .append(". result: ").append(getProperty("result"))
                .append("\n");
        str.append("title: ").append(title).append("\n");
        str.append("# votes: ").append(votes.size())
                .append(" (").append(numYea)
                .append(":").append(numNay)
                .append(":").append(numNotVoting)
                .append(")");
        return str.toString();
    }

    public void putVote(String pid, String v) {
        this.votes.put(pid, v);
        if (v.equals("+")) {
            numYea++;
        } else if (v.equals("-")) {
            numNay++;
        } else {
            numNotVoting++;
        }
    }

    public void setDate(long d) {
        this.date = d;
    }

    public long getDate() {
        return this.date;
    }

    public int getNumYea() {
        return numYea;
    }

    public int getNumNay() {
        return numNay;
    }

    public int getNumNotVoting() {
        return numNotVoting;
    }

    public String getVote(String pid) {
        return this.votes.get(pid);
    }

    public HashMap<String, String> getVotes() {
        return this.votes;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getWhere() {
        return where;
    }

    public void setWhere(String where) {
        this.where = where;
    }

    public int getRoll() {
        return roll;
    }

    public void setRoll(int roll) {
        this.roll = roll;
    }

    public String getBillId() {
        return billId;
    }

    public void setBillId(String billId) {
        this.billId = billId;
    }
}
