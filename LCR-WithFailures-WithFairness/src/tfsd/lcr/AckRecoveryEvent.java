/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package src.tfsd.lcr;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.SendableEvent;

/**
 *
 * @author orcun
 */
public class AckRecoveryEvent extends SendableEvent {
    
      /**
   * Default constructor.
   */
  public AckRecoveryEvent() {
    super();
  }

  /**
   * Constructor of the event.
   * 
   * @param channel
   *          the Appia Channel
   * @param dir
   *          the direction of the event.
   * @param source
   *          the session that created the event.
   * @throws AppiaEventException
   */
  public AckRecoveryEvent(Channel channel, int dir, Session source)
      throws AppiaEventException {
    super(channel, dir, source);
  }

}


