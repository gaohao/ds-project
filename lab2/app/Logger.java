package app;

import ipc.MessagePasser;
import ipc.TimeStampedMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;

import utils.ConfigurationParser;
import utils.ConfigurationParser.ConfigInfo;
import clock.ClockService;
import clock.TimeStamp;

/**
 * This class demonstrates the centralized logging facility of the system.
 * 
 * @author Hao Gao
 * @author Yinsu Chu
 * 
 */
public class Logger {
	private static final int NUM_CMD_ARG = 2;
	private static final String USAGE = "usage: java -cp :snakeyaml-1.11.jar app/Logger "
			+ "<configuration_file_name> <local_name>";

	private static final String HELP_CMD = "help";
	private static final String HELP_CONTENT = "dump - display message relationships and order the messages\n"
			+ "quit - quit this program";

	private static final String DUMP_CMD = "dump";
	private static final String QUIT_CMD = "quit";

	private MessagePasser messagePasser;

	private ReentrantLock msgLock;
	private ArrayList<TimeStampedMessage> allMsg;

	private class LoggerWorker implements Runnable {
		public LoggerWorker() {
			allMsg = new ArrayList<TimeStampedMessage>();
			msgLock = new ReentrantLock();
		}

		public void run() {
			while (true) {
				TimeStampedMessage tsm = messagePasser.receive();
				msgLock.lock();
				allMsg.add(tsm);
				msgLock.unlock();
			}
		}
	}

	/**
	 * This method launches a simple command-line user interface to operate on
	 * the centralized logging facility.
	 * 
	 * @param configurationFileName
	 *            Name of the configuration file on Dropbox.
	 * @param logName
	 *            Name of the local node.
	 * 
	 */
	public void startLogger(String configurationFileName, String localName) {
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

		ClockService.initialize(ci.getContactMap().size(), ci.getType(),
				ci.getLocalNodeId());
		messagePasser = new MessagePasser(configurationFileName, localName, ci,
				cp);

		LoggerWorker lw = new LoggerWorker();
		Thread lwThread = new Thread(lw);
		lwThread.start();

		Scanner scanner = new Scanner(System.in);
		while (true) {
			System.out.print("Logger>> ");
			String cmd = scanner.nextLine();
			if (cmd.equals(HELP_CMD)) {
				System.out.println(HELP_CONTENT);
			} else if (cmd.equals(DUMP_CMD)) {
				msgLock.lock();
				if (allMsg.isEmpty()) {
					msgLock.unlock();
					continue;
				}
				System.out.println("*** Message Relationships ***");
				for (int i = 0; i < allMsg.size() - 1; i++) {
					for (int j = i + 1; j < allMsg.size(); j++) {
						TimeStampedMessage m1 = allMsg.get(i);
						TimeStampedMessage m2 = allMsg.get(j);
						TimeStamp.RelationShip r = m1.getTimeStamp().compare(
								m2.getTimeStamp());
						System.out
								.println(m1.toString() + " *"
										+ r.name().toUpperCase() + "* "
										+ m2.toString());
					}
				}
				System.out.println("*** Ordered Messages ***");
				Collections.sort(allMsg);
				TimeStampedMessage prev = allMsg.get(0);
				System.out.print(prev.toString());
				for (int i = 1; i < allMsg.size(); i++) {
					TimeStampedMessage curr = allMsg.get(i);
					if (curr.compareTo(prev) == 0) {
						System.out.print(" || " + curr.toString());
					} else if (curr.compareTo(prev) != 0) {
						System.out.print("\n" + curr.toString());
					}
					prev = curr;
				}
				System.out.println();
				msgLock.unlock();
			} else if (cmd.equals(QUIT_CMD)) {
				scanner.close();
				System.exit(-1);
			}
		}
	}

	public static void main(String[] args) {
		if (args.length != NUM_CMD_ARG) {
			System.out.println(USAGE);
			System.exit(-1);
		}
		Logger log = new Logger();
		log.startLogger(args[0], args[1]);
	}
}
