package multicast;

import java.util.HashSet;

/**
 * Reliability Queue Element. A received message will stay in the Reliability
 * Queue until it is ensured that every other node in the same group has
 * received the same message. This queue resembles the Hold Back Queue in the
 * lecture slides.
 * 
 * @author Hao Gao
 * @author Yinsu Chu
 * 
 */
public class RQueueElement {
	private HashSet<String> remainingNodes;
	private long receivedTime;
	private MulticastMessage message;

	public RQueueElement(HashSet<String> remainingNodes, long receivedTime,
			MulticastMessage message) {
		this.remainingNodes = remainingNodes;
		this.receivedTime = receivedTime;
		this.message = message;
	}

	public HashSet<String> getRemainingNodes() {
		return remainingNodes;
	}

	public void setRemainingNodes(HashSet<String> remainingNodes) {
		this.remainingNodes = remainingNodes;
	}

	public long getReceivedTime() {
		return receivedTime;
	}

	public void setReceivedTime(long receivedTime) {
		this.receivedTime = receivedTime;
	}

	public MulticastMessage getMessage() {
		return message;
	}

	public void setMessage(MulticastMessage message) {
		this.message = message;
	}
}
