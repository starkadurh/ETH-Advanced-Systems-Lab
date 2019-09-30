package ch.ethz.asltest;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.LinkedList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *  ReadHander is an object that is used with each SocketChannel to parse requests,
 * create GetRequest or SetRequest objects and store partial requests between reads
 * The functionality of storing the partial requests is espesially important as it
 * allows the net-thead to continue reading from other SocketChannels while the
 * rest of a request is being sent.
 */
public class ReadHandler {
    private Logger logger = LogManager.getLogger("MWLogger.ReadHandler");

    private final SocketChannel socketChannel;
    private final ByteBuffer buf;
    public final static String CRLF = "" + (char) 0x0D + (char) 0x0A; // carriage return line feed

    private StringBuilder stringBuilder;
    private String message = "Not Recognized";

    public ReadHandler() {
        // default constructor only used for unit testing
        this.socketChannel = null;
        this.buf = ByteBuffer.allocate(10000);
        this.stringBuilder = new StringBuilder(10000);
    }

    public ReadHandler(SocketChannel socketChannel)  {
        this.socketChannel = socketChannel;
        // buffer of 5000 bytes should be enough as the largest set request with data can not exceed 1.332 bytes
        this.buf = ByteBuffer.allocate(10000);
        this.stringBuilder = new StringBuilder(10000);
    }

    public LinkedList<Request> read() throws IOException {
        char ch;
        int bytesRead = socketChannel.read(buf);
        if (bytesRead == -1) {
            logger.info("Closing SocketChannel " + socketChannel);
            socketChannel.close();
            return null;
        }
        while (bytesRead > 0) {

            buf.flip();
            while (buf.hasRemaining()) {
                ch = (char)buf.get();
                stringBuilder.append(ch);
            }
            buf.clear();

            bytesRead = socketChannel.read(buf);
        }


        LinkedList<Request> requests = new LinkedList<>();
        Request request = null;
        do {
            if (message.equals("Not Recognized")) {
                message = extractMessage();
            }

            request = this.handleMessage(message);
            if (request != null) {
                // message was handled and is reset.
                message = "Not Recognized";
                requests.add(request);
            }
        } while (request != null);
        return requests;
    }

    public boolean isRecognizedMessage(String string) {
        return (string.startsWith("set") || string.startsWith("get"));
    }

    public Request handleMessage(String requestString) {
        try {
            if (requestString.startsWith("set")) {
                int length = getDataLength();

                if (length >= 0) {
                    String data = extractData(length);

                    if (data != null) {
                        return new SetRequest(getSocketChannel(), requestString, data);
                    }
                }

            } else if (requestString.startsWith("get")) {
                return new GetRequest(getSocketChannel(), requestString);
            }

            return null;
        } catch (IllegalArgumentException e) {
            stringBuilder.setLength(0);
            logger.error(e);
            return null;
        }
    }

    public String extractMessage() {
        int indexOfCRLF = stringBuilder.indexOf(CRLF);
        if(indexOfCRLF != -1) {

            String response = stringBuilder.substring(0, indexOfCRLF);
            stringBuilder = stringBuilder.delete(0, indexOfCRLF + CRLF.length());
            if(this.isRecognizedMessage(response)) {
                return response;
            }
            return "Not Recognized";
        }
        return "Not Recognized";
    }

    public int getDataLength() {
        String[] params = message.split(" ");
        try {
            return Integer.parseInt(params[params.length - 1]);
        } catch (NumberFormatException e) {
            logger.error("NumberFormatException in ch.ethz.asltest.ReadHandler.getDataLength() " + e);
            return -1;
        }
    }

    public String extractData(int length) {
        if(stringBuilder.length() >= length + CRLF.length() &&
                stringBuilder.substring(length,length+CRLF.length()).equals(CRLF)) {
            String data = stringBuilder.substring(0, length);
            stringBuilder = stringBuilder.delete(0, length + CRLF.length());
            return data;
        }
        return null;
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    // functions used for unit testing
    public void setStringBuilder(StringBuilder sb) {
        this.stringBuilder = sb;
    }

    public void appendToStringBuilder(String string) {
        stringBuilder.append(string);
    }

    public void setMessage(String string) {
        this.message = string;
    }

    public String getMessage() {
        return message;
    }

}
