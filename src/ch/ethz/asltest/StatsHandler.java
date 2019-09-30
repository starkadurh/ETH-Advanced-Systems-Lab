package ch.ethz.asltest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;

/**
 * StatsHandler collects all the statistics and each worker thread has it's own instance
 * It is implemented such that it uses simple arithmetic such as addition and subtraction
 * as much as it can and keeps the number of system calls to an absolute minimum.
 * Every resolution seconds it enters the log function that then calculates the statistics
 * for the last resolution seconds and calls log4J to write them to file.
 */
class StatsHandler {
    private Logger logger = LogManager.getLogger("statsLogger");
    private Logger finalLogger = LogManager.getLogger("finalLogger");
    private int loggerIndex = 0;

    private final long resolution;
    private final int  histBins;
    private int[] responseTimeHistogram;
    private LinkedList<String> errorMessages = new LinkedList<>();


    private int method = -1; // set: method=0, get: method=1, multiGet: method = 2

    private int total = 0;
    private int gets = 0;
    private int sets = 0;
    private int multiGets = 0;

    private long serviceTime = 0;
    private long serviceTimeSet = 0;
    private long serviceTimeGet = 0;
    private long serviceTimeMultiGet = 0;
    private long responseTime = 0;

    private long waitingTime = 0;
    private int queueLength = 0;

    private long cacheHits = 0;
    private long cacheLookups = 0;


    private int lastTotal = 0;
    private int lastGets = 0;
    private int lastSets = 0;
    private int lastMultiGets = 0;
    private long lastTime;
    private long lastServiceTime = 0;
    private long lastServiceTimeSet = 0;
    private long lastServiceTimeGet = 0;
    private long lastServiceTimeMultiGet = 0;
    private long lastResponseTime = 0;
    private long lastWaitingTime = 0;
    private int lastQueueLength = 0;


    public StatsHandler(long resolution, int histBins) {
        this.lastTime = System.nanoTime();
        this.resolution = resolution * 1000000000L; // 1e9 nanoseconds in a second
        this.histBins = histBins;
        this.responseTimeHistogram = new int[histBins];
    }

    private void log(long currentTime) {
        int count = total-lastTotal;
        int getsCount = gets-lastGets;
        int setsCount = sets-lastSets;
        int multiGetsCount = multiGets-lastMultiGets;

        long ql = queueLength - lastQueueLength;
        long wt = waitingTime - lastWaitingTime;
        long st = serviceTime - lastServiceTime;
        long sts = serviceTimeSet - lastServiceTimeSet;
        long stg = serviceTimeGet - lastServiceTimeGet;
        long stmg = serviceTimeMultiGet - lastServiceTimeMultiGet;
        long rspt = responseTime - lastResponseTime;
        double time = (double)(currentTime-lastTime)/1000000000;

//        System.out.println("-------------------------------------------"+st+ "  "+sts+ "  "+stg+ "  "+stmg);

        logger.info(loggerIndex+","                          // Index for rounds of logging
                +(time>0?count/time:0)+","                      // Average throughput
                +(count>0?ql/count:0)+","                       // Average queue length
                +(count>0?wt/count:0)+","                       // Average waiting time in queue
                +(count>0?st/count:0)+","                       // Average service memcached
                +(setsCount>0?sts/setsCount:0)+","              // Average service sets
                +(getsCount>0?stg/getsCount:0)+","              // Average service gets
                +(multiGetsCount>0?stmg/multiGetsCount:0)+","   // Average service multigets
                +(count>0?rspt/count:0)+","                     // Average response time
                +setsCount+","                                  // Number of sets
                +getsCount+","                                  // Number of gets
                +multiGetsCount);                               // Number of multiGets

        setLastValues(currentTime);
    }

    private void setLastValues(long currentTime) {
        lastTime = currentTime;
        lastTotal = total;
        lastGets = gets;
        lastSets = sets;
        lastMultiGets = multiGets;
        lastServiceTime = serviceTime;
        lastServiceTimeSet = serviceTimeSet;
        lastServiceTimeGet = serviceTimeGet;
        lastServiceTimeMultiGet = serviceTimeMultiGet;
        lastResponseTime = responseTime;
        lastWaitingTime = waitingTime;
        lastQueueLength = queueLength;
        loggerIndex++;
    }

    private void total(long waitingTime, long nanoTime, int queueLength) {
        total++;
        this.waitingTime += waitingTime;
        this.serviceTime -= nanoTime;
        this.queueLength += queueLength;
    }


    public void set(long waitingTime, int queueLength) {
        sets++;
        method = 0;
        long nanoTime = System.nanoTime();
        serviceTimeSet -= nanoTime;
        total(waitingTime, nanoTime, queueLength);
    }

    public void get(long waitingTime, int queueLength) {
        gets++;
        method = 1;
        long nanoTime = System.nanoTime();
        serviceTimeGet -= nanoTime;
        total(waitingTime, nanoTime, queueLength);
    }

    public void multiGet(long waitingTime, int queueLength) {
        multiGets++;
        method = 2;
        long nanoTime = System.nanoTime();
        serviceTimeMultiGet -= nanoTime;
        total(waitingTime, nanoTime, queueLength);
    }

    public void done() {
        long nanoTime = System.nanoTime();
        serviceTime += nanoTime;

        // depending on last request type, add current time to finish -> serviceTIme = currentTime - startTime
        if(method == 0) {
            serviceTimeSet += nanoTime;
        } else if(method == 1) {
            serviceTimeGet += nanoTime;
        } else if(method == 2) {
            serviceTimeMultiGet += nanoTime;
        }
        method = -1;

        if((nanoTime-lastTime) >= resolution) {
            log(nanoTime);
        }
    }

    public void cacheHits(int hits, int keys) {
        cacheHits += hits;
        cacheLookups += keys;
//        logger.info("cache hit ratio " + hits + "/" + keys);
    }

    public void responseTime(long startTime) {
        long time = System.nanoTime() - startTime;
        responseTime += time;
        int index = (int)(time/100000L);

        index = Math.min(index, histBins-1);

        responseTimeHistogram[index]++;
    }

    public void errorMessage(String error) {
        errorMessages.add(error);
    }

    public int[] getHistogram() {
        return responseTimeHistogram;
    }

    public LinkedList<String> getErrorMessages() {
        return errorMessages;
    }

    public long getCacheHits(){
        return cacheHits;
    }

    public long getCacheLookups() {
        return cacheLookups;
    }
}

