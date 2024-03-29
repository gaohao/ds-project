package me;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;

import multicast.CORMulticast;
import multicast.MulticastMessage;
import multicast.MulticastMessage.Type;
import utils.ConfigurationParser;
import utils.ConfigurationParser.ConfigInfo;

public class MutualExclusion {
	public enum STATE {
		CS_RELEASED, CS_WANTED, CS_HELD;
	}
	private String localName;
	private boolean voted;
	private String votingGroupName;
	private ArrayList<String> votingGroupMembers;
	private LinkedBlockingQueue<MulticastMessage> requestQueue;
	private HashSet<String> remainingACK;
	private CORMulticast multicastService;
	private STATE state;
	
	public MutualExclusion(String configurationFileName, String localName,
			ConfigInfo ci, ConfigurationParser cp) {
		voted = false;
		this.localName = localName;
		requestQueue = new LinkedBlockingQueue<MulticastMessage>();
		remainingACK = new HashSet<String>();
		multicastService = new CORMulticast(configurationFileName, localName, ci, cp);
		state = STATE.CS_RELEASED;
		// find voting group
		votingGroupName = "v" + localName;
		for (HashMap<String, Object> g : ci.getGroups()) {
			String groupName = (String) g.get("name");
			if (groupName.equals(votingGroupName)) {
				votingGroupMembers = (ArrayList<String>) (g.get("members"));
				break;
			}
		}
		
		Thread receiverThread = new Thread(new MessageReceiver());
		receiverThread.start();
	}
	
	public void rquestCS() {
		if (state ==  STATE.CS_RELEASED) {
			state = STATE.CS_WANTED;
			System.out.println("State changed from RELEASED to WANTED!");
			remainingACK.clear();
			String members = "";
			for (String mem : votingGroupMembers) {
				members += mem + " ";
				remainingACK.add(mem);
			}
			System.out.println("Request message sent to: " + members);
			MulticastMessage message = new MulticastMessage(localName, votingGroupName, "REQUEST", "", Type.DATA);
			multicastService.send(message);
		}
	}
	
	public void releaseCS() {
		if (state == STATE.CS_HELD) {
			System.out.println("State changed from HELD to RELEASED!");
			state = STATE.CS_RELEASED;
			String members = "";
			for (String mem : votingGroupMembers) {
				members += mem + " ";
			}
			System.out.println("Release message sent to: " + members);
			MulticastMessage message = new MulticastMessage(localName, votingGroupName, "RELEASE", "", Type.DATA);
			multicastService.send(message);
		} else {
			System.out.println("State: " + state);
		}
	}
	
	class MessageReceiver implements Runnable {

		@Override
		public void run() {
			while (true) {
				MulticastMessage message = multicastService.receive();
				String kind = message.getKind();	
				if (kind.equals("REQUEST")) {
					System.out.println("Receive a REQUEST from " + message.getSource());
					if (state == STATE.CS_HELD || voted == true) {
						try {
							System.out.println("Put message to request queue");
							requestQueue.put(message);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} else {
						System.out.println("Send reply message and voted = true");
						MulticastMessage replyMessage = new MulticastMessage(null, localName, 
								message.getSource(), "ACK", null, Type.UNICAST, null);
						multicastService.send(replyMessage);
						voted = true;
					}
				} else if (kind.equals("RELEASE")) {
					System.out.println("Receive a RELEASE from " + message.getSource());
					if (requestQueue.isEmpty()) {
						voted = false;
					} else {
						try {
							MulticastMessage msg = requestQueue.take();
							MulticastMessage replyMessage = new MulticastMessage(null, localName, 
									msg.getSource(), "ACK", null, Type.UNICAST, null);
							multicastService.send(replyMessage);
							voted = true;
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				} else if (kind.equals("ACK")) {
					System.out.println("Receive a ACK from " + message.getSource());
					if (state == STATE.CS_WANTED && remainingACK.contains(message.getSource())) {
						remainingACK.remove(message.getSource());
						if (remainingACK.isEmpty()) {
							state = STATE.CS_HELD;
							System.out.println("State changed from WANTED to HELD!!");
						}
					}
				}
			}
		}

	}
	
	// Check the status to see whether can enter cs or not
	class StatusChecker implements Runnable {

		@Override
		public void run() {
			while (true) {
				if (state == STATE.CS_WANTED && remainingACK.isEmpty()) {
					state = STATE.CS_HELD;
					System.out.println("State changed from WANTED to HELD!");
				}
			}
		}

	}
}
