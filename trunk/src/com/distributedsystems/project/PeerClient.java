package com.distributedsystems.project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class PeerClient {

	private PeerNode peerNode;
	private boolean shutdown = false;
	private BufferedReader br;
	
	private static final boolean debug = true;
	
	private class PeerName implements HandlerInterface {
		private String myId;
		
		public PeerName(String peer) {
			myId = peer;
		}
		
		@Override
		public void handleMessage(PeerConnection connection, PeerMessage message) {
			PeerMessage messageName = null;
			Debug.print("... Replying with peer name: " + myId, debug);
			
			messageName = new PeerMessage(PeerNode.REPLY, myId);
			connection.sendData(messageName);
		}
		
	}
	
	private class PeerList implements HandlerInterface {
		private PeerNode peer;
		
		public PeerList(PeerNode peer) {
			this.peer = peer;
		}
		
		@Override
		public void handleMessage(PeerConnection connection, PeerMessage message) {
			PeerMessage messageList = null;
			Debug.print("Listing peers", debug);
			
			messageList = new PeerMessage(PeerNode.REPLY, String.valueOf(peer.getNumberOfPeers()));
			connection.sendData(messageList);
			
			for (String currentPeerId : peer.getPeerKeys()) {
				PeerInformation currentPeerInformation= peer.getPeer(currentPeerId);
				messageList = new PeerMessage(PeerNode.REPLY, 
						currentPeerInformation.getPeerId() + " " + currentPeerInformation.getHost() + 
						" " + currentPeerInformation.getPort());
				connection.sendData(messageList);
			}

		}
	}
	
	private class AddPeer implements HandlerInterface {
		private PeerNode peer;
		
		public AddPeer(PeerNode peer) {
			this.peer = peer;
		}
		
		@Override
		public void handleMessage(PeerConnection connection, PeerMessage message) {
			String[] dataList = message.getMessageData().split(" ");
			String peerId = dataList[0];
			String host = dataList[1];
			String port = dataList[2];
			
			PeerInformation peerInformation = new PeerInformation(peerId, host, Integer.valueOf(port));
			
			if (peer.getPeerKeys().contains(peerId) == false) {
				peer.insertPeer(peerInformation);
			}
						
			PeerMessage replyMessage = new PeerMessage(PeerNode.REPLY, "Peer added: " + peerId);
			connection.sendData(replyMessage);

		}
		
	}
	
	private class StartGame implements HandlerInterface {
		@Override
		public void handleMessage(PeerConnection connection, PeerMessage message) {
			System.out.println("*** STARTING GAME");
		}
	}
	
	private class EndGame implements HandlerInterface {
		@Override
		public void handleMessage(PeerConnection connection, PeerMessage message) {
			System.out.println("*** END GAME");
		}
	}
	
	private class MoveRight implements HandlerInterface {
		@Override
		public void handleMessage(PeerConnection connection, PeerMessage message) {
			System.out.println("*** MOVING RIGHT");
		}
	}
	
	public PeerClient(String id, int port, PeerInformation trackerPeerInformation) {
		peerNode = new PeerNode(id, port, trackerPeerInformation);
		
		//Add handlers
		HandlerInterface peerName = new PeerName(peerNode.getPeerId());
		peerNode.addHandler(PeerNode.GET_PEER_NAME, peerName);
		
		HandlerInterface peerList = new PeerList(peerNode);
		peerNode.addHandler(PeerNode.GET_PEER_LIST, peerList);
		
		HandlerInterface addPeer = new AddPeer(peerNode);
		peerNode.addHandler(PeerNode.ADD_PEER, addPeer);
		
		HandlerInterface startGame = new StartGame();
		peerNode.addHandler(PeerNode.START_GAME, startGame);
		
		HandlerInterface endGame = new EndGame();
		peerNode.addHandler(PeerNode.END_GAME, endGame);
		
		HandlerInterface moveRight = new MoveRight();
		peerNode.addHandler(PeerNode.MOVE_RIGHT, moveRight);
		
		br = new BufferedReader(new InputStreamReader(System.in));
	}
	
	public void runPeer() {
		Debug.print("... Starting threads", debug);
		console();
	}
	
	public boolean validateCommand(String[] command) {
		int numArguments;
		String commandName = null;
		
		numArguments = command.length;
		if (numArguments < 1) {
			return false;
		}
		
		commandName = command[0];
		HandlerInterface handler = peerNode.getHandler(commandName);
		if (handler == null) {
			return false;
		}
		
		return true;
	}
	
	public void printMyPeers() {
		Debug.print("... Printing peers", debug);
		for (String currentPeerId : peerNode.getPeerKeys()) {
			PeerInformation currentPeerInformation= peerNode.getPeer(currentPeerId);
			System.out.println("<" + currentPeerInformation.getPeerId() + 
					", " + currentPeerInformation.getHost() + ", " + currentPeerInformation.getPort());
		}
	}
	
	public void printHelp() {
		Debug.print("... Printing help", debug);
		System.out.println("MY_LIST: Print the list of available peers");
		System.out.println("MY_NAME: Print info of this peer");
		System.out.println("BYE: Quit");
	}
	
	public void console() {
		String[] commands = null;
		
		Debug.print("... Starting console", debug);
		
		while (shutdown == false) {
			commands = readCommandLine();
			
			if (commands[0].equals("BYE")) {
				Debug.print("... Shutting down threads", debug);
				shutdown = true;
				this.peerNode.setShutdown(shutdown);
				continue;
			}
			
			if (commands[0].equals("MY_LIST")) {
				printMyPeers();
				continue;
			}
			
			if (commands[0].equals("MY_NAME")) {
				System.out.println("I am: " + peerNode.getPeerId());
				continue;
			}
			
			if (commands[0].equals("HELP")) {
				printHelp();
				continue;
			}
			
			if (commands[0].equals("START")) {
				startGame();
				continue;
			}
			
			if (commands[0].equals("END")) {
				endGame();
				continue;
			}
			
			if (commands[0].equals("MOVE_RIGHT")) {
				moveRight();
				continue;
			}
			
			if (validateCommand(commands) == false) {
				System.out.println("Invalid command");
				continue;
			}
		}
	}
	
	private void moveRight() {
		peerNode.broadcastMessage(PeerNode.MOVE_RIGHT, "");
	}

	private void endGame() {
		peerNode.broadcastMessage(PeerNode.END_GAME, "");
	}

	private void startGame() {
		peerNode.broadcastMessage(PeerNode.START_GAME, "");
	}

	
	public String[] readCommandLine() {
		String[] commands = null;
		
		restoreDisplay();
        try {
			String s = br.readLine();
			commands = s.split(" ");
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        return commands;
	}
	
	public void restoreDisplay() {
		System.out.print("Type a command: ");
	}
	
	public static void main(String[] args) {
		int port = -1;
		String myId = null;
		
		String trackerIp = null;
		int trackerPort = -1;
		PeerInformation tracker = null;
		
		
		Debug.print("*** STARTING PEER ****", debug);
		
		if (args.length != 2 && args.length != 4) {
			Debug.print("ERROR: invalid number of parameters: Args Count: " + args.length, debug);
			return;
		}
	
		myId = args[0];
		port = Integer.parseInt(args[1]);
		Debug.print("I am: "+ myId, debug);
		
		
		if (args.length == 4) {
			trackerIp = args[2];
			trackerPort = Integer.parseInt(args[3]);
			
			Debug.print("Tracker: (" + trackerIp + ", " + trackerPort + ")", debug);
			tracker = new PeerInformation(null, trackerIp, trackerPort);			
		}
		else {
			Debug.print("...Creating first Node", debug);			
		}
		
		final PeerClient myClient = new PeerClient(myId, port, tracker);
		
		new Thread("ConnectionHandler") {

			@Override
			public void run() {
				myClient.peerNode.connectionHandler();
			}
			
		}.start();
		
		myClient.console();
	}
	
	
}
