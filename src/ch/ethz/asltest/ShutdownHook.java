package ch.ethz.asltest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedList;


/**
 * ShutDownHook runs when the middleware is being shut down and collects
 * response time histogram data, cache miss rate and error messages
 * from the worker threads
 */
public class ShutdownHook extends Thread {
    private Logger logger = LogManager.getLogger("finalLogger");
    private Logger MWlogger = LogManager.getLogger("MWLogger.ShutdownHook");
    private final ArrayList<Worker> workers;
    private LinkedList<String> errorMessages = new LinkedList<>();
    private int[] histogram;
    private long cacheHits = 0;
    private long cacheLookups = 0;

    public ShutdownHook(ArrayList<Worker> workers) {
        this.workers = workers;
    }

    @Override
    public void run() {
        MWlogger.info("Running shutdown hook!");
        try {
            for (Worker worker : workers) {
                MWlogger.info("Processing: " + worker);
                StatsHandler statsHandler = worker.getStatsHandler();
                collectHistogramValues(statsHandler.getHistogram());
                errorMessages.addAll(statsHandler.getErrorMessages());
                cacheHits += statsHandler.getCacheHits();
                cacheLookups += statsHandler.getCacheLookups();
            }
            logHistogram();
            logger.info("Cache miss ratio: " +(double)(cacheLookups-cacheHits)/cacheLookups + " ("+(cacheLookups-cacheHits)+"/"+cacheLookups+")\n");

            logger.info("Error messages: ");
            for(String errorMessage : errorMessages) {
                logger.info(errorMessage);
            }
        } catch (Exception e) {
            logger.error("Something went wrong: " + e);
        }
        MWlogger.info("DONE! Goodbye.");
    }

    public void collectHistogramValues(int[] hist) {

        if(histogram == null) {
            histogram = new int[hist.length];
        }
        for(int i = 0; i < hist.length; i++) {
            histogram[i] += hist[i];
        }
    }

    public void logHistogram() {
        StringBuilder sb = new StringBuilder();
        long total = 0;
        sb.append("Response time histogram:\n");
        for(int i = 0; i < histogram.length; i++) {
            total += histogram[i];
            sb.append(histogram[i] + " ");
        }

        sb.append("\nRequests in total: " + total + "\n");
        logger.info(sb.toString());
    }
}
