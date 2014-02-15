import java.util.HashMap;


public class VectorClockService extends ClockService {

	HashMap<String,TimeStamp> vectorClocks;

	VectorClockService(){
		 vectorClocks = new HashMap<String,TimeStamp>();
		 super.clockType = "vector";
	}

	@Override
	public TimeStamp getTimeStamp() {
		return super.ts;
	}

	@Override
	public void setTimeStamp(TimeStamp myTime) {
		super.ts = myTime;
		vectorClocks.put(super.ts.getProcName(), myTime);
	}
	
	public TimeStamp getTimeStamp(String procName){
		return vectorClocks.get(procName);
	}
	
	public void updateTimeStamp(String procName, int time){
		vectorClocks.put(procName, new TimeStamp(procName, time));
	}
	
	public HashMap<String,TimeStamp> getVectorClock(){
		return vectorClocks;
	}
	
	@Override
	public void increaseTimeByOne(){
		super.ts.increaseTimeByOne();
		vectorClocks.put(super.ts.getProcName(), super.ts);
	}

	@Override
	public void increaseTime(int offset) {
		super.ts.increaseTime(offset);
		vectorClocks.put(super.ts.getProcName(), super.ts);
	}

	@Override
	public void setTime(int t) {
		super.ts.setTime(t);;
		vectorClocks.put(super.ts.getProcName(), new TimeStamp(ts));
	}

}