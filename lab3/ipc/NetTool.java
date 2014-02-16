package ipc;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Helper methods to create and destroy sockets and server sockets. They are
 * modified from Yinsu Chu and Ming Zhong's project of 15-440/640 in Fall 2013.
 * 
 * @author Ming Zhong
 * @author Yinsu Chu
 * 
 */
public class NetTool {

	/**
	 * Create a server socket.
	 * 
	 * @param IP
	 *            The address to bind.
	 * @param port
	 *            Port to listen on.
	 * @return A new server socket, null on failure.
	 */
	public static ServerSocket createServerSocket(String IP, int port) {
		ServerSocket socket = null;
		InetSocketAddress address = new InetSocketAddress(IP, port);
		try {
			socket = new ServerSocket();
			socket.bind(address);
		} catch (Exception ex) {
			if (socket != null) {
				try {
					socket.close();
				} catch (Exception nestedEx) {
				}
			}
			return null;
		}
		return socket;
	}

	/**
	 * Create socket with remote host.
	 * 
	 * @param IP
	 *            IP address of the remote host.
	 * @param port
	 *            Port number to create the socket to.
	 * @return The socket to the remote host, null on failure.
	 */
	public static Socket createSocket(String IP, int port) {
		Socket socket = null;
		try {
			socket = new Socket(IP, port);
		} catch (Exception ex) {
			if (socket != null) {
				try {
					socket.close();
				} catch (Exception nestedEx) {
				}
			}
			return null;
		}
		return socket;
	}

	/**
	 * Destroy the server socket.
	 * 
	 * @param socket
	 *            The serverSocket to destroy.
	 */
	public static void destroyServerSocket(ServerSocket socket) {
		if (socket == null) {
			return;
		}
		try {
			socket.close();
		} catch (Exception ex) {
		}
	}

	/**
	 * Destroy a socket.
	 * 
	 * @param socket
	 *            The socket to destroy.
	 */
	public static void destroySocket(Socket socket) {
		if (socket == null) {
			return;
		}
		try {
			socket.close();
		} catch (Exception ex) {
		}
	}
}
