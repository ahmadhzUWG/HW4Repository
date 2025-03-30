package model;

import java.util.concurrent.atomic.AtomicInteger;

public class LamportClock {
    private final AtomicInteger time;
    private final int nodeIncrement;

    public LamportClock(int nodeIncrement) {
        this.time = new AtomicInteger(0);
        this.nodeIncrement = nodeIncrement;
    }

    public void tick() {
        time.getAndAdd(nodeIncrement);
    }

    public void update(int receivedTime) {
        int currentTime;
        do {
            currentTime = time.get();
        } while (!time.compareAndSet(currentTime, Math.max(currentTime, receivedTime) + nodeIncrement));
    }

    public int getTime() {
        return time.get();
    }
}

