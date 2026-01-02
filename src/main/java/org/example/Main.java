package org.example;

public class Main {
    public static void main(String[] args) {

        IntakeQueueReentrant queue = new IntakeQueueReentrant(Constants.MAXIMUM_QUEUE_SIZE);
        SystemStateMonitor state = new SystemStateMonitor();
        ProcessedOrderQueueMonitor processedQueue = new ProcessedOrderQueueMonitor();
        EventScheduler event = new EventScheduler(queue,state,processedQueue);

        System.out.println("System Started... Waiting 15 seconds before stopping...");
        Producer p1 = new Producer(queue, state, "Clinic Counter -1");
        Consumer c1 = new Consumer(queue, state, processedQueue, "Doctor -1");
        Auditor a1 = new Auditor(state, processedQueue, "Auditor -1");
        Auditor a2 = new Auditor(state, processedQueue, "Auditor -2");
        Supervisor sup = new Supervisor(state, event, "Supervisor");

        p1.start();
        c1.start();
        a1.start();
        a2.start();
        sup.start();

        try {
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            System.out.println("Main thread interrupted.");
        }

        sup.shutdown();
        p1.shutdown();
        event.shutdownAllProducers();
        queue.setExpiration();
        c1.shutdown();
        event.shutdownAllConsumers();
        processedQueue.setExpiration();
        a1.shutdown();
        a2.shutdown();
        System.out.println("System shutdown completed....");
        
        // Calculate and display performance metrics
        System.out.println("\n============ Performance Metrics ============");
        System.out.println("Total Registered Count (without rejecting): " + state.getTotalRegisteredCount());
        System.out.println("Total Processed Count: " + state.getTotalProcessed());
        System.out.println("Total Report Generated Count: " + state.getTotalReportGenerateCount());
        
        // Calculate average time consumption
        int registeredCount = state.getTotalRegisteredCount();
        int processedCount = state.getTotalProcessed();
        int reportCount = state.getTotalReportGenerateCount();
        
        if (registeredCount > 0) {
            double avgProducerTime = (double) state.getProducerTotalTimeConsumption() / registeredCount;
            System.out.println("Average Producer Time Consumption: " + String.format("%.2f", avgProducerTime) + " ms");
        } else {
            System.out.println("Average Producer Time Consumption: N/A (no registrations)");
        }
        
        if (processedCount > 0) {
            double avgConsumerTime = (double) state.getConsumerTotalTimeConsumption() / processedCount;
            System.out.println("Average Consumer Time Consumption: " + String.format("%.2f", avgConsumerTime) + " ms");
        } else {
            System.out.println("Average Consumer Time Consumption: N/A (no processed orders)");
        }
        
        if (reportCount > 0) {
            double avgAuditorTime = (double) state.getAuditorTotalTimeConsumption() / reportCount;
            System.out.println("Average Auditor Time Consumption: " + String.format("%.2f", avgAuditorTime) + " ms");
        } else {
            System.out.println("Average Auditor Time Consumption: N/A (no reports generated)");
        }
        
        System.out.println("=============================================\n");
    }
}
