package app;

import ipc.MessagePasser;

import java.util.Scanner;

import me.MutualExclusion;
import multicast.CORMulticast;
import multicast.MulticastMessage;
import utils.ConfigurationParser;
import utils.ConfigurationParser.ConfigInfo;
import clock.ClockService;

/**
 * This class demonstrates the multicast infrastructure by providing an
 * interactive command-line user interface.
 * 
 * @author Hao Gao
 * @author Yinsu Chu
 * 
 */
public class MutualExclusionControlPanel {
	private static final int NUM_CMD_ARG = 2;
	private static final String USAGE = "usage: java -cp :snakeyaml-1.11.jar app/MulticastControlPanel "
			+ "<configuration_file_name> <local_name>";

	private static final String HELP_CMD = "help";
	private static final String HELP_CONTENT = "multisend <group_name> <kind> <message>\n"
			+ "quit - quit this program";

	private static final String MULTISEND_CMD = "multisend";
	private static final int MULTISEND_NUM_PARAM = 4;
	private static final String QUIT_CMD = "quit";
	private static final String ACQUIRE_CMD = "acquire";
	private static final String RELEASE_CMD = "release";

	private CORMulticast multicastService;
	private MessagePasser messagePasser;

	private Receiver receiver;
	private Thread receiverThread;
	private MutualExclusion me;

	/**
	 * This class is used by MulticastControlPanel to wait on receive buffer for
	 * the next message and print delivered messages to console.
	 * 
	 * @author Hao Gao
	 * @author Yinsu Chu
	 * 
	 */
	private class Receiver implements Runnable {
		public void run() {
			while (true) {
				MulticastMessage message = multicastService.receive();
				System.out.println("message delivered to local node - " + message.toString());
			}
		}
	}

	/**
	 * This method launches a simple command-line user interface to operate on
	 * the multicast infrastructure.
	 * 
	 * @param configurationFileName
	 *            Name of the configuration file on Dropbox.
	 * @param localName
	 *            Name of the local node.
	 */
	public void startUserInterface(String configurationFileName, String localName) {

		// retrieve configurations
		ConfigurationParser cp = new ConfigurationParser();
		if (!cp.downloadConfigurationFile(configurationFileName, configurationFileName)) {
			System.exit(-1);
		}
		ConfigInfo ci = cp.yamlExtraction(configurationFileName, true, localName);
		if (ci == null) {
			System.exit(-1);
		}

		messagePasser = new MessagePasser(configurationFileName, localName, ci, cp);
		// create CORMulticast and ClockService instances
		ClockService.initialize(ci.getContactMap().size(), ci.getType(), ci.getLocalNodeId());
//		multicastService = new CORMulticast(configurationFileName, localName, ci, cp);
		
		receiver = new Receiver();
		receiverThread = new Thread(receiver);
		receiverThread.start();
		
		me = new MutualExclusion(configurationFileName, localName, ci, cp);

		Scanner scanner = new Scanner(System.in);
		while (true) {
			System.out.print("DS_Lab2(" + localName + ")>> ");
			String cmd = scanner.nextLine();
			if (cmd.equals(HELP_CMD)) {
				System.out.println(HELP_CONTENT);
			} else if (cmd.startsWith(MULTISEND_CMD)) {
				String[] parsedLine = cmd.split("\\s+", MULTISEND_NUM_PARAM);
				if (parsedLine.length == MULTISEND_NUM_PARAM && parsedLine[0].equals(MULTISEND_CMD)) {
					MulticastMessage message = new MulticastMessage(localName, parsedLine[1], parsedLine[2], parsedLine[3], MulticastMessage.Type.DATA);
					multicastService.send(message);
				} else {
					System.out.println("invalid command");
				}
			} else if (cmd.equals(QUIT_CMD)) {
				scanner.close();
				System.exit(-1);
			} else if (cmd.equals(ACQUIRE_CMD)) {
				me.rquestCS();
			} else if (cmd.equals(RELEASE_CMD)) {
				me.releaseCS();
			}
			
			if (!receiverThread.isAlive()) {
				System.out.println("MulticastControlPanel health check: receiver thread died");
			}
		}
	}

	public static void main(String[] args) {
		if (args.length != NUM_CMD_ARG) {
			System.out.println(USAGE);
			System.exit(-1);
		}
		MutualExclusionControlPanel mecp = new MutualExclusionControlPanel();
		mecp.startUserInterface(args[0], args[1]);
	}
}
