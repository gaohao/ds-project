package ipc;

/**
 * A class to store remote node information.
 * 
 * @author Hao Gao
 * @author Yinsu Chu
 * 
 */
public class Contact {
	private String IP;
	private int port;

	public Contact(String IP, int port) {
		this.IP = IP;
		this.port = port;
	}

	protected String getIP() {
		return IP;
	}

	protected void setIP(String IP) {
		this.IP = IP;
	}

	protected int getPort() {
		return port;
	}

	protected void setPort(int port) {
		this.port = port;
	}
}
