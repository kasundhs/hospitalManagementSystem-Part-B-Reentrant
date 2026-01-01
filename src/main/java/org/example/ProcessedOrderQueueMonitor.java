package org.example;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ProcessedOrderQueueMonitor {

    private final Queue<TestOrder> processedOrderQueue = new LinkedList<>();
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);
    private final ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();
    private final ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock(); // For mixing processes don't use readLock

    // Condition to wait on
    private final Condition notEmptyOrNotProcessing = writeLock.newCondition();

    private boolean isProcessing = false;
    public void addProcessedOrder(TestOrder order) {
        writeLock.lock();
        try {
            processedOrderQueue.add(order);
            notEmptyOrNotProcessing.signalAll();
        } finally {
            writeLock.unlock();
        }
    }
    public TestOrder consumeForReport() throws InterruptedException {
        writeLock.lock();
        try {
            // Wait until queue has items and auditor is not processing
            while (processedOrderQueue.isEmpty() || isProcessing) {
                notEmptyOrNotProcessing.await();
            }
            isProcessing = true; // lock processing
            return processedOrderQueue.poll();
        } finally {
            writeLock.unlock();
        }
    }
    public void removeProcessedOrder(){ // remove in consumer interrupted
        writeLock.lock();
        try {
            processedOrderQueue.poll();
        }
        finally {
            writeLock.unlock();
        }
    }
    public void releaseProcessingLock() {
        writeLock.lock();
        try {
            isProcessing = false;
            notEmptyOrNotProcessing.signalAll(); // wake up next auditor
        } finally {
            writeLock.unlock();
        }
    }
    public void setExpiration() {
        writeLock.lock();
        try {
            isProcessing = false;
            LogWriter.log("============= Processed Order Queue - Clearing remaining orders =============");
            if (processedOrderQueue.isEmpty()) {
                LogWriter.log("============= All Reports Generated. Nothing to Expire =============");
            }
            while (!processedOrderQueue.isEmpty()) {
                TestOrder order = processedOrderQueue.poll();
                LogWriter.log(order + " report expired due to system timeout");
            }
            notEmptyOrNotProcessing.signalAll();
        } finally {
            writeLock.unlock();
        }
    }
}
