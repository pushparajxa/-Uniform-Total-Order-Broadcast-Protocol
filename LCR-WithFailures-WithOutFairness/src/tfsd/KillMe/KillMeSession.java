/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package src.tfsd.KillMe;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;

import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.SendableEvent;
import net.sf.appia.core.events.channel.ChannelInit;
import src.Logger;

import tfsd.ProcessInitEvent;
import tfsd.ProcessSet;

/**
 *
 * @author orcun
 */
public class KillMeSession extends Session {

    private Channel channel;
    private ProcessSet processes;
    private int myRank;
    private int eventCount;

    public KillMeSession(Layer layer) {
        super(layer);
    }

    /**
     * Main event handler
     */
    public void handle(Event event) {
        // Init events. Channel Init is from Appia and ProcessInitEvent is to know
        // the elements of the group
        if (event instanceof ChannelInit) {
            handleChannelInit((ChannelInit) event);
        } else if (event instanceof ProcessInitEvent) {
            handleProcessInit((ProcessInitEvent) event);
        } else if (event instanceof SendableEvent) {
            handleSendableEvent((SendableEvent) event);
        }

    }

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
        this.eventCount = 0;

        this.myRank = processes.getSelfRank();



        try {
            event.go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }


    }

    private void handleSendableEvent(SendableEvent event) {


        if (myRank == 2) {
            this.eventCount++;
            if (this.eventCount >= 2000) {
                Logger.log("KILL: Killıng the JVM on the Process with Rank=" + myRank);
                System.exit(0);
            }
        }

        try {
            event.go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }


    }
}
