package model;

import java.util.concurrent.atomic.AtomicLong;

public class LamportClock {
    private final AtomicLong time;
    private final int nodeIncrement;

    public LamportClock(int nodeIncrement) {
        this.time = new AtomicLong(0);
        this.nodeIncrement = nodeIncrement;
    }

    public void tick() {
        time.getAndAdd(nodeIncrement);
    }

    public void update(long receivedTime) {
        long currentTime;
        do {
            currentTime = time.get();
        } while (!time.compareAndSet(currentTime, Math.max(currentTime, receivedTime) + nodeIncrement));
    }

    public long getTime() {
        return time.get();
    }
}

