package com.distributedsystems.project;

public interface SocketInterface {

	//public int read();
	public String read();
	public int read(byte[] data);
	public void write(String data);
	public void write(byte[] data);
	public void close();
	
}