package util.freedomworks;

import core.AbstractObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 *
 * @author vietan
 */
public class FWYear extends AbstractObject<Integer> {

    public static final int NA_SCORE = -1;
    private HashMap<Integer, FWLegislator> legislators;
    private HashMap<Integer, FWBill> bills;
    private ArrayList<FWVote> votes;
    private HashMap<Integer, Integer> legislatorScores;

    public FWYear(int year) {
        super(year);
        this.legislators = new HashMap<Integer, FWLegislator>();
        this.bills = new HashMap<Integer, FWBill>();
        this.votes = new ArrayList<FWVote>();
        this.legislatorScores = new HashMap<Integer, Integer>();
    }

    public HashMap<Integer, Integer> getLegislatorScores() {
        return this.legislatorScores;
    }

    public void putLegislatorScore(int lid, int score) {
        this.legislatorScores.put(lid, score);
    }

    public HashMap<Integer, FWLegislator> getLegislators() {
        return this.legislators;
    }

    public Set<Integer> getLegislatorIDs() {
        return this.legislators.keySet();
    }

    public int getLegislatorScore(int lid) {
        return this.legislatorScores.get(lid);
    }

    public ArrayList<FWVote> getVotes() {
        return this.votes;
    }

    public void addVote(FWVote vote) {
        this.votes.add(vote);
    }

    public HashMap<Integer, FWBill> getBills() {
        return this.bills;
    }

    public void putLegislator(int lid, FWLegislator legislator) {
        this.legislators.put(lid, legislator);
    }

    public FWLegislator getLegislator(int lid) {
        return this.legislators.get(lid);
    }

    public void putBill(int bid, FWBill bill) {
        this.bills.put(bid, bill);
    }

    public FWBill getBill(int bid) {
        return this.bills.get(bid);
    }
}
