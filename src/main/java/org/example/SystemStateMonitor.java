package org.example;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SystemStateMonitor {

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);
    private final ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();

    private int totalProcessedReportCount = 0;
    private int emergencyPatientCount = 0;
    private boolean emergencyPriorityEnabled = true;
    private int totalReportGenerateCount = 0;
    private int numberOfProducerThreads = 1;
    private int numberOfConsumerThreads = 1;

    // Write Locks
    public void incrementProcessed() {
        writeLock.lock();
        try {
            totalProcessedReportCount++;
        } finally {
            writeLock.unlock();
        }
    }

    public void decrementProceed() {
        writeLock.lock();
        try {
            totalProcessedReportCount--;
        } finally {
            writeLock.unlock();
        }
    }
    public void incrementReportCount() {
        writeLock.lock();
        try {
            totalReportGenerateCount++;
        } finally {
            writeLock.unlock();
        }
    }
    public void setEmergencyPriorityEnabled(boolean enabled) {
        writeLock.lock();
        try {
            emergencyPriorityEnabled = enabled;
        } finally {
            writeLock.unlock();
        }
    }

    public void setEmergencyPatientCount() {
        writeLock.lock();
        try {
            emergencyPatientCount++;
        } finally {
            writeLock.unlock();
        }
    }

    public void decrementEmergencyPatientCount() {
        writeLock.lock();
        try {
            emergencyPatientCount--;
        } finally {
            writeLock.unlock();
        }
    }

    // Read Locks
    public int getTotalProcessed() {
        readLock.lock();
        try {
            return totalProcessedReportCount;
        } finally {
            readLock.unlock();
        }
    }

    public int getEmergencyPatientCount() {
        readLock.lock();
        try {
            return emergencyPatientCount;
        } finally {
            readLock.unlock();
        }
    }

    public boolean isEmergencyPriorityEnabled() {
        readLock.lock();
        try {
            return emergencyPriorityEnabled;
        } finally {
            readLock.unlock();
        }
    }

    public int getTotalReportGenerateCount() {
        readLock.lock();
        try {
            return totalReportGenerateCount;
        } finally {
            readLock.unlock();
        }
    }

    public int getNumberOfProducerThreads() {
        return numberOfProducerThreads;
    }
    public int getNumberOfConsumerThreads() {
        return numberOfConsumerThreads;
    }
    public void setNumberOfProducerThreads() {
        numberOfProducerThreads++;
    }
    public void setNumberOfConsumerThreads() {
        numberOfConsumerThreads++;
    }
    public void reduceProducersCount() {
        numberOfProducerThreads--;
    }
    public void reduceConsumersCount() {
        numberOfConsumerThreads--;
    }
}
