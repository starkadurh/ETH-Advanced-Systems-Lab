package ch.ethz.asltest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.*;
import java.util.AbstractMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * MWSocket encapsulates the socket a worker thread uses to communicate with a server.
 * It has a read and a write method to do so.
 * The read method is aware of the different types of response the server should send
 * In case the server is responding to a get request it parses the first line of the
 * request to get the data length and then proceeds to read that number of bytes.
 * When that number of bytes has been read, the response is returned to the worker thread.
 * For responses or set requests nothing needs to be done and the response is return.
 */
public class MWSocket {
    private Logger logger = LogManager.getLogger("MWLogger.MWSocket");

    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private StringBuilder stringBuilder;
    private String message;
    private char[] buf;

    private final static String CRLF = "" + (char) 0x0D + (char) 0x0A;

    public MWSocket() {
        socket = null;
        in = null;
        out = null;
        stringBuilder = null;
        buf = null;
    }

    public MWSocket(InetSocketAddress address) throws IOException {
        socket = new Socket(address.getHostName(), address.getPort());
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        stringBuilder = new StringBuilder();
        buf = new char[10000];
        logger.info("New MWSocket connection: " + socket);
    }

    public AbstractMap.SimpleImmutableEntry<String, Integer> read() throws IOException {

        message = in.readLine();

        int cacheHit = 0;
        if(message.startsWith("VALUE")) {

            stringBuilder.setLength(0);

             do {
                cacheHit++;

                stringBuilder.append(message + CRLF);
                int length = getDataLength() + CRLF.length();

                int totalRead = 0;
                while(length > 0) {
                    int read = in.read(buf,0, length);
                    stringBuilder.append(buf, 0, read);
                    length-=read;
                }

                stringBuilder.append(buf, 0, length);
                message = in.readLine();
            } while (message.startsWith("VALUE"));

            if (!message.startsWith("END")) {
                logger.error("Malformed response from server");
                throw new IOException();
            }
            message = stringBuilder.toString();
        }
        else if (message.startsWith("END")) {
            message = ""; //END is added in the worker
        }

        return new AbstractMap.SimpleImmutableEntry<>(message, cacheHit);
    }

    public void write(String request) {
        out.print(request);
        out.flush();
    }

    public int getDataLength() {
        String[] params = message.split(" ");
        try {
            return Integer.parseInt(params[params.length - 1]);
        } catch (NumberFormatException e) {
            logger.error("NumberFormatException: MWSocket.getDataLength()" + e);
            return -1;
        }
    }
}
