package org.example;

public class Auditor implements Runnable {

    private final SystemStateMonitor state;
    private final ProcessedOrderQueueMonitor processedOrderQueue;
    private final String name;
    private Thread thread;
    private volatile boolean running = true;

    public Auditor(SystemStateMonitor state, ProcessedOrderQueueMonitor processedOrderQueue, String name) {
        this.state = state;
        this.processedOrderQueue = processedOrderQueue;
        this.name = name;
    }

    @Override
    public void run() {
        try {
            while (running) {
                long startTime = System.currentTimeMillis();
                TestOrder order = processedOrderQueue.consumeForReport();

                if (order != null) {
                    LogWriter.log(name + " generating report for " + order);

                    ReportGenrator reportGenrator = new ReportGenrator(order);
                    reportGenrator.reportDetails(order);

                    state.incrementReportCount();
                    
                    long endTime = System.currentTimeMillis();
                    long timeConsumed = endTime - startTime;
                    state.addAuditorTimeConsumption(timeConsumed);

                    LogWriter.log("Total Processed So far: " + state.getTotalProcessed() +
                            " and, Total Report Generates So far: " + state.getTotalReportGenerateCount());
                }
                processedOrderQueue.releaseProcessingLock();
                Thread.sleep(200);
            }

        } catch (InterruptedException e) {
            // Interruption is expected during shutdown, restore interrupt status
            Thread.currentThread().interrupt();
            LogWriter.log(name + " interrupted unexpectedly");
        } finally {
            processedOrderQueue.releaseProcessingLock();
        }
    }

    public void start() {
        thread = new Thread(this, name);
        thread.start();
    }

    public void shutdown() {
        running = false;
        if (thread != null) thread.interrupt();
    }
}
