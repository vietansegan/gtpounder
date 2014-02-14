package util.freedomworks;

import core.AbstractObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import util.IOUtils;

/**
 *
 * @author vietan
 */
public class FWYear extends AbstractObject<Integer> {

    public static final String SCORE_FILE = "speaker-scores.txt";
    public static final String VOTE_FILE = "votes.txt";
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

    public void inputVotes(File filepath) throws Exception {
        throw new RuntimeException("To be implemented");
    }

    public void outputVotes(File filepath) throws Exception {
        throw new RuntimeException("To be implemented");
    }

    public void inputLegislators(File filepath) throws Exception {
        System.out.println("Loading FreedomWorks legislators from " + filepath);

        BufferedReader reader = IOUtils.getBufferedReader(filepath);
        String line;
        String[] sline;
        while ((line = reader.readLine()) != null) {
            sline = line.split("\t");
            int lid = Integer.parseInt(sline[0]);
            String name = sline[1];
            String role = sline[2];
            int score = Integer.parseInt(sline[3]);

            FWLegislator legislator = new FWLegislator(lid);
            legislator.addProperty(FWLegislator.NAME, name);
            legislator.addProperty(FWLegislator.ROLE, role);
            this.putLegislator(lid, legislator);
            this.putLegislatorScore(lid, score);
        }
        reader.close();
    }

    public void outputLegislators(File filepath) throws Exception {
        BufferedWriter writer = IOUtils.getBufferedWriter(filepath);
        for (int lid : this.getLegislatorIDs()) {
            FWLegislator legislator = this.getLegislator(lid);
            int score = this.getLegislatorScore(lid);
            writer.write(lid
                    + "\t" + legislator.getProperty(FWLegislator.NAME)
                    + "\t" + legislator.getProperty(FWLegislator.ROLE)
                    + "\t" + score
                    + "\n");
        }
        writer.close();
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
