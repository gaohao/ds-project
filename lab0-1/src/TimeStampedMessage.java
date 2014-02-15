import java.io.Serializable;
import java.util.HashMap;


public class TimeStampedMessage extends Message implements Serializable {

	private static final long serialVersionUID = 1L;
	
	HashMap<String,TimeStamp> nameToTimeStamp;

	public TimeStampedMessage(String dest, String kind, Object data) {
		super(dest, kind, data);
		nameToTimeStamp = new HashMap<String,TimeStamp>();
	}

	public TimeStampedMessage(TimeStampedMessage msg) {
		super(msg.get_dest(), msg.get_kind(), msg.get_data());
		super.set_source(msg.get_source());
		super.set_seqNum(msg.get_seqNum());
		super.set_delayed(msg.get_delayed());
		super.set_duplicate(msg.get_dupe());
		nameToTimeStamp = new HashMap<String,TimeStamp>();
		for(String name : msg.getTimeStamp().keySet()){
			nameToTimeStamp.put(name, new TimeStamp(msg.getTimeStamp().get(name)));
		}
	}

	public HashMap<String,TimeStamp> getTimeStamp(){
		return nameToTimeStamp;
	}

	public void setTimeStamp(HashMap<String,TimeStamp> nameToTimeStamp){
		for(String name : nameToTimeStamp.keySet()){
			this.nameToTimeStamp.put(name, new TimeStamp(nameToTimeStamp.get(name)));
		}
	}

	public void addTimeStamp(String name, TimeStamp ts){
		nameToTimeStamp.put(name, ts);
	}
	
	public String toString(){
		return super.toString() + " TimeStamp: " + nameToTimeStamp.toString();
	}
	
	public String fullMsg(){
		return ("src=" + super.get_source() + " dst=" + super.get_dest() + " ts=" + nameToTimeStamp.toString() + " msg= " + super.get_data());
	}
	
	public Boolean isGreater(TimeStampedMessage tsm) {
		for(String name : nameToTimeStamp.keySet()){
			if(nameToTimeStamp.get(name).isLesser(tsm.getTimeStamp().get(name)))
				return false;
		}
		return true;
	}
	public Boolean isLesser(TimeStampedMessage tsm) {
		for(String name : nameToTimeStamp.keySet()){
			if(nameToTimeStamp.get(name).isGreater(tsm.getTimeStamp().get(name)))
				return false;
		}
		return true;
	}
	public Boolean isEqual(TimeStampedMessage tsm) {
		for(String name : nameToTimeStamp.keySet()){
			if(nameToTimeStamp.get(name).isEqual(tsm.getTimeStamp().get(name)))
				return false;
		}
		return true;
	}
}
