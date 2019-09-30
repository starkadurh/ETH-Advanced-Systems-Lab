package ch.ethz.asltest;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.net.*;
import java.io.*;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Worker class extends thread and is the worker thread in the system
 * It takes requests from the queue and sends them to the server/s
 * If the request is a sharded multi-get requests it sends a shard of the
 * request to each of the servers before waiting for all of the replies.
 */
public class Worker extends Thread {
    private Logger logger = LogManager.getLogger("MWLogger.Worker");

    private final LinkedBlockingQueue<Request> requestQueue;
    private final List<InetSocketAddress> mcAddresses;
    private final boolean readSharded;
    private List<MWSocket> sockets;
    private int serverCount;
    private final static String CRLF = "" + (char) 0x0D + (char) 0x0A;
    private final StatsHandler statsHandler;

    public Worker(List<InetSocketAddress> mcAddresses, LinkedBlockingQueue<Request> requestQueue, boolean readSharded) {
        this.requestQueue = requestQueue; // The requests in a thread safe queue
        this.mcAddresses = mcAddresses;
        this.readSharded = readSharded;
        this.serverCount = mcAddresses.size();
        this.sockets = new ArrayList<>(serverCount);
        this.statsHandler = new StatsHandler(1, 10000);
    }

    @Override
    public void run() {
        MWSocket socket;
        Request request;
        AbstractMap.SimpleImmutableEntry<String,Integer> answerPair;
        String answer = null;
        int index;
        Coordinator coordinator = new Coordinator(serverCount);
        StringBuilder sb = new StringBuilder(); // for multiGet -> placed here for performance

        connectToServers();

        try {
            while (!Thread.currentThread().isInterrupted()) {

                request = requestQueue.take(); // take is blocking if requestQueue is empty
                int queueLength = requestQueue.size();
                long waitingTime = System.nanoTime() - request.getTimeStamp();

                if(request instanceof SetRequest) {
                    statsHandler.set(waitingTime,queueLength);

                    for(int i = 0; i < serverCount; i++) {
                        index = coordinator.nextWithMemory();
                        socket = sockets.get(index);
                        socket.write(request.toString());
                    }

                    boolean fail = false;
                    String failMessage = "";
                    while ((index = coordinator.nextFromMemory()) != -1) {
                        socket = sockets.get(index);
                        answerPair = socket.read();
                        answer = answerPair.getKey();
                        fail = fail || !answer.equals("STORED");
                        if(!fail && !answer.equals("STORED")) {
                            fail = true;
                            failMessage = answer;
                            logger.error(failMessage);
                        }
                    }
                    if (fail) {
                        answer = failMessage;
                    }
//                    statsHandler.done();

                } else if(request instanceof GetRequest) {

                    GetRequest getRequest = (GetRequest)request;

                    int keysCount = getRequest.keysCount();
                    if(readSharded && keysCount > 1) {
                        sb.setLength(0);

                        String[] multiGets = getRequest.toStringMultiGet(serverCount);
                        for(String get: multiGets) {
                            index = coordinator.nextWithMemory();
                            socket = sockets.get(index);
                            socket.write(get);
                        }

                        statsHandler.multiGet(waitingTime,queueLength);
                        int cacheHits = 0;
                        while ((index = coordinator.nextFromMemory()) != -1) {
                            socket = sockets.get(index);
                            answerPair = socket.read();
                            cacheHits += answerPair.getValue();
                            sb.append(answerPair.getKey());
                        }
//                        statsHandler.done();
                        sb.append("END");
                        answer = sb.toString();
                        statsHandler.cacheHits(cacheHits, keysCount);

                    } else {
                        statsHandler.get(waitingTime,queueLength);

                        socket = sockets.get(coordinator.next());
                        socket.write(request.toString());
                        answerPair = socket.read();
//                        statsHandler.done();
                        answer = answerPair.getKey() + "END";
                        statsHandler.cacheHits(answerPair.getValue(), keysCount);
                    }
                }

                statsHandler.done();
                sendResponse(request.getSocketChannel(), answer);
                statsHandler.responseTime(request.getTimeStamp());
            }
        } catch (IOException e) {
            logger.error("IOException ", e);
        } catch (Exception e) {
            logger.error("Exception ", e);
        }
    }

    private void sendResponse(SocketChannel socketChannel, String response) throws IOException {
        ByteBuffer buf = ByteBuffer.wrap((response + CRLF).getBytes());
        while (buf.hasRemaining()) {
            socketChannel.write(buf);
        }
    }

    private void connectToServers() {
        try {
            for (InetSocketAddress address : mcAddresses) {
                sockets.add(new MWSocket(address));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public StatsHandler getStatsHandler() {
        return statsHandler;
    }

    // Function for debugging
    private void stringToBytesPrint(String string) {
        byte[] bytes = string.getBytes();
        for(byte b: bytes) {
            System.out.print(b + " ");
        }
        System.out.println("");
    }

}

/**
 * Coordinator is the class that handles the round-robin to balance
 * the load between servers. It does this very naively by using modulo
 * If the Worker thread first needs to send to n servers and then read
 * from the same n servers, Coordinator offers the method nextWithMemory
 * and nextFromMemory returns the same indices in the same order
 */

class Coordinator {
    private final int serverCount;
    private int index;
    private ArrayDeque<Integer> socketsToReadFrom;

    public Coordinator(int serverCount) {
        this.serverCount = serverCount;
        this.index = 0;
        this.socketsToReadFrom = new ArrayDeque<>(serverCount);
    }

    public int next() {
        int retVal = index;
        index = (index + 1) % serverCount;
        return retVal;
    }

    public int nextWithMemory() {
        int value = next();
        socketsToReadFrom.push(value);
        return value;
    }

    public int nextFromMemory() {
        if(!socketsToReadFrom.isEmpty()){
            return socketsToReadFrom.pop();
        } else {
            return -1;
        }
    }

}
