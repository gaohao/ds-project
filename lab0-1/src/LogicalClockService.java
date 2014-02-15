public class LogicalClockService extends ClockService {

	public LogicalClockService(TimeStamp ts){
		super.ts = ts;
		super.clockType = "logical";
	}
	
	@Override
	public TimeStamp getTimeStamp() {
		return super.ts;
	}

	@Override
	public void setTimeStamp(TimeStamp ts) {
		super.ts = ts;
	}

	@Override
	public void increaseTimeByOne(){
		super.ts.increaseTimeByOne();
	}

	@Override
	public void increaseTime(int offset) {
		super.ts.increaseTime(offset);
	}

	@Override
	public void setTime(int t) {
		super.ts.setTime(t);
	}
	
}
