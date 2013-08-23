/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package src.tfsd.lcr;

import tfsd.*;
import net.sf.appia.core.*;
import net.sf.appia.core.events.SendableEvent;
import net.sf.appia.core.events.channel.ChannelInit;


import java.util.ArrayList;
import java.util.logging.Level;
import src.Logger;

/**
 *
 * @author orcun
 */
public class LCRSession extends Session {

    private ProcessSet processes;
    private int seqNumber;
    private int ackRecoveryCount = 0;
    private int endRecoveryCount = 0;
    private int[] vectorClock;
    private LCRStack lcrstack;
    private int myRank;
    //channel to send recovery event
    private Channel channel;
    SampleProcess[] processArray;
    private boolean[] procStatusArray;
    private boolean stopRbBroadCast = false;

    public LCRSession(Layer layer) {
        super(layer);
    }

    public void handle(Event event) {
        // Init events. Channel Init is from Appia and ProcessInitEvent is to know
        // the elements of the group
        if (event instanceof ChannelInit) {
            handleChannelInit((ChannelInit) event);
        } else if (event instanceof ProcessInitEvent) {
            handleProcessInitEvent((ProcessInitEvent) event);
        } else if (event instanceof SendableEvent) {
            if (event.getDir() == Direction.DOWN) // UPON event from the above protocol (or application)
            {
                rbBroadcast((SendableEvent) event);
            } else // UPON event from the bottom protocol (or perfect point2point links)
            {
                bebDeliver((SendableEvent) event);
            }
        } else if (event instanceof Crash) {
            handleCrash((Crash) event);
        }

    }

    /**
     * @param init
     */
    private void handleChannelInit(ChannelInit init) {
        try {
            init.go();

        } catch (AppiaEventException e) {
            e.printStackTrace();
        }
        channel = init.getChannel();
    }

    /**
     * @param event
     */
    @SuppressWarnings("unchecked")
    private void handleProcessInitEvent(ProcessInitEvent event) {
        processes = event.getProcessSet();
        processArray = this.processes.getAllProcesses();
        this.myRank = processes.getSelfRank();

        procStatusArray = new boolean[processArray.length];
        for (int i = 0; i < processArray.length; i++) {
            procStatusArray[i] = true;
        }


        try {
            event.go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }
        lcrstack = new LCRStack(processes.getSelfRank());
        this.vectorClock = new int[processes.getSize()];
    }

    /**
     * Called when the above protocol sends a message.
     *
     * @param event
     */
    private void rbBroadcast(SendableEvent event) {
        if (!stopRbBroadCast) {
            // first we take care of the header of the message
            SampleProcess self = processes.getSelfProcess();
            MessageID msgID = new MessageID(self.getProcessNumber(), seqNumber);
            seqNumber++;

            //incrementing the vector clock=I broadcast(saw) this message
            vectorClock[processes.getSelfRank()]++;
            Logger.log("LCR: broadcasting message.");

            String display = event.getMessage().popString();
            event.getMessage().pushString(display);
            event.getMessage().pushObject(msgID);
            int newVc[] = new int[processes.getSize()];
            System.arraycopy(vectorClock, 0, newVc, 0, processes.getSize());
            event.getMessage().pushObject(vectorClock);

            //adding to the pending list
            lcrstack.add(display, newVc, msgID, LCRStack.INIT);
            //Push the flag sayıng ıt has to sent to only successor
            event.getMessage().pushBoolean(true);
            //event.getMessage().pushString("FAIR_NO_FWD");
            // broadcast the message
            bebBroadcast(event);
        }

    }

