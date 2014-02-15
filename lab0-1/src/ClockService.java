public abstract class ClockService {

	protected String clockType;
	
	TimeStamp ts ;

	public abstract TimeStamp getTimeStamp();

	public abstract void setTimeStamp(TimeStamp ts);
	
	public abstract void setTime(int t);
	
	public abstract void increaseTimeByOne();
	
	public abstract void increaseTime(int offset);

	String getClockType() {
		return clockType;
	}
}
