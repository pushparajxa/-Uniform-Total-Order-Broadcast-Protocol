/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package src.tfsd.lcr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
//import java.util.Objects;
import net.sf.appia.core.events.SendableEvent;
import src.Logger;
import tfsd.MessageID;

/**
 *
 * @author orcun
 */
public class LCRStack implements Serializable, Comparator<LCRElement> {
    private static final long serialVersionUID = 12345L;
    public static final int STABLE = 1;
    public static final int INIT = 0;
    private ArrayList<LCRElement> list = new ArrayList<LCRElement>();
    private int rank;

    //SendableEvent cloned = null;
    @Override

    	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((list == null) ? 0 : list.hashCode());
		//result = prime * result + ((msgID == null) ? 0 : msgID.hashCode());
		result = prime * result + (this.rank);
                
		return result;
	}

    @Override
        	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LCRStack other = (LCRStack) obj;
		if (list == null) {
			if (other.list != null)
				return false;
		} else if (!list.equals(other.list))
			return false;
		
		if (rank != other.rank)
			return false;
		
		return true;
	}


    public LCRStack(int rank) {
        this.rank = rank;
    }

   /* public void add(SendableEvent e, int flag) {
        getList().add(new LCRElement(e, flag));
        Logger.log("Before sorting:");
        for (LCRElement l : getList()) {
            Logger.log("message ID" + l.getMsgID().toString() + "state" + l.getState() + " " + l.printVectorClock());
        }
        Logger.log("Before sortıng complete");
        Collections.sort(getList(), this);
        
        Logger.log("After sorting:");
        for (LCRElement l : getList()) {
            Logger.log("message ID" + l.getMsgID().toString() + "state" + l.getState() + " " + l.printVectorClock());
        }

    }
*/
     public void add(String s,int []vc,MessageID id, int flag) {
        getList().add(new LCRElement(s,vc,id,flag));
        Logger.log("Before sorting:");
        for (LCRElement l : getList()) {
            Logger.log("message ID" + l.getMsgID().toString() + "state" + l.getState() + " " + l.printVectorClock());
        }
        Logger.log("Before sortıng complete");
        Collections.sort(getList(), this);
        
        Logger.log("After sorting:");
        for (LCRElement l : getList()) {
            Logger.log("message ID" + l.getMsgID().toString() + "state" + l.getState() + " " + l.printVectorClock());
        }

    }
    public void remove() {
        getList().remove(0);
    }

    /*public void update(SendableEvent e, int flag) {
        SendableEvent cloned = null;
        try {
            cloned = (SendableEvent) e.cloneEvent();
        } catch (Exception e1) {
        }
        cloned.getMessage().popObject();
        MessageID msgID = (MessageID) cloned.getMessage().popObject();
        for (LCRElement l : getList()) {
            if (l.getMsgID().equals(msgID)) {
                l.setState(flag);
                Logger.log("Flag is set to" + flag + "for the message" + msgID);
            }
        }
    }*/
      public void update(MessageID id, int flag) {

        for (LCRElement l : getList()) {
            if (l.getMsgID().equals(id)) {
                l.setState(flag);
                Logger.log("Flag is set to" + flag + "for the message" + id);
            }
        }
    }

    @Override
    public int compare(LCRElement e1, LCRElement e2) {
        int p1 = e1.getProcess();
        int p2 = e2.getProcess();
        int i, j;
        int[] vc1 = e1.getVectorClock();
        int[] vc2 = e2.getVectorClock();

        if (p1 < p2) {
            i = p1;
            j = p2;

            if (vc1[i] <= vc2[i]) {
                return -1;
            } else {
                return 1;
            }

        } else if (p1 == p2) {
            i = p1;
            j = p2;
            if (vc1[i] < vc2[i]) {
                return -1;
            } else {
                return 1;
            }
        } else {
            //p1>p2..that ıs p2<p1
            if (vc2[p2] <= vc1[p2]) {
                return 1;
            } else {
                return -1;
            }


        }



    }

    /**
     * @return the list
     */
    public ArrayList<LCRElement> getList() {
        return list;
    }
}
