package util.freedomworks;

/**
 *
 * @author vietan
 */
public class FWVote {

    public static enum VoteType {

        WITH, AGAINST, NONE, INELIGIBLE
    };
    private FWLegislator legislator;
    private FWBill bill;
    private int year;
    private VoteType type;

    public FWVote(FWLegislator legislator, FWBill bill, int year, VoteType type) {
        this.legislator = legislator;
        this.bill = bill;
        this.year = year;
        this.type = type;
    }

    public FWLegislator getLegislator() {
        return legislator;
    }

    public void setLegislator(FWLegislator legislator) {
        this.legislator = legislator;
    }

    public FWBill getBill() {
        return bill;
    }

    public void setBill(FWBill bill) {
        this.bill = bill;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public VoteType getType() {
        return type;
    }

    public void setType(VoteType type) {
        this.type = type;
    }

    public static VoteType getVoteType(String vt) {
        if (vt.equals("with")) {
            return VoteType.WITH;
        } else if (vt.equals("against")) {
            return VoteType.AGAINST;
        } else if (vt.equals("none")) {
            return VoteType.NONE;
        } else if (vt.equals("ineligible")) {
            return VoteType.INELIGIBLE;
        } else {
            throw new RuntimeException("Vote type " + vt + " not supported");
        }
    }
}
