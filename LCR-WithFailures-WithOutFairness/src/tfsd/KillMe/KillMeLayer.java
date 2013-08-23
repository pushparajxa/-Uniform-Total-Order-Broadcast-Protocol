/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package src.tfsd.KillMe;

import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.SendableEvent;
import net.sf.appia.core.events.channel.ChannelClose;
import net.sf.appia.core.events.channel.ChannelInit;

import tfsd.ProcessInitEvent;


/**
 *
 * @author orcun
 */
public class KillMeLayer extends Layer {

    public KillMeLayer() {
        /* events that the protocol will create */
        evProvide = new Class[0];

        /*
         * events that the protocol require to work. This is a subset of the
         * accepted events
         */
        evRequire = new Class[3];
        evRequire[0] = SendableEvent.class;
        evRequire[1] = ChannelInit.class;
        evRequire[2] = ProcessInitEvent.class;
        

        /* events that the protocol will accept */
        evAccept = new Class[4];
        evAccept[0] = SendableEvent.class;
        evAccept[1] = ChannelInit.class;
        evAccept[2] = ChannelClose.class;
        evAccept[3] = ProcessInitEvent.class;
        

    }

    /**
     * Creates a new session to this protocol.
     *
     * @see appia.Layer#createSession()
     */
    public Session createSession() {
        return new KillMeSession(this);
    }
}
