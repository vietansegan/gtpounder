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
    public static final String KEYVOTE_FILE = "keyvotes.txt";
    public static final String VOTE_FILE = "votes.txt";
    public static final int NA_SCORE = -1;
    private final HashMap<Integer, FWLegislator> legislators;
    private HashMap<Integer, FWBill> bills; // key: key vote order
    private HashMap<Integer, FWBill> keyvotes; // key: roll-call number
    private ArrayList<Integer> keyRollCalls;
    private HashMap<Integer, ArrayList<FWVote>> votes;
    private final HashMap<Integer, Integer> legislatorScores;

    public FWYear(int year) {
        super(year);
        this.legislators = new HashMap<Integer, FWLegislator>();
        this.bills = new HashMap<Integer, FWBill>();
        this.votes = new HashMap<Integer, ArrayList<FWVote>>();
        this.legislatorScores = new HashMap<Integer, Integer>();
    }

    public HashMap<Integer, FWBill> getKeyVotes() {
        return this.keyvotes;
    }

    public ArrayList<Integer> getKeyRollCalls() {
        return this.keyRollCalls;
    }

    public FWBill getKeyVote(int rollcallNum) {
        return this.keyvotes.get(rollcallNum);
    }

    public void inputVotes(File filepath) throws Exception {
        System.out.println("Inputing votes from " + filepath);
        if (bills == null) {
            throw new RuntimeException("Bills not loaded");
        }
        if (legislators == null) {
            throw new RuntimeException("Legislators not loaded");
        }

        this.votes = new HashMap<Integer, ArrayList<FWVote>>();
        BufferedReader reader = IOUtils.getBufferedReader(filepath);
        String line;
        while ((line = reader.readLine()) != null) {
            String[] sline = line.split("\t");
            int lid = Integer.parseInt(sline[0]);

            ArrayList<FWVote> legVotes = new ArrayList<FWVote>();
            for (int ii = 1; ii < sline.length; ii++) {
                int bid = Integer.parseInt(sline[ii].split(":")[0]);
                String vt = sline[ii].split(":")[1];
                FWVote v = new FWVote(legislators.get(lid), bills.get(bid),
                        this.id, FWVote.getVoteType(vt.toLowerCase()));
                legVotes.add(v);
            }
            this.votes.put(lid, legVotes);
        }
        reader.close();
    }

    public void outputVotes(File filepath) throws Exception {
        System.out.println("Outputing votes to " + filepath);
        BufferedWriter writer = IOUtils.getBufferedWriter(filepath);
        for (int lid : votes.keySet()) {
            writer.write(Integer.toString(lid));
            ArrayList<FWVote> lVotes = votes.get(lid);
            for (FWVote v : lVotes) {
                FWLegislator leg = v.getLegislator();
                if (leg.getId() != lid) {
                    throw new RuntimeException("ID mismatch");
                }
                writer.write("\t" + v.getBill().getId() + ":" + v.getType());
            }
            writer.write("\n");
        }
        writer.close();
    }

    public void inputKeyVotes(File filepath) throws Exception {
        System.out.println("Inputing key votes from " + filepath);
        BufferedReader reader = IOUtils.getBufferedReader(filepath);
        String line;
        reader.readLine();
        this.keyRollCalls = new ArrayList<Integer>();
        this.bills = new HashMap<Integer, FWBill>();
        this.keyvotes = new HashMap<Integer, FWBill>();
        while ((line = reader.readLine()) != null) {
            String[] sline = line.split("\t");

            int bid = Integer.parseInt(sline[0]);
            int rollcall = Integer.parseInt(sline[1]);
            FWBill bill = new FWBill(bid);
            bill.addProperty(FWBill.ROLL_CALL, sline[1]);
            bill.addProperty(FWBill.BILL, sline[2]);
            bill.addProperty(FWBill.FW_VOTE_PREFERRED, sline[3]);
            bill.addProperty(FWBill.TITLE, sline[4]);
            if (sline.length > 5) {
                bill.addProperty(FWBill.SUMMARY, sline[5]);
            } else {
                bill.addProperty(FWBill.SUMMARY, "");
            }
            this.bills.put(bid, bill);
            this.keyvotes.put(rollcall, bill);
            this.keyRollCalls.add(rollcall);
        }
        reader.close();
    }

    public void outputKeyVotes(File filepath) throws Exception {
        System.out.println("Outputing key votes to " + filepath);
        BufferedWriter writer = IOUtils.getBufferedWriter(filepath);
        writer.write("ID\tRollCall\tBill\tVotePref\tTitle\tSummary\n"); // header
        for (FWBill bill : this.bills.values()) {
            int rollcall = Integer.parseInt(bill.getProperty(FWBill.ROLL_CALL).replaceAll("Roll Call ", "").trim());
            String billStr = bill.getProperty(FWBill.BILL);
            String votePref = bill.getProperty(FWBill.FW_VOTE_PREFERRED);
            String title = bill.getProperty(FWBill.TITLE);
            String summary = bill.getProperty(FWBill.SUMMARY);
            if (summary != null) {
                summary = summary.replaceAll("\n", " ");
            } else {
                summary = "";
            }
            writer.write(bill.getId()
                    + "\t" + rollcall
                    + "\t" + billStr
                    + "\t" + votePref
                    + "\t" + title
                    + "\t" + summary + "\n");
        }
        writer.close();
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

    public void inputRepublicans(File filepath) throws Exception {
        System.out.println("Loading FreedomWorks Republicans from " + filepath);

        BufferedReader reader = IOUtils.getBufferedReader(filepath);
        String line;
        String[] sline;
        while ((line = reader.readLine()) != null) {
            sline = line.split("\t");
            int lid = Integer.parseInt(sline[0]);
            String name = sline[1];
            String role = sline[2];
            if (!role.endsWith("R")) {
                continue;
            }
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
        System.out.println("Outputing legislators to " + filepath);
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

    public HashMap<Integer, ArrayList<FWVote>> getVotes() {
        return this.votes;
    }

    public ArrayList<FWVote> getVotes(int lid) {
        return this.votes.get(lid);
    }

    public void addVote(int lid, FWVote vote) {
        ArrayList<FWVote> vs = this.votes.get(lid);
        if (vs == null) {
            vs = new ArrayList<FWVote>();
        }
        vs.add(vote);
        this.votes.put(lid, vs);
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
