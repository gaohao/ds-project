package app;

import ipc.MessagePasser;
import ipc.TimeStampedMessage;

import java.util.Scanner;

import utils.ConfigurationParser;
import utils.ConfigurationParser.ConfigInfo;
import clock.ClockService;
import clock.TimeStamp;

/**
 * This class demonstrates the communication infrastructure with MessagePasser
 * by providing an interactive command-line user interface.
 * 
 * @author Hao Gao
 * @author Yinsu Chu
 * 
 */
public class ControlPanel {
	private static final int NUM_CMD_ARG = 2;
	private static final String USAGE = "usage: java -cp :snakeyaml-1.11.jar app/ControlPanel "
			+ "<configuration_file_name> <local_name>";

	private static final String HELP_CMD = "help";
	private static final String HELP_CONTENT = "send <process_name> <kind> <message>\n"
			+ "event - advance local clock by one\n"
			+ "time - display local clock\n" + "quit - quit this program";

	private static final String SEND_CMD = "send";
	private static final int SEND_NUM_PARAM = 4;
	private static final String EVENT_CMD = "event";
	private static final String TIME_CMD = "time";
	private static final String QUIT_CMD = "quit";

	private MessagePasser messagePasser;

	private Receiver receiver;
	private Thread receiverThread;

	/**
	 * This class is used by ControlPanel to wait on receive buffer for the next
	 * message and print delivered messages to console.
	 * 
	 * @author Hao Gao
	 * @author Yinsu Chu
	 * 
	 */
	private class Receiver implements Runnable {
		public void run() {
			while (true) {
				TimeStampedMessage message = messagePasser.receive();
				System.out.println("message delivered to local node - "
						+ message.toString());
			}
		}
	}

	/**
	 * This method launches a simple command-line user interface to operate on
	 * the communication infrastructure.
	 * 
	 * @param configurationFileName
	 *            Name of the configuration file on Dropbox.
	 * @param localName
	 *            Name of the local node.
	 * 
	 */
	public void startUserInterface(String configurationFileName,
			String localName) {

		// retrieve configurations
		ConfigurationParser cp = new ConfigurationParser();
		if (!cp.downloadConfigurationFile(configurationFileName,
				configurationFileName)) {
			System.exit(-1);
		}
		ConfigInfo ci = cp.yamlExtraction(configurationFileName, true,
				localName);
		if (ci == null) {
			System.exit(-1);
		}

		// create MessagePasser and ClockService instances
		ClockService.initialize(ci.getContactMap().size(), ci.getType(),
				ci.getLocalNodeId());
		messagePasser = new MessagePasser(configurationFileName, localName, ci,
				cp);

		receiver = new Receiver();
		receiverThread = new Thread(receiver);
		receiverThread.start();

		Scanner scanner = new Scanner(System.in);
		while (true) {
			System.out.print("DS_Lab2>> ");
			String cmd = scanner.nextLine();
			if (cmd.equals(HELP_CMD)) {
				System.out.println(HELP_CONTENT);
			} else if (cmd.startsWith(SEND_CMD)) {
				String[] parsedLine = cmd.split("\\s+", SEND_NUM_PARAM);
				if (parsedLine.length == SEND_NUM_PARAM
						&& parsedLine[0].equals(SEND_CMD)) {
					TimeStampedMessage message = new TimeStampedMessage(
							parsedLine[1], parsedLine[2], parsedLine[3]);
					TimeStamp ts = messagePasser.send(message);
					System.out
							.println("message put to send buffer, local time updated to: "
									+ ts.toString());
				} else {
					System.out.println("invalid command");
				}
			} else if (cmd.equals(EVENT_CMD)) {
				TimeStamp ts = ClockService.getInstance().updateLocalTime();
				System.out.println("local time updated to: " + ts.toString());
			} else if (cmd.equals(TIME_CMD)) {
				TimeStamp ts = ClockService.getInstance().getLocalTimeCopy();
				System.out.println("local time: " + ts.toString());
			} else if (cmd.equals(QUIT_CMD)) {
				scanner.close();
				System.exit(-1);
			}
			if (!receiverThread.isAlive()) {
				System.out
						.println("ControlPanel health check: receiver thread died");
			}
		}
	}

	public static void main(String[] args) {
		if (args.length != NUM_CMD_ARG) {
			System.out.println(USAGE);
			System.exit(-1);
		}
		ControlPanel cp = new ControlPanel();
		cp.startUserInterface(args[0], args[1]);
	}
}
