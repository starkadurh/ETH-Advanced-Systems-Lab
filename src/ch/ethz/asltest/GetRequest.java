package ch.ethz.asltest;

import java.nio.channels.SocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * GetRequest is a subtype of Request and is used for get requests
 * GetRequest object are capable of spiting it's keys into
 * smaller requests if needed for sharded reads.
 */
public class GetRequest extends Request {
    private Logger logger = LogManager.getLogger("MWLogger.SetRequest");

    private String method;
    private String[] keys;

    public GetRequest(SocketChannel socketChannel, String request) {
        super(socketChannel);
        String[] methodAndKeys = request.split(" ", 2);

        if(methodAndKeys.length != 2) {
            logger.error("IllegalArgumentException: Not a complete get request");
            throw new IllegalArgumentException();
        }

        if(!methodAndKeys[0].equals("get")) {
            logger.error("IllegalArgumentException: Not a get request");
            throw new IllegalArgumentException();
        }

        this.method = methodAndKeys[0];

        this.keys = methodAndKeys[1].trim().split(" ");
    }

    public String getMethod() {
        return method;
    }
    public String[] getKeys() {
        return keys;
    }

    public int keysCount() {
        return keys.length;
    }

    public void print() {
        System.out.print("\n-----\n");
        System.out.print("get ");

        for(String key : keys) {
            System.out.print(key + " ");
        }
        System.out.print("\n-----\n\n");
    }

    public String toString() {
        StringBuilder sb  = new StringBuilder();
        sb.append("get");
        for(String key : keys) {
            sb.append(" " + key);
        }
        sb.append(CRLF);
        return sb.toString();
    }

    // splits the keys in a  multi-get into split many get request strings
    // balances the number of keys in each request
    public String[] toStringMultiGet(int split) {
        StringBuilder sb  = new StringBuilder();
        split = Math.min(keys.length, split); // if there are only two keys but three servers we only want two strings
        String[] gets = new String[split];
        int keyIndex = 0;
        int offset = 0;
        for(int i = 0; i < split; i++) {
            sb.setLength(0);
            sb.append("get");

            int numberOfKeys = (int)Math.ceil(((double)keys.length - keyIndex) / (double)split);

            for(int j = offset; j < (offset + numberOfKeys) && j < keys.length; j++) {
                sb.append(" " + keys[j]);
            }

            offset = offset + numberOfKeys;
            sb.append(CRLF);

            gets[i] = sb.toString();
        }
        return gets;
    }

}
