import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.yaml.snakeyaml.Yaml;


public class MessagePasser {
	private static LinkedList<TimeStampedMessage> incoming_buffer; //buffer of what is coming into this instance of MP
	private LinkedList<TimeStampedMessage> outgoing_buffer; //buffer of what this instance of MP is sending out
	private Yaml yaml; //This will parse the configuration_filename
	private long last_modified; //last modified time for the configuration_filename.yaml
	private static String config_filename;
	private Map<String, Object> config_parsing;
	//maybe have Map<String, Socket> connections to be global map of all active connections
	private static Map<String, Socket> nodes;
	//a list of all possible user names, ip, ports that aren't necessarily connected 
	private static ArrayList<User> users;
	private static User local_user;
    private static ServerSocket local_socket;
    private static int local_port; //set this so the information can be used by the ServerSocket thread
    private static ArrayList<Rule> sendRules;
    private static ArrayList<Rule> receiveRules;
    //Clock variables
    private static ClockService clock;
    
	public MessagePasser(String configuration_filename, String local_name, ClockService clock){
		//parse configuration_filename and setup sockets for communicating with all processes
		//      listed in the configuration section of the file
		//initialize buffers to hold incoming and outgoing messages to the rest of the nodes in the system
		//     (may need additional state? threads?)
		incoming_buffer = new LinkedList<TimeStampedMessage>();
		outgoing_buffer = new LinkedList<TimeStampedMessage>();
		
		sendRules = new ArrayList<Rule>();
		receiveRules = new ArrayList<Rule>();

		MessagePasser.clock = clock;
		
		yaml = new Yaml();
		
		try { 
			//try opening the configuration_filename
			File config_file = new File(configuration_filename);
			InputStream input = new FileInputStream(config_file);
			//the keySet is [configuration, sendRules, receiveRules] for the configuration_filename map
			config_parsing = (Map<String, Object>) yaml.load(input);
			last_modified = 0; //we don't want this to be the real time so we can update Rules initially
			config_filename = configuration_filename;
			users = new ArrayList<User>();
			nodes = new HashMap<String, Socket>();
			
			//for each key, get a list corresponding to each '-' under the header's name + :
			//	  Just need to deal with the configuration list here though
			for (Object config_item : (ArrayList<Object>) config_parsing.get("configuration")){
				//each config_item is a mapping of name, ip, and port
				Map<String, Object> connection_info = (Map<String, Object>) config_item;
				if(((String)connection_info.get("name")).equals(local_name)){
					//this connection info is the local host, so it should be listening, not making a connection
					//      spin off a thread that will act as a server listening for incoming connections
					local_port = (Integer)connection_info.get("port");
					local_user = new User(local_name,InetAddress.getByName((String)connection_info.get("ip")),local_port );
					Thread local = new Thread(new LocalServer());
					local.start();
				} else{
					//Add this connection (name, ip, port) to a global list of all possible connections
					User currentUser = new User((String)connection_info.get("name"),InetAddress.getByName((String)connection_info.get("ip")), (Integer)connection_info.get("port"));
					users.add(currentUser);
					if(clock.getClockType().equals("vector")){
						if(!currentUser.getName().equals("logger")) // ignore logger
							((VectorClockService)clock).updateTimeStamp(currentUser.getName(), 0);
					}
				}
				//System.out.println(connection_info);
			}
			//System.out.println(users);
			updateRules(configuration_filename);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			System.out.println("Could not open file: " + configuration_filename);
			System.exit(0);
		}  catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	void updateRules(String config_file){
		//try opening the configuration_filename
		File configuration_file = new File(config_file);
		//If the config file hasn't been changed, don't bother trying to update all the rules
		if(configuration_file.lastModified() == last_modified)
			return;
		last_modified = configuration_file.lastModified();
		//get rid of all rules currently stored because you have no guarantee that any are still the same
		sendRules.clear();
		receiveRules.clear();
		InputStream input;
		try {
			input = new FileInputStream(configuration_file);		
			//the keySet is [configuration, sendRules, receiveRules] for the configuration_filename map
			config_parsing = (Map<String, Object>) yaml.load(input);
			//Now go through sendRules and receiveRules to populate the two Rules arrays
			for(Object config_item : (ArrayList<Object>) config_parsing.get("sendRules")){
				//each config_item is a mapping of action, and some combination of src, dest, kind, seqNum, dupe
				Map<String, Object> rule_info = (Map<String, Object>) config_item;
				Rule newRule = new Rule((String)rule_info.get("action"));
				if(rule_info.containsKey("src"))
					newRule.setSrc((String)rule_info.get("src"));
				if(rule_info.containsKey("dest"))
					newRule.setDest((String)rule_info.get("dest"));
				if(rule_info.containsKey("kind"))
					newRule.setKind((String)rule_info.get("kind"));
				if(rule_info.containsKey("seqNum"))
					newRule.setSeqNum((Integer)rule_info.get("seqNum"));
				if(rule_info.containsKey("dupe")){
					if(((Boolean)rule_info.get("dupe")) == true)
						newRule.setDupe(true);
				}
				//add the new sendRule to the sendRule's array
				sendRules.add(newRule);	
			}
			//System.out.println(sendRules);
			for(Object config_item : (ArrayList<Object>) config_parsing.get("receiveRules")){
				//each config_item is a mapping of action, and some combination of src, dest, kind, seqNum, dupe
				Map<String, Object> rule_info = (Map<String, Object>) config_item;
				Rule newRule = new Rule((String)rule_info.get("action"));
				if(rule_info.containsKey("src"))
					newRule.setSrc((String)rule_info.get("src"));
				if(rule_info.containsKey("dest"))
					newRule.setDest((String)rule_info.get("dest"));
				if(rule_info.containsKey("kind"))
					newRule.setKind((String)rule_info.get("kind"));
				if(rule_info.containsKey("seqNum"))
					newRule.setSeqNum((Integer)rule_info.get("seqNum"));
				if(rule_info.containsKey("dupe")){
					if(((Boolean)rule_info.get("dupe")) == true)
						newRule.setDupe(true);
				}
				//add the new sendRule to the sendRule's array
				receiveRules.add(newRule);
			}
			//System.out.println(receiveRules);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			System.out.println("Error updating rules. All rules have been deleted. Please check your config file.");
		}
	}
	void send(TimeStampedMessage message){
		String action = "";
		//first call a method to check for updates on rules
		updateRules(config_filename);
		//set the sequence number of the message before sending it
		//seqNum should be non-reused, monotonically increasing integer values
		local_user.incrementSeqNum();
		message.set_seqNum(local_user.getSeqNum());
		message.set_source(local_user.getName());
		message.set_duplicate(false);

		// timer +1
		clock.increaseTimeByOne();
		if (clock.getClockType().equals("vector")) {
			message.setTimeStamp(((VectorClockService) clock).getVectorClock());
		} else{
			message.addTimeStamp("LogicalClock", clock.getTimeStamp());
		}
		//System.out.println(message + "seqNum: " + message.get_seqNum());
		//First check the message against any SendRules before delivering the message to the socket
		if(sendRules.size() > 0){
			Rule currentRule = sendRules.get(0);
			for(int i = 0; i< sendRules.size(); i++){
				currentRule = sendRules.get(i);
				//System.out.println("checking sendRule: " + currentRule.toString());
				if(currentRule.match(message)){
					//System.out.println("Matched a sendRule: " + currentRule.toString());
					action = currentRule.getAction();
					break;
				}
				//currentRule = sendRules.get(i);
			}
		}
		if(!action.equals("")){
			//This means that the message matched a rule, so now handle one of the three possibilities appropriately
			if(action.toLowerCase().equals("drop")){
				//Don't send the message and mark all delayed messages as no longer delayed
				/*for(Message msg : outgoing_buffer){
					msg.set_delayed(false);
				}*/
			} else if(action.toLowerCase().equals("delay")){
				//message.set_delayed(true);
				outgoing_buffer.add(message);				
				return;
			} else if(action.toLowerCase().equals("duplicate")){
				//Mark all delayed messages as no longer delayed
				/*for(Message msg : outgoing_buffer){
					msg.set_delayed(false);
				}*/
				//add one copy of the message to our outgoing_buffer
				outgoing_buffer.addFirst(message);				
				//now add a copy of the message with duplicate set to true
				TimeStampedMessage duped = new TimeStampedMessage(message.get_dest(), message.get_kind(), message.get_data());
				duped.set_seqNum(local_user.getSeqNum());
				duped.set_source(local_user.getName());
				duped.set_duplicate(true);
				//update our own clock and timestamp the message
				outgoing_buffer.addFirst(duped);
			}
		} else {
			outgoing_buffer.addFirst(message); //message did not match any sendRules, so just send it normally
		}
		//By getting to this point, we want to send all messages that are in the outgoing_buffer
		modify_outgoing();
		//Now, all that's left on the outgoing_buffer are messages that we couldn't send due to connection issues
		//We need to mark them as no longer delayed so that we can try sending them the next time we call send
		for(Message msg : outgoing_buffer){
			msg.set_delayed(false);
			//System.out.println("Still on outgoing buffer: " + msg);
		}
		
	}
	
	TimeStampedMessage receive(){
		//first call a method to check for updates on rules
		updateRules(config_filename);
		//deliver a single message from the front of this input queue (if not marked as delayed)
		return modify_incoming(null, false, false, true);
	}
	//This is a thread that will act as a local server to accept incoming connections
	private static class LocalServer implements Runnable{
		final ExecutorService threadPool = Executors.newCachedThreadPool();
		public void run(){
			try {
				local_socket = new ServerSocket(local_port);
				//System.out.println("Made a ServerSocket and about to listen\n");
				while(true){
					//wait for a connection and put it into aNode
					Socket aNode = local_socket.accept();
					/*
					 * get remote IP
					 * find corresponding name
					 * store connection into "nodes" Map object
					 * */
					/*InetAddress connectedIP = aNode.getInetAddress();
					int fromPort = aNode.getPort();
					for(User currentUser : users){
						if(!currentUser.getUser(connectedIP).equals("")){
							modify_nodes(currentUser.getUser(connectedIP, fromPort), aNode, 1);
							threadPool.submit(new ReceiveIncomingConnections(aNode));
							break;
						}
					}*/
					threadPool.submit(new ReceiveIncomingConnections(aNode, false));
					
				}
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				System.out.println("Could not receive message properly. Connection not made.");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				System.out.println("Could not receive message properly. Connection not made.");
			} finally{
				try{
					local_socket.close();
				} catch (Exception e){
					System.err.println("Could not close connection properly: " + e);
				}
			}	
		}
	}
	//This is a thread that will receive the data from an incoming connection and fill up incoming_buffer
	private static class ReceiveIncomingConnections implements Runnable{
		Socket node;
		Boolean identified_source = false;
		public ReceiveIncomingConnections(Socket node, Boolean identified){
			this.node = node;
			//this pertains to if we know the right port for the connecting user or not
			//identified being true initially means we initiated the connection to the listening port
			//identified being false initially means we got a connection and need to find the right user
			identified_source = identified;
		}
		public void run(){
			/*
			 * get input stream from socket (listen essentially)
			 * put received data into Message object
			 * check Message object against recieveRules
			 * put message in "incoming_buffer" array (as needed)
			 * thread to loop and keep reading from socket
			 * */
			try {
				while(true){			
					ObjectInputStream ois = new ObjectInputStream(node.getInputStream());
					//This is a blocking call that should only move on once we read in a full Message object
					//System.out.println("waiting to read in a message from a listening thread");
					TimeStampedMessage msg = (TimeStampedMessage) ois.readObject();					
					if(!identified_source){
						modify_nodes(msg.get_dest(), node, 1);
						for(User usr : users){
							if(usr.isMyName(msg.get_dest()))
								usr.setFromPort(node.getPort());
						}
						identified_source = true;
					}
					//System.out.println("just read in a message from a listening thread");
					//Check against receiveRules
					Rule currentRule = receiveRules.get(0);
					String action = "";
					for(int i = 0; i < receiveRules.size(); i++){
						currentRule = receiveRules.get(i);
						if(currentRule.match(msg)){
							action = currentRule.getAction();
							//System.out.println("Matched receiveRule! " + currentRule.toString());
							break;
						}
						//currentRule = receiveRules.get(i);
					}
					//At this point action is either the action to be performed, or "" if we didn't match any rules
					if(!action.equals("")){
						if(action.toLowerCase().equals("drop")){
							//don't add the message, but must change all current msgs in incoming_buffer to not be delayed anymore
							//modify_incoming(Message, add_to_queue, mark_everything_as_not_delayed, receive_a_message_from_queue)
							modify_incoming(null, false, true, false);
						}
						else if(action.toLowerCase().equals("delay")){
							msg.set_delayed(true);
							modify_incoming(msg, true, false, false);
						}
						else if(action.toLowerCase().equals("duplicate")){
							//must change all current msgs in incoming_buffer to not be delayed anymore
							//add the message to the incoming_buffer twice since we matched a duplicate rule
							modify_incoming(msg, true, false, false);
							modify_incoming(msg, true, true, false);
						}
					}
					else{
						//If we don't match any rules, just put the message on the incoming_buffer
						//since we received something that wasn't delay, we make sure everything is changed from delay on the buffer;
						modify_incoming(msg, true, true, false);
						
					}
					//System.out.println(msg.toString());
				}
			} catch (IOException e) {
				//THIS IS WHAT HAPPENS WHEN A USER DISCONNECTS
				InetAddress connectedIP = node.getInetAddress();
				int fromPort = node.getPort();
				String connectedUser;
				for(User currentUser : users){
					if(!currentUser.getUser(connectedIP, fromPort).equals("")){
						connectedUser = currentUser.getUser(connectedIP, fromPort);
						//System.out.println("matched user: " + currentUser);
						System.out.println("Disconnected from user: " + connectedUser); //find the user
						//make sure that this user's connection Socket is removed from the global Map<String, Socket> nodes
						modify_nodes(connectedUser, null, 2);
						break;
					}
				}
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				System.out.println("There was a problem receiving a message.");
			} 
			//System.out.println("Something connected!");
		}
	}
	private void modify_outgoing(){
		/*
		 * For each message in outgoing_buffer
		 * 	   check if there is a connection already open for that message
		 * 	   if there is, send the data
		 *     if there is not, find the right dest user, try to open socket, add to global nodes, spin off listening thread, send data
		 *          if fail to open socket, what to do with message? inform user regardless that you cannot connect
		 */
		Socket sendSocket;
		TimeStampedMessage msg;
		while( outgoing_buffer.peek() != null && outgoing_buffer.peek().get_delayed() == false){
			msg = outgoing_buffer.poll();
			sendSocket = modify_nodes(msg.get_dest(), null, 3);
			
			if(sendSocket == null){
				User foundUser = null;
				//find the user that we're trying to send to and get their IP/port
				for(User destUser : users){
					if(destUser.isMyName(msg.get_dest())){
						foundUser = destUser;
						break;
					}
				}
				if(foundUser != null){
					try {
						//System.out.println("Trying to connect to " + msg.get_dest() + " for first time to send message");
						//System.out.println("   Because current nodes are: " + nodes.toString());
						//try to open a connection with that destination user
						sendSocket = new Socket(foundUser.getIP(), foundUser.getPort());
						//if connection successful, add this connection to the global nodes list
						modify_nodes(msg.get_dest(), sendSocket, 1);
						//now spin off a thread to listen on this socket
						Thread newListener = new Thread(new ReceiveIncomingConnections(sendSocket, true));
						newListener.start();
						//now actually send the data
						sendData(msg, sendSocket);
						
					} catch (IOException e) {
						// Inform user that you cannot connect or send that message due to connection issues
						System.out.println("Cannot connect to user " + msg.get_dest() + " at this time.  Please try again later.");
						msg.set_delayed(true);
						outgoing_buffer.add(msg);
						}
					} else{
						//this means the user isn't in our list
						System.out.println("Sorry, but user " + msg.get_dest() + " is not in our system.");
					}				
			} else{
				//System.out.println("Sending a message and already have a connection to " + msg.get_dest());
				//We already have an open connection to the message's destination, so just send the data
				sendData(msg, sendSocket);
			}
		}
	}
	
	private void sendData(TimeStampedMessage msg, Socket sendSocket){
		try {
			ObjectOutputStream oos = new ObjectOutputStream(sendSocket.getOutputStream());
			oos.writeObject(msg);
		} catch (IOException e) {
			System.out.println("Failed to send message to " + msg.get_dest() + " of kind " + msg.get_kind() + " of seqNum " + msg.get_seqNum() 
					+ "\nand data: " + msg.get_data().toString());
			System.out.println("The message has been added back to the queue for a later attempt.");
			outgoing_buffer.add(msg);
		}
	}
	
	private static synchronized TimeStampedMessage modify_incoming(TimeStampedMessage msg, Boolean add, Boolean changeDelay, Boolean receive){
		if(add)
			incoming_buffer.add(msg);
		if(changeDelay){
			for(Message currMessage : incoming_buffer){
				currMessage.set_delayed(false);
			}
		}
		if(receive){
			if(incoming_buffer.peek() != null){
				if(incoming_buffer.peek().get_delayed() == false){
					msg =  incoming_buffer.poll();
					if (clock.getClockType().equals("vector")){ //vector clock
						HashMap<String, TimeStamp> srcTimeStamp = msg.getTimeStamp();
						for(String name : srcTimeStamp.keySet()){
							if (srcTimeStamp.get(name).isGreater( ((VectorClockService)clock).getTimeStamp(name))) {
								((VectorClockService)clock).updateTimeStamp(name, srcTimeStamp.get(name).getTime());
							}
						}
						if (msg.getTimeStamp().get(msg.get_source()).isGreater(clock.getTimeStamp())) {
							//if the sender's clock is larger than us, then set our time to sender's time + 1
							int newTime = msg.getTimeStamp().get(msg.get_source()).getTime() + 1;
							clock.setTime(newTime);
						} else {
							clock.increaseTimeByOne();
						}
					} else { // logical clock
						TimeStamp srcTimestamp = msg.getTimeStamp().get("LogicalClock");
						//if the sender's clock is larger than us, then set our time to sender's time + 1
						if (srcTimestamp.isGreater(clock.getTimeStamp()) ) {
							clock.ts.setTime(srcTimestamp.getTime() + 1);
						} else
							clock.increaseTimeByOne();
					}
					
					return msg;
				}
			}
		}
		//System.out.println(incoming_buffer.size() + " Message(s) now on incoming_buffer");
		return null;
	}
	
	private static synchronized Socket modify_nodes(String name, Socket sock, int action){
		//Add or remove a user from the global Map of nodes (active connections)
		/*
		 * Action of 1 means to add name/socket pair to the global map
		 * Action of 2 means to remove node 'name' from global map
		 * Action of 3 means to check if there is an active connection for name, and return it if it exists*/
		if (action == 1) {
			nodes.put(name, sock);
		} else if (action == 2) {
			nodes.remove(name);
		} else if (action == 3) {
			for(String key : nodes.keySet()) {
				if(key.equals(name)) {
					//this means there is an active connection, so return the socket
					return nodes.get(key);
				}
			}
		} else if (action == 4) {
			//just for debugging purposes
			System.out.println(nodes);
		}
		return null;
	}
}
