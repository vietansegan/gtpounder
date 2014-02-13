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
}
