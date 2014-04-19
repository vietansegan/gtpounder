package util.freedomworks;

import core.AbstractObject;

/**
 *
 * @author vietan
 */
public class FWBill extends AbstractObject<Integer> {

    public static final String FW_VOTE_PREFERRED = "vote-preferred";
    public static final String TITLE = "title";
    public static final String ROLL_CALL = "roll-call";
    public static final String BILL = "bill";
    public static final String SUMMARY = "summary";

    public FWBill(Integer id) {
        super(id);
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("Roll-call:\t").append(getProperty(ROLL_CALL)).append("\n");
        str.append("Bill:\t").append(getProperty(BILL)).append("\n");
        str.append("Vote-pref:\t").append(getProperty(FW_VOTE_PREFERRED)).append("\n");
        str.append("Title:\t").append(getProperty(TITLE)).append("\n");
        str.append("Summary:\t").append(getProperty(SUMMARY)).append("\n");
        return str.toString();
    }
}
