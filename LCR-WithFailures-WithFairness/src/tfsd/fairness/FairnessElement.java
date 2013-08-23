/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package src.tfsd.fairness;

/**
 *
 * @author orcun
 */
public class FairnessElement {

    private int burstsize;
    private int received;
    private int sent;

    public FairnessElement(int b, int r, int s) {
        this.burstsize=b;
        this.received=r;
        this.sent=s;
        
    }

    /**
     * @return the burstsize
     */
    public int getBurstsize() {
        return burstsize;
    }

    /**
     * @param burstsize the burstsize to set
     */
    public void setBurstsize(int burstsize) {
        this.burstsize = burstsize;
    }

    /**
     * @return the received
     */
    public int getReceived() {
        return received;
    }

    /**
     * @param received the received to set
     */
    public void setReceived(int received) {
        this.received = received;
    }

    /**
     * @return the sent
     */
    public int getSent() {
        return sent;
    }

    /**
     * @param sent the sent to set
     */
    public void setSent(int sent) {
        this.sent = sent;
    }
}
