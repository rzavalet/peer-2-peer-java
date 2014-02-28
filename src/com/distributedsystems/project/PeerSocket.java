package com.distributedsystems.project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

public class PeerSocket implements SocketInterface{

	private Socket socket;
	private boolean debug = false;
	
	public PeerSocket(Socket socket) {
		this.socket = socket;
		try {
			socket.setTcpNoDelay(true);
			socket.setPerformancePreferences(1, 0, 0);
			socket.setReceiveBufferSize(256);
			socket.setSendBufferSize(256);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public PeerSocket(String host, int port) {
		InetAddress address;
		try {
			address = InetAddress.getByName(host);

			socket = new Socket(address, port);
			socket.setTcpNoDelay(true);
			socket.setPerformancePreferences(1, 0, 0);
			socket.setReceiveBufferSize(256);
			socket.setSendBufferSize(256);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	@Override
	public int read() {
		InputStream in = null;
		int size = -1;
		
		try {
			in = socket.getInputStream();
			size = in.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        return size;
	}*/

	@Override
	public int read(byte[] data) {
		InputStream in = null;
		int size = -1;
		
		try {
			in = socket.getInputStream();
			size = in.read(data);
			Debug.print("...Received bytes: " + new String(data), debug);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        return size;
	}
	
	public String read() {
	
		try {
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String data = inFromServer.readLine();
			Debug.print("...Received bytes: " + data, debug);
			return data;
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        return null;
	}
	
	@Override
	public void write(byte[] data) {
		
		OutputStream out = null;
		
		try {
			out = socket.getOutputStream();
			//Debug.print("...Sending bytes: " + new String(data), debug);
			out.write(data);
			out.flush();
			//out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void write(String data) {

		try {
			PrintWriter outToServer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
			Debug.print("...Sending bytes: " + new String(data), debug);
			outToServer.print(data + "\n");
			outToServer.flush();
			//out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void close() {
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
