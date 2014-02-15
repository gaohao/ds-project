package utils;

import ipc.Contact;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import org.yaml.snakeyaml.Yaml;

import clock.ClockService;

/**
 * This class is used to retrieve and parse YAML configuration file.
 * 
 * @author Hao Gao
 * @author Yinsu Chu
 * 
 */
public class ConfigurationParser {

	/**
	 * This class is used to pass back parsed configuration.
	 * 
	 * @author Hao Gao
	 * @author Yinsu Chu
	 * 
	 */
	public class ConfigInfo {
		private HashMap<String, Contact> contactMap;
		private ClockService.ClockType type;
		private ArrayList<HashMap<String, Object>> sendRules;
		private ArrayList<HashMap<String, Object>> receiveRules;
		private int localNodeId;
		private ArrayList<HashMap<String, Object>> groups;

		public ConfigInfo(HashMap<String, Contact> contactMap,
				ClockService.ClockType type,
				ArrayList<HashMap<String, Object>> sendRules,
				ArrayList<HashMap<String, Object>> receiveRules,
				int localNodeId, ArrayList<HashMap<String, Object>> groups) {
			this.contactMap = contactMap;
			this.type = type;
			this.sendRules = sendRules;
			this.receiveRules = receiveRules;
			this.localNodeId = localNodeId;
			this.groups = groups;
		}

		public HashMap<String, Contact> getContactMap() {
			return contactMap;
		}

		public ClockService.ClockType getType() {
			return type;
		}

		public ArrayList<HashMap<String, Object>> getSendRules() {
			return sendRules;
		}

		public ArrayList<HashMap<String, Object>> getReceiveRules() {
			return receiveRules;
		}

		public int getLocalNodeId() {
			return localNodeId;
		}

		public ArrayList<HashMap<String, Object>> getGroups() {
			return groups;
		}
	}

	// constants in the configuration file
	private static final String ITEM_CONFIGURATION = "configuration";
	private static final String CONTACT_NAME = "name";
	private static final String CONTACT_IP = "ip";
	private static final String CONTACT_PORT = "port";
	private static final String CLOCK_SERVICE_TYPE = "clockService";
	private static final String CLOCK_SERVICE_LOGICAL = "logical";
	private static final String CLOCK_SERVICE_VECTOR = "vector";
	private static final String ITEM_SEND_RULES = "sendRules";
	private static final String ITEM_RECEIVE_RULES = "receiveRules";
	private static final String ITEM_GROUP = "groups";

	private String ETag;
	private HttpURLConnection connection;

	public ConfigurationParser() {
		this.ETag = "initial";
		this.connection = null;
	}

