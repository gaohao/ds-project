package multicast;

import ipc.MessagePasser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import multicast.MulticastMessage.Type;

public class GroupManager {
	private String name; // groupName
	private String localName; // node name
	private LinkedBlockingQueue<RQueueElement> reliabilityQueue;
	private LinkedBlockingQueue<MulticastMessage> casualOrderingQueue;
	private ArrayList<String> members;
	private HashMap<String, Integer> memberNameToId;
	private int[] seqVector;
	private int sendCounter;

	private final ReentrantLock lockForReliabilityQueue = new ReentrantLock();
	private final ReentrantLock lockForcCasualOrderingQueue = new ReentrantLock();

	public GroupManager(String localName, String name, int id,
			ArrayList<String> members, int[] seqVector,
			HashMap<String, Integer> memberNameToId) {
		this.name = name;
		this.localName = localName;
		this.reliabilityQueue = new LinkedBlockingQueue<RQueueElement>();
		this.casualOrderingQueue = new LinkedBlockingQueue<MulticastMessage>();
		this.seqVector = seqVector;
		this.sendCounter = 0;
		this.members = members;
		this.memberNameToId = memberNameToId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public LinkedBlockingQueue<RQueueElement> getReliabilityQueue() {
		return reliabilityQueue;
	}

	public void setReliabilityQueue(
			LinkedBlockingQueue<RQueueElement> reliabilityQueue) {
		this.reliabilityQueue = reliabilityQueue;
	}

	public LinkedBlockingQueue<MulticastMessage> getCasualOrderingQueue() {
		return casualOrderingQueue;
	}

	public void setCasualOrderingQueue(
			LinkedBlockingQueue<MulticastMessage> casualOrderingQueue) {
		this.casualOrderingQueue = casualOrderingQueue;
	}

	public void send(MulticastMessage message, MessagePasser mp) {
		// increase send counter
		sendCounter++;
		int[] copyOfVector = Arrays.copyOf(seqVector, seqVector.length);
		copyOfVector[memberNameToId.get(localName)] = sendCounter;
		message.setSeqVector(copyOfVector);

		MulticastMessage originalMessage = new MulticastMessage(message);
		HashSet<String> remainingNodes = new HashSet<String>();

		for (String m : members) {
			if (!m.equals(localName)) {
				remainingNodes.add(m);
			}
		}

		RQueueElement rqElem = new RQueueElement(remainingNodes,
				System.currentTimeMillis(), originalMessage);
		// acquire a lock here!
		try {
			lockForReliabilityQueue.lock();
			//System.out
			//		.println("message added to reliability queue - " + originalMessage);
			reliabilityQueue.put(rqElem);
			lockForReliabilityQueue.unlock();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		for (String m : members) {
			if (!m.equals(localName)) {
				message.setDest(m);
				mp.send(new MulticastMessage(message));
			}
		}
	}

	public ArrayList<String> getMembers() {
		return members;
	}

	public void setMembers(ArrayList<String> members) {
		this.members = members;
	}

	public void checkReliabilityQueue(MulticastMessage message, MessagePasser mp) {
		MulticastMessage originalMessage;
		Type type = message.getType();
		String from = message.getSource();
		if (type == Type.DATA) {
			originalMessage = message;
		} else if (type == Type.ACK || type == Type.TIMEOUT) {
			originalMessage = (MulticastMessage) message.getData();
		} else {
			// invalid message
			return;
		}

		lockForReliabilityQueue.lock();
		// check reliability queue
		RQueueElement validRQElem = null;
		for (RQueueElement rqElem : reliabilityQueue) {
			String src = rqElem.getMessage().getSource();
			int[] vector = rqElem.getMessage().getSeqVector();
			if (src.equals(originalMessage.getSource())
					&& Arrays.equals(vector, originalMessage.getSeqVector())) {
				// already receive the message from other node
				validRQElem = rqElem;
				break;
			}
		}

		if (validRQElem == null) {
			if (!IsAlreadyReceived(originalMessage)) {
				HashSet<String> remainingNodes = new HashSet<String>();
				for (String m : members) {
					if (!m.equals(localName)) {
						if (!m.equals(from)) {
							remainingNodes.add(m);
						}
					}
				}
				RQueueElement rqElem = new RQueueElement(remainingNodes,
						System.currentTimeMillis(), originalMessage);
				try {
					//System.out.println("message added to reliability queue - " + originalMessage);
					reliabilityQueue.put(rqElem);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				for (String m : members) {
					if (!m.equals(localName)) {
						message = new MulticastMessage(
								originalMessage.getGroupName(), localName, m,
								originalMessage.getKind(),
								new MulticastMessage(originalMessage),
								Type.ACK, null);
						mp.send(message);
					}
				}
			}

		} else {
			HashSet<String> remainingNode = validRQElem.getRemainingNodes();
			if (remainingNode.contains(from)) {
				remainingNode.remove(from);
			}
		}
		if (type == Type.TIMEOUT) {
			// receive a TIMEOUT message from source, this means source
			// did not receive the ACK
			//System.out
			//		.println("resending a message because the type of the message received is TIMEOUT");
			message = new MulticastMessage(originalMessage.getGroupName(),
					localName, from, originalMessage.getKind(),
					new MulticastMessage(originalMessage), Type.ACK, null);
			mp.send(message);
		}
		lockForReliabilityQueue.unlock();
	}

	public void checkTimeOut(long timeout, MessagePasser mp) {
		lockForReliabilityQueue.lock();
		long curTime = System.currentTimeMillis();
		for (RQueueElement rqElem : reliabilityQueue) {
			// resend
			if (curTime - rqElem.getReceivedTime() >= timeout) {
				rqElem.setReceivedTime(curTime);
				// message is organic from this node
				MulticastMessage message;
				MulticastMessage originalMessage = rqElem.getMessage();
				message = new MulticastMessage(localName,
						originalMessage.getGroupName(), "timeout",
						new MulticastMessage(originalMessage), Type.TIMEOUT);
				for (String name : rqElem.getRemainingNodes()) {
					message.setDest(name);
					mp.send(message);
					System.out.println("timeout - " + message);
				}
			}
		}
		lockForReliabilityQueue.unlock();
	}

	public int[] getSeqVector() {
		return seqVector;
	}

	public void checkReceivedMessage(
			LinkedBlockingQueue<MulticastMessage> deliverQueue) {
		lockForReliabilityQueue.lock();
		Iterator<RQueueElement> itrRQElem = reliabilityQueue.iterator();
		while (itrRQElem.hasNext()) {
			RQueueElement rqElem = itrRQElem.next();
			if (rqElem.getRemainingNodes().isEmpty()) {
				try {
					casualOrderingQueue.put(rqElem.getMessage());
					//System.out.println("message added to casuality queue - "
					//		+ rqElem.getMessage());
					itrRQElem.remove();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		lockForReliabilityQueue.unlock();
		lockForcCasualOrderingQueue.lock();
		// check casual order
		Iterator<MulticastMessage> itrMessage = casualOrderingQueue.iterator();
		while (itrMessage.hasNext()) {
			MulticastMessage message = itrMessage.next();
			int sourceMemberId = memberNameToId.get(message.getSource());
			if (checkCasualOrder(message, sourceMemberId)) {
				try {
					seqVector[sourceMemberId]++;
					deliverQueue.put(message);
					//System.out.println("message added to deliver queue - "
					//		+ message);
					itrMessage.remove();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		lockForcCasualOrderingQueue.unlock();
	}

	private boolean checkCasualOrder(MulticastMessage message,
			int sourceMemberId) {
		for (int i = 0; i < seqVector.length; i++) {
			if (i != sourceMemberId) {
				if (message.getSeqVector()[i] > seqVector[i]) {
					return false;
				}
			} else {
				if (message.getSeqVector()[i] != seqVector[i] + 1) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean IsAlreadyReceived(MulticastMessage message) {
		boolean isDeliverd = true;
		for (int i = 0; i < seqVector.length; i++) {
			if (message.getSeqVector()[i] > seqVector[i]) {
				isDeliverd = false;
				break;
			}
		}
		if (isDeliverd) {
			return true;
		} else {
			// check casual ordering queue;
			for (MulticastMessage m : casualOrderingQueue) {
				if (message.getSource().equals(m.getSource())
						&& Arrays.equals(message.getSeqVector(),
								m.getSeqVector())) {
					return true;
				}
			}
		}
		return false;
	}
}
