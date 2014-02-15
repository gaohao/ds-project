package ipc;

import java.io.Serializable;

/**
 * 
 * This class holds all necessary information that needs to be transmitted to
 * the destination.
 * 
 * @author Hao Gao
 * @author Yinsu Chu
 * 
 */
public class Message implements Serializable {
	private static final long serialVersionUID = -7606425083609686258L;
	private String dest;
	private String kind;
	private Object data;
	private String src;
	private int sequenceNumber;
	private boolean dupe;

	public Message(String kind, Object data) {
		this.kind = kind;
		this.data = data;
	}

	public Message(String dest, String kind, Object data) {
		this.dest = dest;
		this.kind = kind;
		this.data = data;
	}

	public Message(String src, String dest, String kind, Object data) {
		this.src = src;
		this.dest = dest;
		this.kind = kind;
		this.data = data;
	}

	public Message(Message message) {
		this.dest = message.dest;
		this.kind = message.kind;
		this.data = message.data;
		this.src = message.src;
		this.sequenceNumber = message.sequenceNumber;
		this.dupe = message.dupe;
	}

	@Override
	public String toString() {
		return "[src] " + src + " [dst] " + dest + " [kind] " + kind
				+ " [seq] " + sequenceNumber + " [dup] "
				+ (dupe ? "true" : "false") + " [data] " + data.toString();
	}

	public String getDest() {
		return dest;
	}

	public void setDest(String dest) {
		this.dest = dest;
	}

	public String getKind() {
		return kind;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public String getSource() {
		return src;
	}

	public void setSource(String source) {
		this.src = source;
	}

	public int getSequenceNumber() {
		return sequenceNumber;
	}

	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public boolean isDupe() {
		return dupe;
	}

	public void setDupe(boolean dupe) {
		this.dupe = dupe;
	}

}
