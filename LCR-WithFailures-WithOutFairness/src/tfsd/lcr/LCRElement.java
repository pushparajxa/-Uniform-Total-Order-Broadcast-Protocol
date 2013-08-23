/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package src.tfsd.lcr;

import java.io.Serializable;
import java.util.Arrays;
//import java.util.Objects;
import net.sf.appia.core.events.SendableEvent;
import tfsd.MessageID;
import tfsd.SampleSendableEvent;

/**
 *
 * @author orcun
 */
class LCRElement implements  Serializable {
    private static final long serialVersionUID = 123456643849L;
    private MessageID msgID;
    private int [] vectorClock;
    private int state;
    private int processNo;
    private String usermsg;
    //this cannot be serialized so we should change the constructor
    //I just added hashcode && equals and serialversionUID
    //private SendableEvent event;
    
   /* public LCRElement(SendableEvent e,int flag){
    
        try {
             SendableEvent se = (SendableEvent)e.cloneEvent();
             this.vectorClock = (int [])se.getMessage().popObject();
             this.msgID = (MessageID)se.getMessage().popObject();
             this.processNo=msgID.process;
             this.state = flag;
             
             SendableEvent se2 = (SendableEvent)e.cloneEvent();
             this.event = se2;
        } catch (Exception e1) {
        }
   
    
    }*/
        public LCRElement(String s,int [] vc,MessageID id,int flag){
             this.usermsg=s;
             this.vectorClock = vc;
             this.msgID = id;
             this.processNo=msgID.process;
             this.state = flag;

    }
    public String printVectorClock(){
        String vc = "VC:[";
       
        for (int i: vectorClock) {
            vc = vc+i+",";
        }
        vc = vc +"]";
        return vc;
    }

    /**
     * @return the msgID
     */
    public MessageID getMsgID() {
        return msgID;
    }

    /**
     * @return the vectorClock
     */
    public int[] getVectorClock() {
        return vectorClock;
    }

    /**
     * @return the state
     */
    public int getState() {
        return state;
    }
      public int getProcess() {
        return processNo;
    }

    /**
     * @param state the state to set
     */
    public void setState(int state) {
        this.state = state;
    }

    /**
     * @return the event
     */
   /* public SendableEvent getEvent() {
        return event;
    }*/

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		//result = prime * result + ((event == null) ? 0 : event.hashCode());
		result = prime * result + ((msgID == null) ? 0 : msgID.hashCode());
		result = prime * result + (this.state);
                result = prime * result + (this.processNo);
		result = prime * result + ((vectorClock == null) ? 0 : vectorClock.hashCode());
		return result;
	}

    @Override
   /* public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final LCRElement other = (LCRElement) obj;
        /*if (!Objects.equals(this.msgID, other.msgID)) {
            return false;
        }
        if (!Arrays.equals(this.vectorClock, other.vectorClock)) {
            return false;
        }
        if (this.state != other.state) {
            return false;
        }
        if (this.processNo != other.processNo) {
            return false;
        }
       / if (!Objects.equals(this.event, other.event)) {
            return false;
        }
        return true;
    }*/

    	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LCRElement other = (LCRElement) obj;
		/*if (event == null) {
			if (other.event != null)
				return false;
		} else if (!event.equals(other.event))
			return false;*/
		if (msgID == null) {
			if (other.msgID != null)
				return false;
		} else if (!msgID.equals(other.msgID))
			return false;
		if (state != other.state)
			return false;
		if (vectorClock == null) {
			if (other.vectorClock != null)
				return false;
		} else if (!vectorClock.equals(other.vectorClock))
			return false;
		return true;
	}

    /**
     * @return the usermsg
     */
    public String getUsermsg() {
        return usermsg;
    }

    /**
     * @param usermsg the usermsg to set
     */
    public void setUsermsg(String usermsg) {
        this.usermsg = usermsg;
    }
    
}
