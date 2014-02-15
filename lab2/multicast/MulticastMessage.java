package multicast;

import ipc.TimeStampedMessage;

import java.util.Arrays;

public class MulticastMessage extends TimeStampedMessage {
	private static final long serialVersionUID = -6266905058526960435L;

	public enum Type {
		DATA, ACK, TIMEOUT
	}

	// which group this message belongs to
	private String groupName;

	// message type
	// 1) DATA: original multicast data
	// 2) ACK: the first ack with data
	// 3) INQUIRE: to inquire a missing data/ack
	// 4) CONFIRM: confirm the reception of a data/ack
	private Type type;

	// for casual ordering
	private int[] seqVector;

	public MulticastMessage(String source, String groupName, String kind,
			Object data, Type type) {
		super(kind, data);
		setSource(source);
		this.groupName = groupName;
		this.type = type;
		this.seqVector = null;
	}

	public MulticastMessage(String groupName, String src, String dest,
			String kind, Object data, Type type, int[] seqVector) {
		super(src, dest, kind, data);
		this.groupName = groupName;
		this.type = type;
		this.seqVector = seqVector;
	}

	public MulticastMessage(MulticastMessage message) {
		super(message);
		this.groupName = message.groupName;
		this.seqVector = message.seqVector == null ? null : message.seqVector
				.clone();
		this.type = message.type;
	}

	@Override
	public String toString() {
		return "[src] " + getSource() + " [dst] " + getDest() + " [kind] "
				+ getKind() + " [seq] " + getSequenceNumber() + " [dup] "
				+ (isDupe() ? "true" : "false") + " [multicast_seq_vector] "
				+ Arrays.toString(seqVector) + " [data] "
				+ getData().toString();
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public int[] getSeqVector() {
		return seqVector;
	}

	public void setSeqVector(int[] seqVector) {
		this.seqVector = seqVector;
	}
}
