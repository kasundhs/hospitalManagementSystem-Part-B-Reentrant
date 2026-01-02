package org.example;

import java.util.Random;

public class Producer implements Runnable {

    private final IntakeQueueReentrant queue;
    private final SystemStateMonitor state;
    private final String name;
    private volatile boolean running = true;
    private Thread thread;
    private final Random rnd = new Random();

    public Producer(IntakeQueueReentrant queue, SystemStateMonitor state, String name) {
        this.queue = queue;
        this.state = state;
        this.name = name;
    }

    @Override
    public void run() {
        String[] types = {"PCR", "Blood Test", "Histopathology"};

        try {
            while (running) {
                long startTime = System.currentTimeMillis();
                Priority priority = rnd.nextInt(10) < 5 ? Priority.EMERGENCY : Priority.NORMAL;
                IsSpecialTest special = rnd.nextInt(10) < 2 ? IsSpecialTest.YES : IsSpecialTest.NO;
                TestOrder order = new TestOrder(types[rnd.nextInt(types.length)], priority, special);

                if (order.isSpecialTest == IsSpecialTest.YES) {
                    LogWriter.log(order + " rejected (special test unavailable)");
                    continue;
                }
                try {
                    if (priority == Priority.EMERGENCY)
                        state.setEmergencyPatientCount();
                    queue.produce(order);
                    long endTime = System.currentTimeMillis();
                    long timeConsumed = endTime - startTime;
                    state.addProducerTimeConsumption(timeConsumed);
                    state.incrementTotalRegisteredCount();
                    LogWriter.log(name + " registered " + order);
                }
                catch (InterruptedException e){
                    LogWriter.log( order.toString()+" Created. But It cannot be Completed, due to time Exceed.");
                    if(priority == Priority.EMERGENCY){
                        state.decrementEmergencyPatientCount();
                    }
                }
                Thread.sleep(100 + rnd.nextInt(300));
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
