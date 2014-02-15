import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MessagePasserTester {
	//can create messages (Message) and call MessagePasser.send
	//     will set dst, src, kind, data
	//can also call receive method to get anything on MessagePasser's receive buffer
	private static String local_name, config_file, data, dest, kind, isLogged;
	private static int selection;
	private static TimeStampedMessage receivedMsg;
	private static ClockService clock;
	public static void main(String args[]){
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("MessagePasser Testing Interface\nPlease specify configuration file location:");		
			config_file = br.readLine();
			System.out.println("Please specify your name:");
			local_name = br.readLine();
			if (local_name.equals("logger")) {
				System.out.println("Invalid user name. If you want to view logs, please use a logger");
				System.exit(1);
			}
			System.out.println("Please select a clock type: logical / vector:");
			String clockType = br.readLine();
			if (clockType.equals("logical")) {
				clock = new LogicalClockService(new TimeStamp(local_name, 0));
			}
			else if(clockType.equals("vector")){
				clock = new VectorClockService();
				clock.setTimeStamp(new TimeStamp(local_name, 0));
			} else{
				System.out.println("You have entered in an invalid clock type. Please start over.");
				System.exit(1);
			}
			//Now that we have the config filename and local name, we can instantiate our MessagePasser
			MessagePasser mp = new MessagePasser(config_file, local_name, clock);
			//Now to prompt the user for what to do
			while(true){
				try{
					System.out.println("Please input a number for your selection (1-3):\n"
							+ "1. Send Message\n2. Check Messages\n3. Call Clock Service\n4. Exit");
					selection = Integer.parseInt(br.readLine());
					if(selection == 1){
						System.out.print("Please name the destination: ");
						dest = br.readLine();
						System.out.print("Specify the kind of message: ");
						kind = br.readLine();
						System.out.print("What is the data: ");
						data = br.readLine();
						System.out.print("Do you want to log the message? (y/n)");
						isLogged = br.readLine();
						System.out.println("Processing message......");
						TimeStampedMessage msg = new TimeStampedMessage(dest, kind, data);
						mp.send(msg);
						if(isLogged.equals("y")){
							TimeStampedMessage log = new TimeStampedMessage("logger", "log", new TimeStampedMessage(msg));
							mp.send(log);
						}

					} else if(selection == 2) {
						System.out.println("Checking....");
						receivedMsg = mp.receive();
						if(receivedMsg != null){
							System.out.println("You have received the following message:\n" + receivedMsg.toString());
							System.out.println("Would you like the log the message you have received? (y/n)");
							isLogged = br.readLine();
							if(isLogged.equals("y")) {
								mp.send(new TimeStampedMessage("logger", "log", receivedMsg));
							}
						} else {
							System.out.println("You have no new messages ready at this time.");
						}
						
					} else if (selection == 3) {
						// Call clock service and increase timer 
						clock.increaseTimeByOne();
						System.out.println("The timestamp for this event is: " + clock.ts.getTime());
						
						System.out.println("Do you want to log this event? (y/n)");
						isLogged = br.readLine();
						if(isLogged.equals("y")){
							System.out.println("What is the data for this event?");
							data = br.readLine();
							dest = "logger";
							kind = "log";
							TimeStampedMessage tsm = new TimeStampedMessage(null, kind, data);
							tsm.set_source(local_name);
							if (clock.getClockType().equals("vector")) {
								tsm.setTimeStamp(((VectorClockService) clock).getVectorClock());
							} else{
								tsm.addTimeStamp("LogicalClock", clock.getTimeStamp());
							}
							mp.send(new TimeStampedMessage(dest, kind, tsm));
						}
						
					} else if(selection == 4){
						System.out.println("Thank you for sending messages with MessagePasser! Goodbye.");
						System.exit(1);
					} else{
						System.out.println("Sorry, that was not a valid command. Please try again\n");
					}
				} catch(NumberFormatException e){
					System.out.println("Sorry, you input an invalid command. Please start over.");
				}
			}
			
		} catch (IOException e) {
			//e.printStackTrace();
			System.out.println("I don't understand your input. Please try again.");
		}
	}
}
