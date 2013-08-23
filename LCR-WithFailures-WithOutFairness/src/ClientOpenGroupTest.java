import java.io.IOException;
import java.net.SocketAddress;

import net.sf.appia.jgcs.AppiaGroup;
import net.sf.appia.jgcs.AppiaProtocolFactory;
import net.sf.appia.jgcs.AppiaService;
import net.sf.jgcs.Annotation;
import net.sf.jgcs.ControlSession;
import net.sf.jgcs.DataSession;
import net.sf.jgcs.ExceptionListener;
import net.sf.jgcs.JGCSException;
import net.sf.jgcs.Message;
import net.sf.jgcs.MessageListener;
import net.sf.jgcs.Protocol;
import net.sf.jgcs.ProtocolFactory;
import net.sf.jgcs.Service;
import net.sf.jgcs.UnsupportedServiceException;

/**
 * 
 * This class defines a ClientOpenGroupTest
 * This example shows how to use and configure Appia with jGCS
 * using an open group, where there is a group of servers that accept
 * Messages from external members. This is the (external) client part.
 * 
 * The example only shows how to configure and use, and it only sends
 * dummy messages. It does not intend to implement any algorithm.
 * 
 * @author <a href="mailto:nunomrc@di.fc.ul.pt">Nuno Carvalho</a>
 * @version 1.0
 */
public class ClientOpenGroupTest implements MessageListener, ExceptionListener {

    private static final int MAX_MESSAGES=2;
    
    // only the data session is used
	private DataSession data;
	private ControlSession control;
	private Service rpcService;
	private long tInit=0;
	private int id=0;
	private int lastReceivedMessage = -1;
	
	public ClientOpenGroupTest(DataSession data, ControlSession control, Service serviceVSC) {
		this.data = data;
		this.rpcService = serviceVSC;		
		this.control = control;
	}

	// messages are received here.
	public Object onMessage(Message msg) {
        System.out.println("Received message\n");
	    boolean canSend=false;
	    try {
            ClientMessage cliMsg = (ClientMessage) Constants.createMessageInstance(msg.getPayload());
            cliMsg.unmarshal();
            if(cliMsg.id > lastReceivedMessage){
                lastReceivedMessage = cliMsg.id;
                canSend = true;
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
	    long deltaT = System.nanoTime()-tInit;
	    System.out.println("Message from "+msg.getSenderAddress()+" TIME = "+deltaT+" nanos");

	    return null;
	}

	public void onJoin(SocketAddress peer) {
		System.out.println("-- JOIN: " + peer);
	}

	public void onLeave(SocketAddress peer) {
		System.out.println("-- LEAVE: " + peer);
	}

	public void onFailed(SocketAddress peer) {
		System.out.println("-- FAILED: " + peer);
	}
	
	public void onException(JGCSException arg0) {
		System.out.println("-- EXCEPTION: " + arg0.getMessage());
		arg0.printStackTrace();
	}
	
	private void sendMessage() throws UnsupportedServiceException, IOException{
        Message m = data.createMessage();
        ClientMessage climsg = new ClientMessage(id++);
        climsg.marshal();
        byte[] bytes = Constants.createMessageToSend(Constants.MessageType.CLIENT, climsg.getByteArray());
        m.setPayload(bytes);
        tInit=System.nanoTime();
        System.out.println("sending message #"+(id-1)+" PAYLOAD="+bytes.length);
        data.send(m, rpcService, null,null);
	}

	public void run() throws Exception {
	    sendMessage();
	    Thread.sleep(Long.MAX_VALUE);
	}

	public static void main(String[] args) {
        if(args.length != 1){
            System.out.println("Must put the xml file name as an argument.");
            System.exit(1);
        }
		try {
			ProtocolFactory pf = new AppiaProtocolFactory();
			AppiaGroup g = new AppiaGroup();
			g.setConfigFileName(args[0]);
			g.setGroupName("group");
			g.setManagementMBeanID("id1");
			Protocol p = pf.createProtocol();
			DataSession session = p.openDataSession(g);
			ControlSession control = p.openControlSession(g);
			Service service = new AppiaService("rrpc");
			ClientOpenGroupTest test = new ClientOpenGroupTest(session, control, service);
			session.setMessageListener(test);
			session.setExceptionListener(test);
			test.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
