package com.distributedsystems.project;

public interface HandlerInterface {
	void handleMessage(PeerConnection connection, PeerMessage message);
}
