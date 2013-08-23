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

package tfsd.allAckURB;

import tfsd.*;
import net.sf.appia.core.*;
import net.sf.appia.core.events.SendableEvent;
import net.sf.appia.core.events.channel.ChannelInit;

import java.net.SocketAddress;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * Session implementing the AllAck Uniform Reliable Broadcast protocol.
 * 
 * @author orcun
 * 
 */
public class AllAckURBSession extends Session {

    private ProcessSet processes;
    private int seqNumber;
    // array of lists
    private LinkedList<SendableEvent>[] from;
    // List of delivered MessageID objects
    private LinkedList<MessageID> delivered=new LinkedList<MessageID>();
    // List of pending MessageID objects
    private LinkedList<MessageID> pending=new LinkedList<MessageID>();
    //List of Acks 
    private HashMap<MessageID,LinkedList<Integer>> acks=new HashMap<MessageID,LinkedList<Integer>>();
    //cloned events to be delivered
    private HashMap<MessageID,SendableEvent> deliverEvents=new HashMap<MessageID,SendableEvent>();


    /**
     * @param layer
     */
    public AllAckURBSession(Layer layer) {
        super(layer);
    }

    /**
     * Main event handler
     */
    public void handle(Event event) {
        // Init events. Channel Init is from Appia and ProcessInitEvent is to know
        // the elements of the group
        if (event instanceof ChannelInit)
            handleChannelInit((ChannelInit) event);
        else if (event instanceof ProcessInitEvent)
            handleProcessInitEvent((ProcessInitEvent) event);

        else if (event instanceof SendableEvent) {
            if (event.getDir() == Direction.DOWN)
                // UPON event from the above protocol (or application)
                rbBroadcast((SendableEvent) event);
            else
                // UPON event from the bottom protocol (or perfect point2point links)
                bebDeliver((SendableEvent) event);
        }
        else if (event instanceof Crash)
            handleCrash((Crash) event);
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
    }

    /**
     * @param event
     */
    @SuppressWarnings("unchecked")
    private void handleProcessInitEvent(ProcessInitEvent event) {
        processes = event.getProcessSet();
        try {
            event.go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }

        from = new LinkedList[processes.getSize()];
        for (int i = 0; i < from.length; i++)
            from[i] = new LinkedList<SendableEvent>();
    }

    /**
     * Called when the above protocol sends a message.
     * 
     * @param event
     */
    private void rbBroadcast(SendableEvent event) {
        // first we take care of the header of the message
        SampleProcess self = processes.getSelfProcess();
        MessageID msgID = new MessageID(self.getProcessNumber(), seqNumber);
        seqNumber++;
	//Storing the message in the pending list.
	pending.add(msgID);
        System.out.println("RB: broadcasting message.");
        event.getMessage().pushObject(msgID);
        // broadcast the message
        bebBroadcast(event);
         }

    /**
     * Called when the lower protocol delivers a message.
     * 
     * @param event
     */
    private void bebDeliver(SendableEvent event) {
        System.out.println("RB: Received message from beb.");
	//getting the msg ID
        MessageID msgID = (MessageID) event.getMessage().peekObject();
	//cloning the event
	SendableEvent cloned = null;
            try {
                cloned = (SendableEvent) event.cloneEvent();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
                return;
            }
        deliverEvents.put(msgID, cloned);
        System.out.println("Cloned event mapped with msgID");
        SampleProcess pi = processes.getProcess((SocketAddress) event.source);
        int piNumber = pi.getProcessNumber();
	LinkedList<Integer> ll = new LinkedList<Integer>();
	//building ack array
	if(!acks.containsKey(msgID)){
	    ll.add(piNumber);
	    acks.put(msgID,ll);
	}
	else
	    ll=acks.get(msgID);
	    //checks whether ack is received or not (not to duplicate acks from same process)
	    if(!ll.contains(piNumber)){
		ll.add(piNumber);
		acks.put(msgID,ll);
	} 
	
	if(!pending.contains(msgID)){
	    System.out.println("Msg is added to pending list");
	    pending.add(msgID);
	    //triggering a bebbroadcast
	    bebBroadcast(event);
	}
      
        //try deliver msgs in the pending list
        tryDeliver();
 

    }

    /**
     * Called by this protocol to send a message to the lower protocol.
     * 
     * @param event
     */
    private void bebBroadcast(SendableEvent event) {
        System.out.println("RB: sending message to beb.");
        try {
            event.setDir(Direction.DOWN);
            event.setSourceSession(this);
            event.init();
            event.go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when some process crashed.
     * 
     * @param crash
     */
    private void handleCrash(Crash crash) {
        int pi = crash.getCrashedProcess();
        System.out.println("Process " + pi + " failed.");

        try {
            crash.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }

        // changes the state of the process to "failed"
        processes.getProcess(pi).setCorrect(false);
	//try deliver msg.
        tryDeliver();
    }
    private void tryDeliver(){
        //iterate msgs in pending list and deliver them if you have all acks.
        Iterator<MessageID> i=pending.iterator();
        LinkedList<Integer> ll = new LinkedList<Integer>();
        //get alive process number
        int count= processes.getCorrectSize(); 
        System.out.println("Correct Process number is " + count);
        SendableEvent toDeliver = null;
        while (i.hasNext()) {
            MessageID messageID = i.next();
            //list of acks for that msg id
            ll=acks.get(messageID);
            System.out.println("ack number is " + ll.size());
            //If it is majorityAck protocol if condition should look for count/2 instead of count.
            if(ll.size() >= count){
                if(!(delivered.contains(messageID))){
                    delivered.add(messageID);
                    //delivering the message:
                    toDeliver=deliverEvents.get(messageID);
                    toDeliver.getMessage().popObject();
                            try {
                                toDeliver.setDir(Direction.UP);
                                toDeliver.setSourceSession(this);
                                toDeliver.init();
                                toDeliver.go();
                                } 
                            catch (AppiaEventException e) {
                                e.printStackTrace();
                                }
              pending.remove(messageID);
              System.out.println("Msg is delivered and removed from pending list");
                }
            }
                
            
        }
    }
}
