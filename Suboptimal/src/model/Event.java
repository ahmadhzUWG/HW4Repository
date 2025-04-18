package model;

import java.io.Serializable;

/**
 * Represents an event in the DS. This object is serializable so it can be sent
 * over the network.
 * 
 * @author CS4225
 * @version Spring 2025
 */
public class Event implements Serializable {

	private static final long  serialVersionUID = 4225L;
	
	private transient long p1, p2, p3, p4, p5, p6, p7;
	
	private final String sender;
	private final String receiver;
	private final long timestamp;
	
	@SuppressWarnings("unused")
	private transient long p8, p9, p10, p11, p12, p13, p14;

	public Event(String sender, String receiver, long timestamp) {
		this.sender = sender;
		this.receiver = receiver;
		this.timestamp = timestamp;
	}

	public String getSender() {
		return this.sender;
	}

	public String getReceiver() {
		return this.receiver;
	}

	public long getTimestamp() {
		return this.timestamp;
	}

	@Override
	public String toString() {
		return "Event from " + sender + " to " + receiver + " with timestamp " + timestamp;
	}
	
	public long preventJVMOptimization(){
		return p1 + p2 + p3 + p4 + p5 + p6 + p7; 
	}
}