    /**
     * Called when the lower protocol delivers a message.
     *
     * @param event
     */
    private void bebDeliver(SendableEvent event) {
        /*
         * The event wıll have message wıth followıng contents
         * Text
         * MessageId
         * VectorClock assoscıated wıth the message.
         */
        Logger.log("LCR: Received message from beb.");


        //cloning the event
        SendableEvent cloned = null;
        try {
            cloned = (SendableEvent) event.cloneEvent();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return;
        }

        if (event instanceof RecoveryEvent) {

            ArrayList<LCRElement> lcrlist = (ArrayList<LCRElement>) cloned.getMessage().popObject();
            for (LCRElement l : lcrlist) {
                int j = l.getProcess();
                String s = l.getUsermsg();
                MessageID id = l.getMsgID();
                int vc[] = l.getVectorClock();
                if (vc[j] > vectorClock[j]) {
                    l.setState(LCRStack.INIT);
                   // lcrstack.getList().add(l);
                    lcrstack.add(s, vc, id, LCRStack.INIT);
                  
                }
            }
            //sending Ackrecovery to the guy who sent recovery
            try {
                AckRecoveryEvent ackrecoveryEvent = new AckRecoveryEvent(channel, Direction.DOWN, this);


                //async or not ??
                int dest = cloned.getMessage().popInt();
                Logger.log("LCR:Receıved Recovery Event from proc " + dest);
                ackrecoveryEvent.getMessage().pushInt(dest);
                ackrecoveryEvent.go();
                Logger.log("LCR: Sendıng AckRecoveryEvent to dest=" + dest);
            } catch (AppiaEventException e) {
                e.printStackTrace();
            }
            Logger.log("LCR:Handlıng  Recovery Event Complete");
        } else if (event instanceof AckRecoveryEvent) {

            Logger.log("LCR:Receıved AckRecovery Event.AckRcvdCount=" + ackRecoveryCount + ".");
            ackRecoveryCount++;
            if (ackRecoveryCount >= processes.getCorrectSize()) {
                try {
                    EndRecoveryEvent endrecoveryEvent = new EndRecoveryEvent(channel, Direction.DOWN, this);
                    endrecoveryEvent.go();
                } catch (AppiaEventException e) {
                    e.printStackTrace();
                }
            }

            Logger.log("LCR:Handlıng  AckRecovery Event Complete .AckRcvdCount=" + ackRecoveryCount);
        } else if (event instanceof EndRecoveryEvent) {
            endRecoveryCount++;
            Logger.log("LCR:Receıved EndRecovery Event.endRecoveryCount=" + endRecoveryCount + ".");
            if (endRecoveryCount >= processes.getCorrectSize()) {
                forceDeliver();
                try {
                    stopRbBroadCast = false;
                    Logger.log("LCR: Sending Unblock event. ");
                    ReaderWakeEvent wakeEvent = new ReaderWakeEvent(channel, Direction.UP, this);
                    wakeEvent.go();
                } catch (AppiaEventException e) {
                    e.printStackTrace();
                }
            }
            Logger.log("LCR:Handlıng  EndRecovery Event Complete.");

        } else {
            int vcSource[] = (int[]) cloned.getMessage().popObject();
            MessageID msgID = (MessageID) cloned.getMessage().popObject();
            String str = cloned.getMessage().popString();


            int j = msgID.process;

            if (str.equalsIgnoreCase("ACK")) {
                Logger.log("LCR: Received an ack message");
                if (myRank != predecessor(predecessor(j))) {
                    lcrstack.update(msgID, LCRStack.STABLE);
                    event.getMessage().pushBoolean(true);
                    //event.getMessage().pushString("FAIR_FWD");
                    bebBroadcast(event);

                } else {
                    lcrstack.update(msgID, LCRStack.STABLE);
                }

                tryDeliver();
            } else {
                //be careful with pop && push
                //int[] syncVC = (int[]) event.getMessage().popObject();
                //MessageID mid = (MessageID) event.getMessage().popObject();
                //String display = event.getMessage().popString();
                // event.getMessage().pushString(display);
                if (vcSource[j] > vectorClock[j]) {

                    if (myRank != predecessor(j)) {
                        //order is different from (pending&&send) compare to pseudocode    
                        //adding to the pending list

                        lcrstack.add(str, vcSource, msgID, LCRStack.INIT);
                        //Push the flag sayıng ıt has to sent to only successor
                        event.getMessage().pushBoolean(true);
                       // event.getMessage().pushString("FAIR_FWD");
                        // broadcast the message
                        bebBroadcast(event);

                    } else {
                        //adding to the pending list
                        lcrstack.add(str, vcSource, msgID, LCRStack.STABLE);

                        cloned.getMessage().pushString("ACK");
                        cloned.getMessage().pushObject(msgID);
                        cloned.getMessage().pushObject(vcSource);
                        cloned.getMessage().pushBoolean(true);
                        //cloned.getMessage().pushString("FAIR_FWD");
                        bebBroadcast(cloned);

                        tryDeliver();
                    }
                    //I saw the message from process j
                    vectorClock[j]++;
                }
            }




        }
    }

