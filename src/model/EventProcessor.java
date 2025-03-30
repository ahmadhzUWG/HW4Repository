package model;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class EventProcessor extends Thread {
    private final Counter counter;
    private final LamportClock clock;
    private final String nodeName;
    private final CountDownLatch latch;
    private static final int NUM_EVENTS = 10;
    private List<String> otherNodes;

    public EventProcessor(Counter counter, LamportClock clock, String nodeName, CountDownLatch latch, List<String> otherNodes) {
        this.counter = counter;
        this.clock = clock;
        this.nodeName = nodeName;
        this.latch = latch;
        this.otherNodes = otherNodes;
    }

    @Override
    public void run() {
    	String threadName = "Thread_" + Thread.currentThread().getId();

        for (int i = 0; i < NUM_EVENTS; i++) {
            clock.tick();
            counter.increment();
            System.out.println(threadName + " executing local event");
            sendEvent();
        }

        latch.countDown();
    }

    private void sendEvent() {
    	Random random = new Random();
        int randomNode = random.nextInt(otherNodes.size());
        String receiver = otherNodes.get(randomNode);
        String[] parts = receiver.split(",");
        receiver = parts[0];
        String address = parts[1];
        
        try (Socket socket = new Socket(address, Node.port);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
        	            
        	
			Event event = new Event(nodeName, receiver, clock.getTime());
			out.println(event);
        } catch (IOException e) {
            System.err.println("Error sending event from Node " + nodeName);
        }
    }
}
