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
    
    private final ExecutorService eventExecutor;
    private final ExecutorService listenerExecutor;

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
        
        this.eventExecutor = Executors.newFixedThreadPool(NUM_THREADS);
        this.listenerExecutor = Executors.newSingleThreadExecutor();
   }

    public void start() {
        CountDownLatch latch = new CountDownLatch(NUM_THREADS);

        listenerExecutor.execute(this::listenForEvents);
        
        ShutdownListener shutdownListener = new ShutdownListener();
        shutdownListener.setDaemon(true);
        shutdownListener.start();

        this.startTime = System.currentTimeMillis();
        
        for (int i = 0; i < NUM_THREADS; i++) {
            eventExecutor.execute(new EventProcessor(localCounters[i], clock, nodeName, latch, otherNodes));
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
        	long executionTime = System.currentTimeMillis() - startTime;
            System.out.println("Final Lamport time: " + clock.getTime());
            System.out.println("Total Execution time = " + executionTime + " ms");
            
            try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
            
            eventExecutor.shutdown();
            try {
                if (!eventExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    eventExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                eventExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }

            listenerExecutor.shutdown();
        }
    }

    private void createEventLog()
    {
    	File eventLog = new File("events.log");
    	try (FileWriter writer = new FileWriter(eventLog, false)) {
            writer.write("");
		} catch (IOException e) {
			e.printStackTrace();
		}
        
    }
    
    private void logEvent(Event event) {
        try {
        	FileWriter writer = new FileWriter("events.log", true);
        	String receiver = event.getReceiver();
        	writer.write(receiver + " has sent an event\n");
        	writer.close();
        } catch (IOException e) {
        	e.printStackTrace();
        }
    }
    
    private void listenForEvents() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
        	this.createEventLog();
            System.out.println(nodeName + " listening...");
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    if (!eventExecutor.isShutdown() && !eventExecutor.isTerminated()) {
                        eventExecutor.execute(() -> processEvent(socket));
                    }
                } catch (Exception e) {
                    System.err.println("ERROR IN LISTENING FOR EVENTS: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.err.println("Error in " + nodeName + ": " + e.getMessage());
        }
    }

    private void processEvent(Socket socket) {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            Event event = (Event) in.readObject();
            long receivedTime = event.getTimestamp();
            String sender = event.getSender();
            clock.update(receivedTime);
            remoteCounter.increment();
            System.out.println("Thread-" + Thread.currentThread().getId() + " executing received event (t=" + receivedTime + ") from Node" + sender);
            this.logEvent(event);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error processing event: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {}
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
		try (BufferedReader br = new BufferedReader(new FileReader("nodes.csv"))) {

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
