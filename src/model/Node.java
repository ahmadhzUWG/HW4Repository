package model;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Node {
    private final int nodeId;
    private final int port;
    private final LamportClock clock;
    private final Counter[] localCounters;
    private final Counter remoteCounter;
    private final int[] peerPorts;
    private static final int NUM_THREADS = 2;
    private long startTime;

    public Node(int nodeId, int port, int[] peerPorts) {
        this.nodeId = nodeId;
        this.port = port;
        this.peerPorts = peerPorts;
        this.clock = new LamportClock(nodeId);
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
        
        try {
            Thread.sleep(2000 - (nodeId * 225));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        this.startTime = System.currentTimeMillis();
        
        Thread[] eventThreads = new Thread[NUM_THREADS];
        for (int i = 0; i < NUM_THREADS; i++) {
            eventThreads[i] = new EventProcessor(localCounters[i], clock, nodeId, peerPorts, latch);
            eventThreads[i].start();
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
        	long executionTime = System.currentTimeMillis() - startTime;
        	try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
            System.out.println("Final Lamport time: " + clock.getTime());
            System.out.println("Total Execution time = " + executionTime + " ms");
        }
    }


    private void listenForEvents() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Node " + nodeId + " listening on port " + port);
            while (true) {
                try (Socket socket = serverSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    String message = in.readLine();
                    if (message != null) {
                        String[] parts = message.split(",");
                        int receivedTime = Integer.parseInt(parts[0]);
                        int senderId = Integer.parseInt(parts[1]);
                        clock.update(receivedTime);
                        remoteCounter.increment();
                        System.out.println("Thread-" + Thread.currentThread().getId() + " executing received event (t=" + receivedTime + ") from Node" + senderId);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error in Node " + nodeId);
        }
    }
    public static int[] getPeerPorts(int nodeId) {
        List<Integer> peerPortsList = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            if (i + 1 != nodeId) {
                peerPortsList.add(4225 + i);
            }
        }

        return peerPortsList.stream().mapToInt(Integer::intValue).toArray();
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java Node <node_id>");
            return;
        }
        int nodeId = Integer.parseInt(args[0]);
        int port = 4225 + (nodeId - 1);
        int[] peerPorts = getPeerPorts(nodeId);
        
        Node node = new Node(nodeId, port, peerPorts);
        node.start();
    }
}