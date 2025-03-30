package model;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Node {
    private final String nodeName;
    public static final int port = 4225;
    private final LamportClock clock;
    private final Counter[] localCounters;
    private final Counter remoteCounter;
    private static final int NUM_THREADS = 8;
    private long startTime;
    private List<String> otherNodes = new ArrayList<String>();

    public Node(String nodeName, String ipAddress) {
    	loadOtherNodes(nodeName, ipAddress);
    	int nodeIncrement = Integer.valueOf(ipAddress.substring(ipAddress.length() - 1, ipAddress.length()));
        this.nodeName = nodeName;
        this.clock = new LamportClock(nodeIncrement);
        this.remoteCounter = new Counter();
        this.localCounters = new Counter[NUM_THREADS];
        for (int i = 0; i < NUM_THREADS; i++) {
            localCounters[i] = new Counter();
        }
   }

    public void start() {
        CountDownLatch latch = new CountDownLatch(NUM_THREADS);

        Thread listenerThread = new Thread(this::listenForEvents);
        listenerThread.setDaemon(true);
        listenerThread.setPriority(Thread.MAX_PRIORITY);
        listenerThread.start();
        
        ShutdownListener shutdownListener = new ShutdownListener();
        shutdownListener.setDaemon(true);
        shutdownListener.start();

        this.startTime = System.currentTimeMillis();
        
        Thread[] eventThreads = new Thread[NUM_THREADS];
        for (int i = 0; i < NUM_THREADS; i++) {
            eventThreads[i] = new EventProcessor(localCounters[i], clock, nodeName, latch, otherNodes);
            eventThreads[i].start();
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
        	long executionTime = System.currentTimeMillis() - startTime;
            System.out.println("Final Lamport time: " + clock.getTime());
            System.out.println("Total Execution time = " + executionTime + " ms");
        }
    }


    private void listenForEvents() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println(nodeName + " listening...");
            while (true) {
            	try {
                    Socket socket = serverSocket.accept();
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    
                    Event event = (Event) in.readObject();
                    long receivedTime = event.getTimestamp();
                    String senderId = event.getSender();
                    clock.update(receivedTime);
                    remoteCounter.increment();
                    System.out.println("Thread-" + Thread.currentThread().getId() + " executing received event (t=" + receivedTime + ") from Node" + senderId);
                    socket.close(); 
                    in.close();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.err.println("Error in " + nodeName);
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java Node <node_name> <node_ip_address>");
            return;
        }
        String nodeName = args[0];
        String ipAddress = args[1];
        
        Node node = new Node(nodeName, ipAddress);
        node.start();
    }

	private void loadOtherNodes(String nodeName, String ipAddress) {
		String nodeSelf = nodeName + "," + ipAddress;
		List<String> nodes = new ArrayList<String>();
		try (BufferedReader br = new BufferedReader(new FileReader("src/nodes.csv"))) {

            String line;
            while ((line = br.readLine()) != null) {
	            if (!line.equals(nodeSelf)) {
	            	nodes.add(line);
	            }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.otherNodes = nodes;
	}
}