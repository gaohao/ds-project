import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;


public class MessageLogger {
	//can create messages (Message) and call MessagePasser.send
	//     will set dst, src, kind, data
	//can also call receive method to get anything on MessagePasser's receive buffer
	private static String local_name, config_file;
	private static ClockService clock;
	public static void main(String args[]){
		try {
			System.out.println("Message Logger Testing Interface");		
			local_name = "logger";
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("Please specify configuration file location:");		
			config_file = br.readLine();
			
			System.out.println("Please select a clock type: logical / vector:");
			String clockType = br.readLine();
			if (clockType.equals("logical")) {
				clock = new LogicalClockService(new TimeStamp(local_name, 0));
			} else if(clockType.equals("vector")) {
				clock = new VectorClockService();
				clock.setTimeStamp(new TimeStamp(local_name, 0));
			} else{
				System.out.println("You have entered in an invalid clock type. Please start over.");
				System.exit(1);
			}
			
			System.out.println("Message Logger started:");

			ArrayList<TimeStampedMessage> logs = new ArrayList<TimeStampedMessage>();
			String selection;
			TimeStampedMessage msg;

			//Now that we have the config filename and local name, we can instantiate our MessagePasser
			MessagePasser mp = new MessagePasser(config_file, local_name, clock);
			//Now to prompt the user for what to do
			while(true){
				System.out.println("Please input a number for your selection (1-2):\n"
						+ "1. View Logs\n2. Exit");
				try {
					selection = br.readLine();
					if(selection.equals("1")){
						//Pull everything out of the incoming buffer so we can analyze it
						while( (msg = mp.receive()) != null ){
							logs.add(msg);
						}
						showLogs(logs);
					}
					else if(selection.equals("2")){
						System.out.println("Goodbye.");
						System.exit(1);
					}
					else{
						System.out.println("Sorry, I don't understand that command. Please try again.");
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		} catch (IOException e) {
			//e.printStackTrace();
			System.out.println("I don't understand your input. Please try again.");
		}
	}

	public static void showLogs(ArrayList<TimeStampedMessage> logs){
		TimeStampedMessage msg1, msg2;
		if(logs.size() == 1){
			System.out.println("Num 0; " + logs.get(0).getTimeStamp() + "; logged from "+ logs.get(0).get_source() + "; msg { " + ((TimeStampedMessage)logs.get(0).get_data()).fullMsg() + " }");
		} else {
			for(int i = 0; i < logs.size() - 1; i++){
				msg1 = (TimeStampedMessage)logs.get(i).get_data();
				for(int j = i + 1; j < logs.size(); j++){
					msg2 = (TimeStampedMessage)logs.get(j).get_data();
					if (msg1.isGreater(msg2)) {
						System.out.println("Num " + j + "; " + msg2.getTimeStamp() + "; logged from "+ msg2.get_source() + "; msg { " + msg2.fullMsg() + " }");
						System.out.println("->");
						System.out.println("Num " + i + "; " + msg1.getTimeStamp() + "; logged from "+ msg1.get_source() + "; msg { " + msg1.fullMsg() + " }");
						System.out.println("");
					} else if (msg1.isLesser(msg2)) {
						System.out.println("Num " + i + "; " + msg1.getTimeStamp() + "; logged from "+ msg1.get_source() + "; msg { " + msg1.fullMsg() + " }");
						System.out.println("->");
						System.out.println("Num " + j + "; " + msg2.getTimeStamp() + "; logged from "+ msg2.get_source() + "; msg { " + msg2.fullMsg() + " }");
						System.out.println("");
					} else if (msg1.isEqual(msg2)) {
						System.out.println("Num " + i + "; " + msg1.getTimeStamp() + "; logged from "+ msg1.get_source() + "; msg { " + msg1.fullMsg() + " }");
						System.out.println("==");
						System.out.println("Num " + j + "; " + msg2.getTimeStamp() + "; logged from "+ msg2.get_source() + "; msg { " + msg2.fullMsg() + " }");
						System.out.println("");
					} else{
						System.out.println("Num " + i + "; " + msg1.getTimeStamp() + "; logged from "+ msg1.get_source() + "; msg { " + msg1.fullMsg() + " }");
						System.out.println("||");
						System.out.println("Num " + j + "; " + msg2.getTimeStamp() + "; logged from "+ msg2.get_source() + "; msg { " + msg2.fullMsg() + " }");
						System.out.println("");
					}
				}
			}
		}
	}
}