    public int predecessor(int num) {
        
        int pred = (num - 1 + this.processes.getSize()) % this.processes.getSize();
        while (!processArray[pred].isCorrect()) {
            pred = (pred - 1 + this.processes.getSize()) % this.processes.getSize();
        }
        Logger.log("LCR: predecessor is " + pred);
        return pred;


    }

    /**
     * Called by this protocol to send a message to the lower protocol.
     *
     * @param event
     */
    private void bebBroadcast(SendableEvent event) {
        Logger.log("LCR: sending message to beb.");
        try {
            event.setDir(Direction.DOWN);
            event.setSourceSession(this);
            event.init();
            event.go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }
    }

    private void handleCrash(Crash crash) {



        int pi = crash.getCrashedProcess();
        Logger.log("LCR: Process " + pi + " failed.");
        procStatusArray[pi] = false;
        try {
            crash.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }
        //stopRbBroadCast = true;
        // changes the state of the process to "failed"
        processes.getProcess(pi).setCorrect(false);

        try {

            ReaderBlockEvent blockEvent = new ReaderBlockEvent(channel, Direction.UP, this);

            //ReaderBlockEvent blockEvent = new ReaderBlockEvent();

            blockEvent.init();
            blockEvent.go();
            Logger.log("LCR: initiated reader block event after detecting crash. ");

        } catch (AppiaEventException e) {
            e.printStackTrace();
        }

        
        try {
            Logger.log("LCR: initiated recovery event after detecting crash. ");
            RecoveryEvent recoveryEvent = new RecoveryEvent(channel, Direction.DOWN, this);
            recoveryEvent.getMessage().pushInt(myRank);
            Logger.log("trying to push pending list");
            recoveryEvent.getMessage().pushObject(lcrstack.getList());
            recoveryEvent.getMessage().pushObject(procStatusArray);
            //recoveryEvent.getMessage().pushBoolean(false);


            recoveryEvent.go();

        } catch (AppiaEventException e) {
            e.printStackTrace();
        }
    }

    private void tryDeliver() {

        ArrayList<LCRElement> ls = lcrstack.getList();
        while (!ls.isEmpty()) {
            if (ls.get(0).getState() == LCRStack.STABLE) {

                LCRElement le = ls.get(0);
                SampleSendableEvent toDeliver = null;
                try {
                    toDeliver = new SampleSendableEvent(channel, Direction.UP, this);
                } catch (AppiaEventException ex) {
                    java.util.logging.Logger.getLogger(LCRSession.class.getName()).log(Level.SEVERE, null, ex);
                }

                MessageID msgID = le.getMsgID();
                String s = le.getUsermsg();
                s = msgID.process + ":" + s;
                toDeliver.getMessage().pushString(s);
                try {
                    toDeliver.init();
                    toDeliver.go();
                    Logger.log("Delıverıng the message to the applıcatıon");
                } catch (AppiaEventException e) {
                    e.printStackTrace();
                }

                lcrstack.remove();


            } else {
                return;
            }
        }
    }
    //after receiving END-RECOVERY ACK

    private void forceDeliver() {
        Logger.log("LCR: Enterıng forceDelıver Method.");
        ArrayList<LCRElement> ls = lcrstack.getList();
        while (!ls.isEmpty()) {

            LCRElement le = ls.get(0);
            SampleSendableEvent toDeliver = null;
            try {
                toDeliver = new SampleSendableEvent(channel, Direction.UP, this);
            } catch (AppiaEventException ex) {
                java.util.logging.Logger.getLogger(LCRSession.class.getName()).log(Level.SEVERE, null, ex);
            }
            MessageID msgID = le.getMsgID();
            Logger.log("Forcing to deliver");
            String s = le.getUsermsg();
            s = msgID.process + ":" + s;
            toDeliver.getMessage().pushString(s);
            try {
                toDeliver.init();
                toDeliver.go();
                Logger.log("Delıverıng the message to the applıcatıon");
            } catch (AppiaEventException e) {
                e.printStackTrace();
            }

            //removes the first element from head of the pending list
            lcrstack.remove();
            //update local vector clock= I saw this message.
            vectorClock[msgID.process]++;

        }
        ackRecoveryCount = 0;
        endRecoveryCount = 0;
        Logger.log("LCR: Exıtıng forceDelıver Method.");
    }

}
