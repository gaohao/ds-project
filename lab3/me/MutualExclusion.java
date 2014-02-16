package me;

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
	private boolean votingGroupName;
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
		
	}
	
	public void rquestCS() {
		
	}
	
	public void releaseCS() {
		
	}
	
	class MessageReceiver implements Runnable {

		@Override
		public void run() {
			while (true) {
				MulticastMessage message = multicastService.receive();
				String kind = message.getKind();	
				
				if (kind.equals("REQUEST")) {
					if (state == STATE.CS_HELD || voted == true) {
						try {
							requestQueue.put(message);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} else {
						MulticastMessage replyMessage = new MulticastMessage(null, localName, 
								message.getSource(), "ACK", null, Type.UNICAST, null);
						multicastService.send(replyMessage);
						voted = true;
					}
				} else if (kind.equals("RELEASE")) {
					
				} else if (kind.equals("ACK")) {
					if (state == STATE.CS_WANTED && remainingACK.contains(message.getSource())) {
						remainingACK.remove(message.getSource());
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
				}
			}
		}

	}
}
