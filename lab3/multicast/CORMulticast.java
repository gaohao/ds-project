package multicast;

import ipc.MessagePasser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import multicast.MulticastMessage.Type;
import utils.ConfigurationParser;
import utils.ConfigurationParser.ConfigInfo;

/**
 * Casual-Ordering Reliable Multicast (CORMulticast). Implementation details:
 * (1) Reliability Tools #1 in the lecture slides (R-multicast and R-deliver on
 * the book, Figure 15-9). (2) Causal ordering using vector timestamps on the
 * book (Figure 15.15).
 * 
 * @author Hao Gao
 * @author Yinsu Chu
 * 
 */
public class CORMulticast {
	private static final String GROUP_NAME = "name";
	private static final String GROUP_MEMBER = "members";
	private static final long timeout = 10 * 1000;

	// each group is represented as a HashMap, inside which key GROUP_NAME has
	// the group's name as a String value, key GROUP_MEMBER has the group's
	// members as an ArrayList<String> value
	private ArrayList<HashMap<String, Object>> groupData;

	// the multicast infrastructure is built upon MessagePasser
	private MessagePasser messagePasser;

	private HashMap<String, GroupManager> nameToManager;
	private HashMap<String, Integer> groupNameToId;
	private ArrayList<GroupManager> groupManagers;

	private String localName;

	// deliverQueue is shared among all groups
	private LinkedBlockingQueue<MulticastMessage> deliverQueue;

	public CORMulticast(String configurationFileName, String localName,
			ConfigInfo ci, ConfigurationParser cp) {
		this.groupData = ci.getGroups();
		this.messagePasser = new MessagePasser(configurationFileName,
				localName, ci, cp);
		this.nameToManager = new HashMap<String, GroupManager>();
		this.groupNameToId = new HashMap<String, Integer>();
		this.groupManagers = new ArrayList<GroupManager>();
		this.deliverQueue = new LinkedBlockingQueue<MulticastMessage>();
		this.localName = localName;
		initializeGroups();

		Thread receiverThread = new Thread(new Receiver());
		receiverThread.start();

		Thread statusCheckerThread = new Thread(new StatusChecker());
		statusCheckerThread.start();
	}

	@SuppressWarnings("unchecked")
	private void initializeGroups() {
		int groupId = 0;
		for (HashMap<String, Object> g : groupData) {
			HashMap<String, Integer> memberNameToId = new HashMap<String, Integer>();
			String groupName = (String) g.get(GROUP_NAME);
			ArrayList<String> groupMembers = (ArrayList<String>) (g
					.get(GROUP_MEMBER));
			for (int i = 0; i < groupMembers.size(); i++) {
				memberNameToId.put(groupMembers.get(i), i);
			}
			GroupManager groupManager = new GroupManager(localName, groupName,
					groupId, groupMembers, new int[groupMembers.size()],
					memberNameToId);
			groupManagers.add(groupManager);
			nameToManager.put(groupName, groupManager);
			groupNameToId.put(groupName, groupId);
			groupId++;
		}
	}

	public void send(MulticastMessage message) {
		if (message.getType() == Type.UNICAST) {
			messagePasser.send(message);
		} else {
			// send a message to a group
			String groupName = message.getGroupName();
			if (nameToManager.containsKey(groupName)) {
				boolean isValidMember = false;
				for (String member : nameToManager.get(groupName).getMembers()) {
					if (member.equals(message.getSource())) {
						isValidMember = true;
						break;
					}
				}
				if (isValidMember) {
					nameToManager.get(groupName).send(message, this.messagePasser);
				} else {
					System.out.println("user (" + message.getSource()
							+ ") does not belong to this group");
				}
			} else {
				System.out.println("group name (" + groupName + ") does not exist");
			}
		}
	}
	
	public MulticastMessage receive() {
		// receive a message from deliverQueue
		// acquire lock here
		try {
			return this.deliverQueue.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}
	}

	class Receiver implements Runnable {

		@Override
		public void run() {
			while (true) {
				MulticastMessage message = (MulticastMessage) messagePasser
						.receive();
				System.out
						.println("CORMulticast - message received from MessagePasser - " + message);
				if (message.getType() == Type.UNICAST) {
					try {
						deliverQueue.put(message);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					String groupName = message.getGroupName();
					GroupManager gm = nameToManager.containsKey(groupName) ? nameToManager
							.get(groupName) : null;
					if (gm != null) {
						gm.checkReliabilityQueue(message, messagePasser);
					}
				}
			}
		}

	}

	class StatusChecker implements Runnable {

		@Override
		public void run() {
			while (true) {
				for (GroupManager gm : groupManagers) {
					gm.checkReceivedMessage(deliverQueue);
					gm.checkTimeOut(timeout, messagePasser);
				}
			}
		}

	}
}
