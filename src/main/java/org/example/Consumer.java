package org.example;

import java.util.Random;

public class Consumer implements Runnable {

    private final IntakeQueueReentrant queue;
    private final SystemStateMonitor state;
    private final ProcessedOrderQueueMonitor processedOrderQueue;
    private final String name;
    private Thread thread;
    private final Random rnd = new Random();
    private volatile boolean running = true;

    public Consumer(IntakeQueueReentrant queue, SystemStateMonitor state,
                    ProcessedOrderQueueMonitor processedOrderQueue, String name) {

        this.queue = queue;
        this.state = state;
        this.processedOrderQueue = processedOrderQueue;
        this.name = name;
    }

    @Override
    public void run() {
        try {
            while (running) {
                TestOrder order = queue.consume(state.isEmergencyPriorityEnabled());
                if (order == null) continue;
                try {
                    if (order.priority == Priority.EMERGENCY)
                        state.decrementEmergencyPatientCount();
                    LogWriter.log(name + " processing " + order);
                    state.incrementProcessed();
                    processedOrderQueue.addProcessedOrder(order);
                }
                catch (Exception e){
                    LogWriter.log(order.toString()+" is started to Process. But Cannot Complete due to time Exceed.");
                    if(order.priority == Priority.EMERGENCY)
                        state.setEmergencyPatientCount();
                    processedOrderQueue.removeProcessedOrder();
                    state.decrementProceed();
                }
                Thread.sleep(200 + rnd.nextInt(250));
            }

        } catch (InterruptedException e) {
            LogWriter.log(name + " interrupted unexpectedly");
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

    public void gracefulShutdown() {
        running =false;
        if ((thread != null)) {
            thread.interrupt();
            try {
                thread.join();
            }catch (InterruptedException e) {
                shutdown();
                LogWriter.threadWriterLog("Exception is occurred while joining the thread : " + name);
            }
        }
    }

    public String toString(){
        return name;
    }
}
