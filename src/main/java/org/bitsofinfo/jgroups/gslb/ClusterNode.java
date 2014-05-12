package org.bitsofinfo.jgroups.gslb;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.blocks.MethodCall;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.protocols.relay.SiteMaster;
import org.jgroups.util.RspList;

public class ClusterNode {
	

	private static String getUserInput(String query) throws Exception {
		String input = null;
		while(input == null || input.isEmpty()) {
			System.out.print(query);
			BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));                      
			input = stdin.readLine();
		}
		
		return input;
		
	}
	

	public static void main(String[] args) {

		String clusterName = "test";
		String nodeName = null;
		
		try {
			
			String siteId = getUserInput("Enter site ID: ");
			String configRootDir = getUserInput("Config dir root: ");
			
			//clusterName = siteId + "-" +clusterName;
			nodeName = determineHostName() + "-" + UUID.randomUUID().toString().replace("-", "").substring(0,4);
			
			System.setProperty("configRootDir", configRootDir);
			
			InputStream configStream = ClusterNode.class.getResourceAsStream(siteId+"_gslb.xml");
			
			JChannel channel = new JChannel(configStream);
			
			/*channel.setReceiver(new ReceiverAdapter() {
				public void receive(Message msg) {
					System.out.println("received msg from " + msg.getSrc() + ": " + msg.getObject());
				}
			});*/
			
			 MethodCall call=new MethodCall(ClusterNode.class.getMethod("testMethod", int.class));
			 RequestOptions opts=new RequestOptions(ResponseMode.GET_ALL, 5000).setAnycasting(true);
			 RpcDispatcher rpc = new RpcDispatcher(channel, new ClusterNode());
			
			channel.connect(clusterName);
			
			// dumb message sender
			//channel.send(new Message(null, "sender: "+nodeName+", says: hello world"));
			
			// rpc invoker
			List<Address> dests=new ArrayList<Address>(channel.getView().getMembers());
			dests.add(new SiteMaster("site2"));
			RspList<Object> rsp_list = rpc.callRemoteMethods(dests, "testMethod",
					 						new Object[]{2},
					 						new Class[]{int.class},
					 						opts);

			// Alternative: use a (prefabricated) MethodCall:
			// call.setArgs(i);
			// rsp_list=disp.callRemoteMethods(null, call, opts);
			System.out.println("Responses: " + rsp_list);

			
			channel.close();
			
		} catch(Exception e) {
			e.printStackTrace();
		}

	}
	
	public static int testMethod(int input) {
		return input * 2;
	}
	
	private static String determineHostName() throws Exception {

		InetAddress addr = InetAddress.getLocalHost();
		// Get IP Address
		byte[] ipAddr = addr.getAddress();
		// Get sourceHost
		String tmpHost = addr.getHostName();

		// we only care about the HOST portion, strip everything else
		// as some boxes report a fully qualified sourceHost such as
		// host.domainname.com

		int firstDot = tmpHost.indexOf('.');
		if (firstDot != -1) {
			tmpHost = tmpHost.substring(0,firstDot);
		}
		return tmpHost;

	}
	
	

}
