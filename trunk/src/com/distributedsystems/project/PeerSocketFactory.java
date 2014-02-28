package com.distributedsystems.project;

import java.net.Socket;


public class PeerSocketFactory {

	private static PeerSocketFactory instance = null;
	
	protected PeerSocketFactory() {	}
	
	public static PeerSocketFactory getSocketFactory() {
		if (instance == null) {
			instance = new PeerSocketFactory();
		}
		
		return instance;
	}
	
	public SocketInterface makeSocket(String host, int port) {
		SocketInterface mySocket = new PeerSocket(host, port);
		return mySocket;
	}

	public SocketInterface makeSocket(Socket socket) {
		SocketInterface mySocket = new PeerSocket(socket);
		return mySocket;
	}
}
