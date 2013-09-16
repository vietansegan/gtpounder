package util.govtrack;

import core.AbstractObject;

/**
 *
 * @author vietan
 */
public class GTTurn extends AbstractObject {

    private String speakerId;
    private String text;

    public GTTurn(String id) {
        super(id);
    }

    public GTTurn(String id, String sid, String t) {
        super(id);
        this.speakerId = sid;
        this.text = t;
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
