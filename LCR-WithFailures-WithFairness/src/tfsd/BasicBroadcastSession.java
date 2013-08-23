/*
 *
 * Hands-On code of the book Introduction to Reliable Distributed Programming
 * by Christian Cachin, Rachid Guerraoui and Luis Rodrigues
 * Copyright (C) 2005-2011 Luis Rodrigues
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 *
 * Contact
 * 	Address:
 *		Rua Alves Redol 9, Office 605
 *		1000-029 Lisboa
 *		PORTUGAL
 * 	Email:
 * 		ler@ist.utl.pt
 * 	Web:
 *		http://homepages.gsd.inesc-id.pt/~ler/
 * 
 */
package tfsd;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.SendableEvent;
import net.sf.appia.core.events.channel.ChannelInit;
import src.Logger;
import src.tfsd.lcr.AckRecoveryEvent;
import src.tfsd.lcr.EndRecoveryEvent;
import src.tfsd.lcr.RecoveryEvent;
import tfsd.ProcessInitEvent;
import tfsd.ProcessSet;
import tfsd.SampleProcess;
import tfsd.pfd.PFDStartEvent;

/**
 * Session implementing the Basic Broadcast protocol.
 *
 * @author nuno
 *
 */
public class BasicBroadcastSession extends Session {

    /*
     * State of the protocol: the set of processes in the group
     */
    private ProcessSet processes;
    private int myRank;
    private int numProcs;
    private boolean [] procStatusArray;
    /**
     * Builds a new BEBSession.
     *
     * @param layer
     */
    public BasicBroadcastSession(Layer layer) {
        super(layer);
    }

    /**
     * Handles incoming events.
     *
     * @see appia.Session#handle(appia.Event)
     */
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
                bebBroadcast((SendableEvent) event);
            } else // UPON event from the bottom protocol (or perfect point2point links)
            {
                pp2pDeliver((SendableEvent) event);
            }
        }
    }

    /**
     * Gets the process set and forwards the event to other layers.
     *
     * @param event
     */
    private void handleProcessInitEvent(ProcessInitEvent event) {
        processes = event.getProcessSet();
        this.myRank = processes.getSelfRank();
        this.numProcs = processes.getSize();

        procStatusArray = new boolean[numProcs];
         for(int i=0;i<numProcs;i++){
             procStatusArray[i] = true;
         }
         
        
        try {
            event.go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles the first event that arrives to the protocol session. In this
     * case, just forwards it.
     *
     * @param init
     */
    private void handleChannelInit(ChannelInit init) {
        try {
            init.go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }
    }

    /**
     * Broadcasts a message.
     *
     * @param event
     */
    private void bebBroadcast(SendableEvent event) {
        //    Debug.print("BEB: broadcasting message.");

        // get an array of processes
        SampleProcess[] processArray = this.processes.getAllProcesses();
        SendableEvent sendingEvent = null;
        
        if (event instanceof AckRecoveryEvent) {
            sendingEvent = event;
            sendingEvent.source = processes.getSelfProcess().getSocketAddress();
            int toSend = event.getMessage().popInt();
            sendingEvent.dest = processArray[toSend].getSocketAddress();
            sendingEvent.setSourceSession(this);
            try {
                sendingEvent.init();
                sendingEvent.go();
                Logger.log("RB: Sendıng AckRecoveryEvent to dest="+toSend);
                return;
            } catch (AppiaEventException e) {
                e.printStackTrace();
                return;
            }
        }
  
        else if (event instanceof EndRecoveryEvent || event instanceof RecoveryEvent) {

            //Send to alive ones.
            if(event instanceof RecoveryEvent){
               procStatusArray = (boolean []) event.getMessage().popObject();
            }
            

            // for each process...
            for (int i = 0; i < processArray.length; i++) {
                if (procStatusArray[i]) {
                    try {
                        // if it is the last process, don't clone the event
                        if (i == (processArray.length - 1)) {
                            sendingEvent = event;
                        } else {
                            sendingEvent = (SendableEvent) event.cloneEvent();
                        }

                        // set source and destination of event message
                        sendingEvent.source = processes.getSelfProcess().getSocketAddress();
                        sendingEvent.dest = processArray[i].getSocketAddress();

                        // sets the session that created the event.
                        // this is important when this session is sending a cloned event
                        sendingEvent.setSourceSession(this);


                        // if it is the "self" process, send the event upwards
                        if (i == processes.getSelfRank()) {
                            sendingEvent.setDir(Direction.UP);
                        }

                        // initializes and sends the message event
                        sendingEvent.init();
                        sendingEvent.go();
                        Logger.log("RB: Sending recovery or endRecovery event to dest="+i);
                    } catch (CloneNotSupportedException e) {
                        e.printStackTrace();
                        return;
                    } catch (AppiaEventException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            }
            return;

        }


        boolean sendSuccessor = event.getMessage().popBoolean();

        Logger.log(
                "RB: boolean popped in the beb");

        if (sendSuccessor) {
            //Send to only successor--in case of failures toSend should be checked it it is alive or not

            int toSend = (myRank + 1) % numProcs;
            while (!procStatusArray[toSend]) {
                toSend = (toSend + 1 ) % this.processes.getSize();
            }
            Logger.log("RB: Detination is " +toSend);
            sendingEvent = event;
            sendingEvent.source = processes.getSelfProcess().getSocketAddress();
            sendingEvent.dest = processArray[toSend].getSocketAddress();

            //setting session but we are not cloning?
            // sets the session that created the event.
            // this is important when this session is sending a cloned event
            sendingEvent.setSourceSession(this);
            try {
                sendingEvent.init();
                sendingEvent.go();
            } catch (AppiaEventException e) {
                e.printStackTrace();
                return;
            }

        } else {
            //Send to everyOne.


            // for each process...
            for (int i = 0; i < processArray.length; i++) {
                
                try {
                    // if it is the last process, don't clone the event
                    if (i == (processArray.length - 1)) {
                        sendingEvent = event;
                    } else {
                        sendingEvent = (SendableEvent) event.cloneEvent();
                    }

                    // set source and destination of event message
                    sendingEvent.source = processes.getSelfProcess().getSocketAddress();
                    sendingEvent.dest = processArray[i].getSocketAddress();

                    // sets the session that created the event.
                    // this is important when this session is sending a cloned event
                    sendingEvent.setSourceSession(this);


                    // if it is the "self" process, send the event upwards
                    if (i == processes.getSelfRank()) {
                        sendingEvent.setDir(Direction.UP);
                    }

                    // initializes and sends the message event
                    sendingEvent.init();
                    sendingEvent.go();
                    
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                    return;
                } catch (AppiaEventException e) {
                    e.printStackTrace();
                    return;
                }
            }

        }
    }

    /**
     * Delivers an incoming message.
     *
     * @param event
     */
    private void pp2pDeliver(SendableEvent event) {
        // just sends the message event up
        //    Debug.print("BEB: Delivering message.");
        try {
            event.go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }
    }
}
