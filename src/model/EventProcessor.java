package model;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class EventProcessor extends Thread {
    private final Counter counter;
    private final LamportClock clock;
    private final int nodeId;
    private final Random rand = new Random();
    private final CountDownLatch latch;
    private static final int MAX_NUM_EVENTS = 1000;

    public EventProcessor(Counter counter, LamportClock clock, int nodeId, CountDownLatch latch) {
        this.counter = counter;
        this.clock = clock;
        this.nodeId = nodeId;
        this.latch = latch;
    }

    @Override
    public void run() {
    	String threadName = "Thread_" + Thread.currentThread().getId();
        int eventCount = rand.nextInt(MAX_NUM_EVENTS) + 1;

        for (int i = 0; i < eventCount; i++) {
            clock.tick();
            counter.increment();
            System.out.println(threadName + " executing local event");
            sendEvent();
        }

        latch.countDown();
    }

    private void sendEvent() {
        try (Socket socket = new Socket("localhost", 4225);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
        	out.println(clock.getTime() + "," + nodeId);
        } catch (IOException e) {
            System.err.println("Error sending event from Node " + nodeId);
        }
    }
}
