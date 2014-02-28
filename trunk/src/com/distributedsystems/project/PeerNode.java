package com.distributedsystems.project;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class PeerNode {

	public static final String GET_PEER_NAME = "NAME";
	public static final String REPLY = "REPL";
	public static final String ADD_PEER = "ADDP";
	public static final String GET_PEER_LIST = "GETL";
	public static final String END_GAME = "ENDG";
	public static final String START_GAME = "STAG";
	public static final String MOVE_RIGHT = "MOVR";
	
	private PeerInformation myPeerInformation;
	private HashMap<String, PeerInformation> fingerTable;
	private HashMap<String, HandlerInterface> handlers;
	
	private boolean shutdown = false;
	private static final boolean debug = true;
	
	public PeerNode(String id, int port, PeerInformation trackerPeerInformation) {
		
		Debug.print("Creating Peer Node", debug);
		
		//TODO: Make it more robust
		InetAddress address;
		try {
			address = InetAddress.getLocalHost();
			String hostIP = address.getHostAddress() ;
		    //String hostName = address.getHostName();
		    this.myPeerInformation = new PeerInformation(id, hostIP, port);
		    
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		//Initialize handlers
		this.handlers = new HashMap<String, HandlerInterface>();
		
		//Get peers		
		Debug.print("...Creating Finger Table", debug);
		this.fingerTable = new HashMap<String, PeerInformation>();
		buildFingerTable(trackerPeerInformation, 5);
		
	}
	
	private void buildFingerTable(PeerInformation trackerPeerInformation, int ttl){
		List<PeerMessage> messages = null;
		PeerMessage message = null;
		
		if (trackerPeerInformation == null){
			return;
		}
		
		//First obtain the peer's name
		//Debug.print("...Obtaining tracker information", debug);
		messages = null;
		messages = connectAndSend(trackerPeerInformation, GET_PEER_NAME, "", true);
		if (messages == null) {
			return;
		}
		
		if (messages.size() <= 0) {
			return;
		}
		
		message = messages.get(0);
		if (!message.getMessageType().equals(REPLY)) {
			return;
		}
		
		Debug.print("...Adding peer in remote peer", debug);
		
		String remotePeerId = message.getMessageData();
		if (fingerTable.containsKey(remotePeerId) == true) {
			Debug.print("...Peer already in table " + remotePeerId, debug);
			return;
		}
		else {
			Debug.print("...Peer not in table " + remotePeerId, debug);
		}
		
		//Now ask the peer to add me
		messages = null;
		messages = connectAndSend(trackerPeerInformation, ADD_PEER, 
				getPeerId() + " " + getHost() + " " + getPort() + " ", true);
		if (messages == null) {
			return;
		}
		
		if (messages.size() <= 0) {
			return;
		}
		
		message = messages.get(0);
		if (!message.getMessageType().equals(REPLY)) {
			return;
		}
		
		Debug.print("...Adding remote peer in local peer", debug);
		Debug.print("...... PeerID: " + remotePeerId, debug);
		//Add the remote peer to my finger table
		trackerPeerInformation.setPeerId(remotePeerId);
		fingerTable.put(trackerPeerInformation.getPeerId(), trackerPeerInformation);
		
		//Ask the peer for other peers
		Debug.print("...Getting remote peer list", debug);
		messages = null;
		messages = connectAndSend(trackerPeerInformation, GET_PEER_LIST, "", true);
		if (messages == null) {
			return;
		}
		
		if (messages.size() <= 1) {
			return;
		}
		
		message = messages.remove(0);
		
		for (PeerMessage currentMessage : messages) {
			String[] fields = currentMessage.getMessageData().split(" ");
			PeerInformation currentRemoteHost = new PeerInformation(fields[0], 
					fields[1], Integer.parseInt(fields[2]));
			if (getPeerId().equals(currentRemoteHost.getPeerId()) == false) {
				buildFingerTable(currentRemoteHost, ttl-1);
			}
		}
		
		Debug.print("*** FINISHED BUILDING TABLE ***", debug);
	}
	
	public void setShutdown(boolean shutdown) {
		this.shutdown = shutdown;
	}
	
	public String getPeerId() {
		return this.myPeerInformation.getPeerId();
	}
	
	public String getHost() {
		return this.myPeerInformation.getHost();
	}
	
	public int getPort() {
		return this.myPeerInformation.getPort();
	}
	
	public HandlerInterface getHandler(String command) {
		return this.handlers.get(command);
	}
	
	public void addHandler(String msgType, HandlerInterface handler) {
		this.handlers.put(msgType, handler);
	}
	

	public void broadcastMessage(String messageType,  String messageData) {
		for(String remotePeerId : getPeerKeys()) {
			connectAndSend(getPeer(remotePeerId), messageType, messageData, false);
		}
	}
	
	public List<PeerMessage> connectAndSend(PeerInformation remoteHost, 
			String messageType,  String messageData, boolean waitReply) {
		
		PeerMessage reply = null;
		List<PeerMessage> messages = new ArrayList<PeerMessage>();
		
		Debug.print("...Sending message to: " + remoteHost.getHost() + ":" + remoteHost.getPort(), debug);
		
		PeerConnection connection = new PeerConnection(remoteHost);
		PeerMessage message = new PeerMessage(messageType, messageData);
		connection.sendData(message);
		
		if (waitReply == true) {
			Debug.print("...Receiving message from: " + remoteHost.getHost() + ":" + remoteHost.getPort(), debug);
			reply = connection.receiveData();
			while (reply != null) {
				messages.add(reply);
				reply = connection.receiveData();
			}

		}
		
		connection.close();
		return messages;
	}
	
	public PeerMessage sendToPeer(String remotePeerId, 
			String messageType,  String messageData, boolean waitReply) {
		
		List<PeerMessage> receivedMessage = null;
		PeerInformation remoteHost = null;
		
		remoteHost = fingerTable.get(remotePeerId);
		if (remoteHost == null) {
			return null;
		}
		
		receivedMessage = connectAndSend(remoteHost, messageType, messageData, waitReply);
		if (receivedMessage == null) {
			return null;
		}
		
		if (receivedMessage.isEmpty()) {
			return null;
		}
		
		return receivedMessage.get(0);
	}
	
	public PeerInformation getPeer(String peerId) {
		return fingerTable.get(peerId);
	}
	
	public Set<String> getPeerKeys() {
		return fingerTable.keySet();
	}
	
	public int getNumberOfPeers() {
		return fingerTable.size();
	}
	
	public void insertPeer(PeerInformation peer) {
		fingerTable.put(peer.getPeerId(), peer);
	}
	
	public void connectionHandler() {
		Debug.print("...Starting connection Handler", debug);
		//Set the socket
			ServerSocket serverSocket = null;
			try {
				serverSocket = new ServerSocket(getPort());
				serverSocket.setSoTimeout(2);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if (serverSocket == null) {
				System.out.println("ERROR: could not create server socket...");
				return;
			}
			
			while (shutdown == false) {
				final Socket clientSocket;
				try {
					clientSocket = serverSocket.accept();
					clientSocket.setSoTimeout(0);
					//Start a new thread to handle this request
					new Thread() {

						@Override
						public void run() {
							PeerConnection peerConnection = new PeerConnection(myPeerInformation, clientSocket);
							PeerMessage message = peerConnection.receiveData();
							Debug.print("Processing: " + message.getMessageType(), debug);
							HandlerInterface handler = handlers.get(message.getMessageType());
							if (handler != null){
								handler.handleMessage(peerConnection, message);
							}
							peerConnection.close();
						}
						
					}.start();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
					//Debug.print("...Waiting connection", debug);
				}
				
			}

	}
	
	
}