	/**
	 * Download configuration file at a given URL and save to a given file name.
	 * 
	 * @param serverConfigurationFileName
	 *            The configuration file name on Dropbox.
	 * @param localConfigurationFileName
	 *            The local file name to save to.
	 * @return True on downloading new configuration file, false otherwise (only
	 *         download the file if ETag has changed).
	 */
	public boolean downloadConfigurationFile(
			String serverConfigurationFileName,
			String localConfigurationFileName) {
		String configurationFileURL = "https://dl.dropboxusercontent.com/s/x5okbj3cwyj3t1c/"
				+ serverConfigurationFileName + "?dl=1";
		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;
		boolean configChanged = false;
		try {
			URL url = new URL(configurationFileURL);
			connection = (HttpURLConnection) url.openConnection();

			// we use ETag to check whether the configuration file has changed
			connection.setRequestProperty("If-None-Match", ETag);

			int responseCode = connection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				InputStream is = connection.getInputStream();
				bis = new BufferedInputStream(is);
				FileOutputStream fos = new FileOutputStream(
						localConfigurationFileName);
				bos = new BufferedOutputStream(fos);
				int input = 0;
				while ((input = bis.read()) != -1) {
					bos.write(input);
				}
				bos.flush();
				ETag = connection.getHeaderField("ETag");
				configChanged = true;
			} else if (responseCode != HttpURLConnection.HTTP_NOT_MODIFIED) {
				System.out
						.println("unexpected HTTP responce code when downloading configuration file - "
								+ responseCode);
			}
		} catch (Exception ex) {
			System.out.println("failed to download configuration file - "
					+ ex.getMessage());
		} finally {
			if (bos != null) {
				try {
					bos.close();
				} catch (IOException ex) {
				}
			}
			if (bis != null) {
				try {
					bis.close();
				} catch (IOException ex) {
				}
			}
			if (connection != null) {
				connection.disconnect();
				connection = null;
			}
		}
		return configChanged;
	}

	/**
	 * Parse a given configuration file.
	 * 
	 * @param configurationFile
	 *            The name of the configuration file.
	 * @param firstLoad
	 *            Whether this is the first load (some items, e.g. other nodes'
	 *            information and clock service, will be parsed only at the
	 *            first load).
	 * @param localName
	 *            The name of the local node.
	 * @return Parsed configuration information, null otherwise (e.g. in some
	 *         illegal situation, such as localName does not exist in the
	 *         configuration file).
	 */
	@SuppressWarnings("unchecked")
	public ConfigInfo yamlExtraction(String configurationFileName,
			boolean firstLoad, String localName) {
		Yaml yaml = new Yaml();
		InputStream is = null;

		try {
			is = new FileInputStream(configurationFileName);
		} catch (IOException ex) {
			System.out.println("failed to open configuration file - "
					+ ex.getMessage());
		}

		HashMap<String, ArrayList<HashMap<String, Object>>> yamlMap = (HashMap<String, ArrayList<HashMap<String, Object>>>) (yaml
				.load(is));

		HashMap<String, Contact> contactMap = new HashMap<String, Contact>();
		ClockService.ClockType type = ClockService.ClockType.DEFAULT;
		ArrayList<HashMap<String, Object>> sendRules = new ArrayList<HashMap<String, Object>>();
		ArrayList<HashMap<String, Object>> receiveRules = new ArrayList<HashMap<String, Object>>();
		int localNodeId = 0;
		ArrayList<HashMap<String, Object>> groups = new ArrayList<HashMap<String, Object>>();

		for (Map.Entry<String, ArrayList<HashMap<String, Object>>> entry : yamlMap
				.entrySet()) {
			if (entry.getKey().equals(ITEM_CONFIGURATION) && firstLoad) {
				for (HashMap<String, Object> map : entry.getValue()) {
					if (map.containsKey(CLOCK_SERVICE_TYPE)) {
						String service = (String) map.get(CLOCK_SERVICE_TYPE);
						if (service.equals(CLOCK_SERVICE_LOGICAL)) {
							type = ClockService.ClockType.LOGICAL;
							System.out.println("clock service: logical");
						} else if (service.equals(CLOCK_SERVICE_VECTOR)) {
							type = ClockService.ClockType.VECTOR;
							System.out.println("clock service: vector");
						} else {
							System.out.println("invalid clock service type");
						}
					} else {
						String name = (String) map.get(CONTACT_NAME);
						String IP = (String) map.get(CONTACT_IP);
						Integer port = (Integer) map.get(CONTACT_PORT);
						Contact contact = new Contact(IP, (int) port);
						contactMap.put(name, contact);
					}
				}
				if (!contactMap.containsKey(localName)) {
					System.out.println("local name " + localName
							+ " does not exist in the configuration file");
					return null;
				}
				// calculate local node ID
				PriorityQueue<String> heap = new PriorityQueue<String>();
				for (String s : contactMap.keySet()) {
					heap.add(s);
				}
				while (!heap.isEmpty()) {
					String name = heap.poll();
					if (name.equals(localName)) {
						break;
					} else {
						localNodeId++;
					}
				}
				System.out.println("local node ID: " + localNodeId);
				System.out.println("total number of nodes: "
						+ contactMap.size());
			} else if (entry.getKey().equals(ITEM_GROUP)) {
				groups = entry.getValue();
			} else if (entry.getKey().equals(ITEM_SEND_RULES)) {
				sendRules = entry.getValue();
			} else if (entry.getKey().equals(ITEM_RECEIVE_RULES)) {
				receiveRules = entry.getValue();
			}
		}
		try {
			is.close();
		} catch (IOException ex) {
		}

		return new ConfigInfo(contactMap, type, sendRules, receiveRules,
				localNodeId, groups);
	}
}
