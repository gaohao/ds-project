import java.io.Serializable;


public class TimeStamp implements Serializable{

	private static final long serialVersionUID = 1L;
	private String procName;
	private int time;

	public TimeStamp(String procName , int time){
		this.procName = procName;
		this.time = time;
	}

	public TimeStamp(TimeStamp ts) {
		this.procName = ts.procName;
		this.time = ts.time;
	}

	public String getProcName() {
		return procName;
	}
	
	public void setProcName(String procName) {
		this.procName = procName;
	}
	
	public int getTime() {
		return time;
	}
	
	public void setTime(int time) {
		this.time = time;
	}
	
	public void increaseTimeByOne(){
		time++;
	}
	
	public void increaseTime(int offset){
		assert offset > 0;
		time += offset;
	}
	
	public Boolean isEqual(TimeStamp ts){
		return ts.time == this.time;
	}

	public Boolean isGreater(TimeStamp ts){

		return this.time > ts.time;
	}

	public Boolean isLesser(TimeStamp ts){
		return !isGreater(ts);
	}
	
	public String toString(){
		return Integer.toString(time);
	}
}
