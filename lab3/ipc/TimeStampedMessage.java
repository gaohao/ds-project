package ipc;

import clock.TimeStamp;

public class TimeStampedMessage extends Message implements
		Comparable<TimeStampedMessage> {
	private static final long serialVersionUID = -5908777442179653889L;
	private TimeStamp timeStamp;

	public TimeStampedMessage(String kind, Object data) {
		super(kind, data);
	}

	public TimeStampedMessage(String dest, String kind, Object data) {
		super(dest, kind, data);
		this.timeStamp = null;
	}

	public TimeStampedMessage(String src, String dest, String kind, Object data) {
		super(src, dest, kind, data);
		this.timeStamp = null;
	}

	public TimeStampedMessage(TimeStampedMessage message) {
		super(message);
		this.timeStamp = new TimeStamp(message.timeStamp);
	}

	public TimeStamp getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(TimeStamp timeStamp) {
		this.timeStamp = timeStamp;
	}

	@Override
	public String toString() {
		return "[src] " + getSource() + " [dst] " + getDest() + " [kind] "
				+ getKind() + " [seq] " + getSequenceNumber() + " [dup] "
				+ (isDupe() ? "true" : "false") + " [time] "
				+ timeStamp.toString() + " [data] " + getData().toString();
	}

	@Override
	public int compareTo(TimeStampedMessage tsm) {
		return this.timeStamp.compareTo(tsm.timeStamp);
	}
}
