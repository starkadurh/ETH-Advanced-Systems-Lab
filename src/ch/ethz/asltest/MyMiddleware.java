package ch.ethz.asltest;

import java.nio.channels.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.lang.Runtime;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * MyMiddleware is the heart of the middleware
 * It creates worker threads, registers the shutdown hook, creates the request queue, accepts incoming connections
 * and handles incoming requests. It uses a NIO Selector and a ServerSocketChannel to handle incoming connections.
 * For each new connection it creates a SocketChannel and a ReadHandler for that SocketChannel.
 */
public class MyMiddleware {
    private static Logger logger = LogManager.getLogger("MWLogger.MyMiddleware");

    private final ArrayList<Worker> workers;

    private final InetSocketAddress serverAddress;
    private Selector selector;

    private final List<InetSocketAddress> mcAddresses;
    private final int numThreadsPTP;
    private final boolean readSharded;

    public final LinkedBlockingQueue<Request> requestQueue;

    public MyMiddleware(String myIp, int myPort, List<String> mcAddresses, int numThreadsPTP, boolean readSharded){
        this.serverAddress = new InetSocketAddress(myIp, myPort);
        this.mcAddresses = parseMCAddresses(mcAddresses);
        this.numThreadsPTP = numThreadsPTP;
        this.readSharded = readSharded;
        this.requestQueue = new LinkedBlockingQueue<Request>();
        this.workers = new ArrayList<>(numThreadsPTP);

    }

    public void run() {
        logger.info("Running Middleware");

        createWorkerTreads();

        createShutdownHook();

        registerServerSocketChannel();

        serverLoop();

    }

    private void createShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHook(workers)));
    }

    private void createWorkerTreads() {
        // Creating numThreadsPTP worker threads to handle requests
        logger.info("Creating " + numThreadsPTP + " worker threads");
        for (int i = 0; i < numThreadsPTP; i++) {
            Worker worker = new Worker(mcAddresses, requestQueue, readSharded);
            worker.start();
            workers.add(i, worker);
        }
    }

    private void registerServerSocketChannel() {

        logger.info("Creating ServerSocketChannel");
        // Creating ServerSocket that receive requests and puts them in the request queue
        try {
            // Server Selector
            selector = Selector.open();

            // Server Socket Channel to accept new connections
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(serverAddress, 10000);
            serverSocketChannel.configureBlocking(false); // make none blocking

            // Register Server Socket Channel to Server Selector
            SelectionKey key = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        } catch (IOException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
    }

    private void serverLoop() {
        logger.info("Starting server loop");

        int counter=0;
        setMWRuningState("running");
        // main server loop
        while (true) {
            try {
                // Blocking method that waits until any channel is ready for an action
                int readyChannels = selector.select();
                if (readyChannels == 0) continue; // if no channel is ready, continue


                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();
                    if (!key.isValid()) continue;

                    // Server Socket Channel has a new connection, Creating new Socket channel
                    if (key.isAcceptable()) {

                        ServerSocketChannel channel = (ServerSocketChannel) key.channel();
                        SocketChannel socketChannel = channel.accept();
                        logger.info("New Connection from " + socketChannel);
                        socketChannel.configureBlocking(false); // make none blocking

                        // Register new Socket Channel to Server Selector to allow reading from new connection
                        SelectionKey k = socketChannel.register(selector, SelectionKey.OP_READ, new ReadHandler(socketChannel));
                    }

                    // Socket Channel has readable data, reading from socket
                    else if (key.isReadable()) {
                        if(key.channel() instanceof SocketChannel) {
                            ReadHandler rh = (ReadHandler) key.attachment();
                            LinkedList<Request> requests = rh.read();
                            if(requests != null && requests.size() > 0) {
                                requestQueue.addAll(requests);
                                counter += requests.size();
                                if(counter%10000 == 0) {
                                    logger.info(counter + " requests received");
                                }
                            }
                        }
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    private List<InetSocketAddress> parseMCAddresses(List<String> mcAddresses) {
        List<InetSocketAddress> addresses = new ArrayList<>();

        // Split string into host and port
        for(String s: mcAddresses) {
            String host = (s.split(":"))[0];
            int port = Integer.parseInt(s.split(":")[1]);
            addresses.add(new InetSocketAddress(host, port));
        }

        return addresses;
    }

    public static void setMWRuningState(String state) {
        try {
            PrintWriter writer = new PrintWriter("/tmp/middleware", "UTF-8");
            writer.println("running");
            writer.close();
        } catch (IOException e) {
            logger.error(e);
        }
    }

}
