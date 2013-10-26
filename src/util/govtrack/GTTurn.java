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
public class GTTurn extends AbstractObject {

    private String speakerId;
    private String text;
    private ArrayList<String> billsMentioned;
    private String mainBillMentioned;

    public GTTurn(String id) {
        super(id);
    }

    public GTTurn(String id, String sid, String t) {
        super(id);
        this.speakerId = sid;
        this.text = t;
    }

    public void setMainBillMentioned(String b) {
        this.mainBillMentioned = b;
    }

    public String getMainBillMentioned() {
        return this.mainBillMentioned;
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

    public void addBillsMentioned(ArrayList<String> bm) {
        for (String b : bm) {
            this.billsMentioned.add(b);
        }
    }

    public void setBillsMentioned(ArrayList<String> bm) {
        this.billsMentioned = bm;
    }

    public ArrayList<String> getBillsMentioned() {
        return this.billsMentioned;
    }

    public String getSpeakerId() {
        return speakerId;
    }

    public void setSpeakerId(String speakerId) {
        this.speakerId = speakerId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return this.id + "\t" + this.speakerId + "\t" + this.text;
    }
}
