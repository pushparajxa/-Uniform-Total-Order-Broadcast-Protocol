/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package src.tfsd.fairness;

import tfsd.*;
import net.sf.appia.core.*;
import net.sf.appia.core.events.channel.ChannelInit;


import java.util.ArrayList;
import java.util.HashMap;
import net.sf.appia.core.events.SendableEvent;
import src.Logger;
import src.tfsd.lcr.AckRecoveryEvent;
import src.tfsd.lcr.EndRecoveryEvent;
import src.tfsd.lcr.ReaderBlockEvent;
import src.tfsd.lcr.RecoveryEvent;


public class FairnessSession extends Session {

    private Channel channel;
    private ProcessSet processes;
    private SampleProcess self;
    private int myRank;
    //sendqueue
    private ArrayList<SendableEvent> sendQ = new ArrayList<SendableEvent>();
    //localqueue
    private ArrayList<EncapEvent> forward = new ArrayList<EncapEvent>();
    //burst size
    private int burst;
    //table for ensuring fairness
    private ArrayList<FairnessElement> table = new ArrayList<FairnessElement>();
    private Object lock;
    private boolean piggyBack = false;

    /**
     * Constructor of the Session.
     *
     * @param layer parent layer.
     */
    public FairnessSession(Layer layer) {
        super(layer);


    }

    public void handle(Event event) {

        if (event instanceof ChannelInit) {
            handleChannelInit((ChannelInit) event);
        } else if (event instanceof ProcessInitEvent) {
            handleProcessInit((ProcessInitEvent) event);
        } else if (event instanceof SendableEvent) {
            if (event.getDir() == Direction.DOWN) {
                handleLocal((SendableEvent) event);
            } else {
                handleStranger((SendableEvent) event);
            }
        } else if (event instanceof Crash) {
            handleCrash((Crash) event);
        }

    }

    /**
     * @param init
     */
    private void handleChannelInit(ChannelInit init) {
        channel = init.getChannel();
        try {
            init.go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param event
     */
    private void handleProcessInit(ProcessInitEvent event) {
        processes = event.getProcessSet();
        self = processes.getSelfProcess();
        lock = new Object();//USed for lockıng.
        myRank = processes.getSelfRank();
        
        try {
            event.go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }
    }

    private void handleLocal(SendableEvent event) {
        //do nothing for the events related with Recovery
        if (event instanceof RecoveryEvent || event instanceof AckRecoveryEvent || event instanceof EndRecoveryEvent) {
            try {
                event.go();
            } catch (AppiaEventException e) {
                e.printStackTrace();
            }
            return;
        }

        String msg = (String) event.getMessage().popString();

        if (msg.equalsIgnoreCase("FAIR_NO_FWD")) {
            sendQ.add(event);
            burst = sendQ.size();
            if (burst >= 2) {
                synchronized (lock) {
                    piggyBack = true;
                }
            }
            
            int procNum;
        boolean doPgBck = false;
        
        //sforward.add(new EncapEvent(event, msgId.process,str,msgId,vc));
        //iterate in the forward queue
        while (!forward.isEmpty()) {
            //reach to msg ID so to process which sends that
            //forward.get(i).
            EncapEvent ece = forward.get(0);
            procNum = ece.procNum;

            if (table.get(procNum).getReceived() < table.get(procNum).getSent()) //forward it
            {
                synchronized (lock) {
                    if (piggyBack) {
                        doPgBck = true;
                        piggyBack = false;

                    } else {
                        //do nothing.
                    }

                }

                if (doPgBck) {
                    try {

                        PiggyBackEvent pgbEvent = new PiggyBackEvent(channel, Direction.UP, this);
                        pgbEvent.getMessage().pushString(ece.str);
                        pgbEvent.getMessage().pushObject(ece.msgId);
                        pgbEvent.getMessage().pushObject(ece.vc);
                        pgbEvent.getMessage().pushInt(burst);
                        pgbEvent.getMessage().pushInt(myRank);
                        
                        
                        
                        pgbEvent.init();
                        pgbEvent.go();
                        Logger.log("FAIRNESS: Created PıggyBack Event. Sendıng it.");

                    } catch (AppiaEventException e) {
                        e.printStackTrace();
                    }
                    doPgBck = false;
                }

                forward.remove(0);

            }
        }
            
            
            

        } else {
            //It wıll have FAİR_FWD.ACK message and Forwardıng the broadcast sent by predecessors.
            try {
                event.go();
            } catch (AppiaEventException e) {
                e.printStackTrace();
            }
        }


    }

    private void handleStranger(SendableEvent event) {
        MessageID msgId;
        int vc[];
        String str;
        //do nothing for the events related with Recovery
        if (event instanceof RecoveryEvent || event instanceof AckRecoveryEvent || event instanceof EndRecoveryEvent) {
            try {
                event.go();
            } catch (AppiaEventException e) {
                e.printStackTrace();
            }
            return;
        } else {
            //Now the event may be ACK or FwdBroadCast Event.
            SendableEvent cloned = null;
            try {
                cloned = (SendableEvent) event.cloneEvent();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
                return;
            }
            vc = (int[]) cloned.getMessage().popObject();//POP Vector Clock
            msgId = (MessageID) cloned.getMessage().popObject();//POP Message ID
            str = cloned.getMessage().popString();//POP Message Strıng

            if (str.equalsIgnoreCase("ACK")) {
                try {
                    event.go();
                } catch (AppiaEventException e) {
                    e.printStackTrace();
                }
                return;
            } else {
                //Thıs ıs a 
            }

        }

        
    }

    private void handleCrash(Crash crash) {
        //same here??
        int pi = crash.getCrashedProcess();
        Logger.log("Fairness:Process " + pi + " failed.");

        try {
            crash.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }

        // should we change also here??
        //processes.getProcess(pi).setCorrect(false);

    }
}

class EncapEvent {

    SendableEvent se;
    int procNum;
    MessageID msgId;
    int vc[];
    String str;

    public EncapEvent(SendableEvent se, int procNum,String str,MessageID mid,int []vc) {
        this.se = se;
        this.procNum = procNum;
        this.str= str;
        this.msgId = mid;
        this.vc = vc;

    }
}
