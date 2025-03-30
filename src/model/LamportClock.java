package model;

import java.util.concurrent.atomic.AtomicInteger;

public class LamportClock {
    private final AtomicInteger time;
    private final int nodeId;

    public LamportClock(int nodeId) {
        this.time = new AtomicInteger(0);
        this.nodeId = nodeId;
    }

    public void tick() {
        time.getAndAdd(nodeId);
    }

    public void update(int receivedTime) {
        int currentTime;
        do {
            currentTime = time.get();
        } while (!time.compareAndSet(currentTime, Math.max(currentTime, receivedTime) + nodeId));
    }

    public int getTime() {
        return time.get();
    }
}

