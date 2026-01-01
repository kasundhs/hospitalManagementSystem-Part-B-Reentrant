package org.example;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class IntakeQueueReentrant {
    private final Queue<TestOrder> emergencyQueue = new LinkedList<>();
    private final Queue<TestOrder> normalQueue = new LinkedList<>();
    private final int capacity;
    private boolean isForNormalPatients = true;
    private volatile boolean isShuttingDown = false;
    
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    public IntakeQueueReentrant(int capacity) {
        this.capacity = capacity;
    }
    
    public int getQueueUsage(){
        lock.lock();
        try {
            return emergencyQueue.size() + normalQueue.size();
        } finally {
            lock.unlock();
        }
    }

    public void produce(TestOrder order) throws InterruptedException {
        lock.lock();
        try {
            if (isShuttingDown)
                throw new InterruptedException("Cannot produce System is proceeding for shut down");
            
            while (getQueueUsage() == capacity && !isShuttingDown) {
                notFull.await();
            }
            
            if (isShuttingDown)
                throw new InterruptedException("Cannot Continue producing, due to System is to shut down");
            
            if (order.priority == Priority.EMERGENCY) {
                emergencyQueue.add(order);
            } else {
                normalQueue.add(order);
            }
            notEmpty.signalAll();

        } finally {
            lock.unlock();
        }
    }

    public TestOrder consume(boolean emergencyFirst) throws InterruptedException {
        lock.lock();
        try {
            while (getQueueUsage() == 0 && !isShuttingDown) {
                notEmpty.await();
            }
            
            if (getQueueUsage() == 0 && isShuttingDown) {
                return null;
            }
            
            TestOrder order = null;
            if (emergencyFirst && !emergencyQueue.isEmpty()) {
                order = emergencyQueue.poll();
            }
            /* While emergencyFirst is disabled emergency patients have to wait
             * until normal queue is empty. To prevent that use isForNormalPatients variable.*/
            else if (isForNormalPatients && !normalQueue.isEmpty()) {
                order = normalQueue.poll();
                isForNormalPatients = false;
            }
            else if (!emergencyQueue.isEmpty()) {
                order = emergencyQueue.poll();
                isForNormalPatients = true;
            }
            
            notFull.signalAll();
            return order;

        } finally {
            lock.unlock();
        }
    }

    public void setExpiration() {
        lock.lock();
        try {
            isShuttingDown = true;
            LogWriter.log("============= System set to ShutDown =============");
            notFull.signalAll();
            notEmpty.signalAll();
            
            while (!normalQueue.isEmpty()) {
                TestOrder order = normalQueue.poll();
                LogWriter.log(order.toString() + " is expired due to system timeout");
            }
            while (!emergencyQueue.isEmpty()) {
                TestOrder order = emergencyQueue.poll();
                LogWriter.log(order.toString() + " is expired due to system timeout");
            }

        } finally {
            lock.unlock();
        }
    }
}

