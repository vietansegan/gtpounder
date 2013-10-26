package util.govtrack;

import core.AbstractObject;
import java.util.ArrayList;
import java.util.HashMap;
import util.RankingItem;
import util.RankingItemList;

/**
 *
 * @author vietan
 */
public class GTDebate extends AbstractObject {

    private String title;
    private ArrayList<GTTurn> turns;
    private ArrayList<String> billsMentioned; // list of bill IDs mentioned
    private GTRoll associatedRoll;

    public GTDebate(String id) {
        super(id);
        this.turns = new ArrayList<GTTurn>();
        this.billsMentioned = new ArrayList<String>();
    }

    /**
     * For each debate, select a single bill that is associated with the debate.
     * If more than one bills are discussed in the debate, the bill that are
     * mentioned the most is returned. In case of ties, the bill that appears
     * earlier is chosen.
     *
     * This strategy is suggested by Thomas et. al. (EMNLP 06).
     *
     * @return The bill ID that is associated with this debate.
     */
    public String getBillAssociatedWith() {
        if (billsMentioned.isEmpty()) {
            return null;
        }
        HashMap<String, Integer> billFirstMentioned = new HashMap<String, Integer>();
        HashMap<String, Integer> billMentionedCounts = new HashMap<String, Integer>();
        for (int i = 0; i < billsMentioned.size(); i++) {
            String bm = billsMentioned.get(i);
            Integer count = billMentionedCounts.get(bm);
            if (count == null) {
                billMentionedCounts.put(bm, 1);
                billFirstMentioned.put(bm, i);
            } else {
                billMentionedCounts.put(bm, count + 1);
            }
        }

        RankingItemList<String> rankList = new RankingItemList<String>();
        for (String billDiscussed : billMentionedCounts.keySet()) {
            rankList.addRankingItem(new RankingItem<String>(
                    billDiscussed,
                    billMentionedCounts.get(billDiscussed),
                    -billFirstMentioned.get(billDiscussed)));
        }
        rankList.sortDescending();
        return rankList.getRankingItem(0).getObject();
    }

    public int getNumTurns() {
        return this.turns.size();
    }

    public GTRoll getAssociatedRoll() {
        return this.associatedRoll;
    }

    public void setAssociatedVote(GTRoll roll) {
        this.associatedRoll = roll;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append(this.getId()).append(":\t").append(title).append("\n");
        str.append(billsMentioned.toString()).append("\n");
        for (GTTurn turn : turns) {
            str.append("---").append(turn.toString()).append("\n");
        }
        return str.toString();
    }

    public void addBillMentioned(String billId) {
        this.billsMentioned.add(billId);
    }

    public ArrayList<String> getBillsMentioned() {
        return this.billsMentioned;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return this.title;
    }

    public void addTurn(GTTurn turn) {
        this.turns.add(turn);
    }

    public ArrayList<GTTurn> getTurns() {
        return this.turns;
    }

    public GTTurn getTurn(int idx) {
        return this.turns.get(idx);
    }
}
