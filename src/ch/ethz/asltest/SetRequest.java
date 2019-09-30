package ch.ethz.asltest;

import java.nio.channels.SocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * SetRequets is a subtype of Request and is used for set requests
 */
public class SetRequest extends Request {
    private Logger logger = LogManager.getLogger("MWLogger.SetRequest");


    private String method;
    private String key = "";
    private int flags = -1;
    private int exptime = 0;
    private int bytes = 0;
    private String data = "";

    public SetRequest() {
        // default constructor
    }

    public SetRequest(SocketChannel socketChannel, String request) throws IllegalArgumentException{
        super(socketChannel);

        // check if request is shorter than shortest possible set request e.g. 'set 0 0 1 1'.length == 11
        if (request.length() < 11) {
            logger.error("IllegalArgumentException: Set request to short: ("+request+").length()=" + request.length());
            throw new IllegalArgumentException();
        }

        //check the method
        method = request.substring(0,3);

        if(!method.equals("set")) {
            logger.error("IllegalArgumentException: Not a set request");
            throw new IllegalArgumentException();
        }
        String[] paramsAndData = request.split(CRLF, 2);

        // Should always be two strings
        if(paramsAndData.length != 2) {
            logger.error("IllegalArgumentException: set request malformed");
            throw new IllegalArgumentException();
        }

        setVariables(paramsAndData[0]);

        this.data = stripTrailingCRLFifApplicable(paramsAndData[1]);

    }

    public SetRequest(SocketChannel socketChannel, String request, String data) throws IllegalArgumentException{
        super(socketChannel);

        // check if request is shorter than shortest possible set request e.g. 'set 0 0 1 1'.length == 11
        if (request.length() < 11) {
            logger.error("IllegalArgumentException: Set request to short: ("+request+").length()=" + request.length());
            throw new IllegalArgumentException();
        }
        method = request.substring(0,3);

        if(!method.equals("set")) {
            logger.error("IllegalArgumentException: Not a set request");
            throw new IllegalArgumentException();
        }

        setVariables(request);

        this.data = stripTrailingCRLFifApplicable(data);

    }

    public void setVariables(String params) throws IllegalArgumentException {
        setVariables(params.split(" "));
    }

    public void setVariables(String[] params) throws IllegalArgumentException {
        if (params.length == 5) {
                method = params[0];
                key = params[1];
                flags = Integer.parseInt(params[2]);
                exptime = Integer.parseInt(params[3]);
                bytes = Integer.parseInt(params[4]);
        } else {
            logger.error("IllegalArgumentException: Unable to create set request");
            throw new IllegalArgumentException();
        }
    }


    public int getDataLengthInBytes() {
        return bytes;
    }

    private String stripTrailingCRLFifApplicable(String data) {
        // remove trailing CRLF if data is 2 bytes longer than it should be and last 2 bytes are CRLF
        if(getDataLengthInBytes() == data.length()-2 &&
                data.lastIndexOf(CRLF) == data.length()-2) {
            return data.substring(0, getDataLengthInBytes());
        } else {
            return data;
        }
    }

    public void setData(String data) {
        this.data = stripTrailingCRLFifApplicable(data);
    }

    public String getMethod() {
        return method;
    }
    public String getData() {
        return data;
    }
    public String getKey() {
        return key;
    }
    public int getFlags() {
        return flags;
    }
    public int getExptime() {
        return exptime;
    }

    public void print() {
        System.out.print("\n-----\n");
        System.out.println(method + " " + key + " " + flags + " " + exptime + " " + bytes);
        System.out.println(data);
        System.out.print("-----\n\n");
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(method + " " + key + " " + flags + " " + exptime + " " + bytes + CRLF);
        sb.append(data + CRLF);
        return sb.toString();
    }
}
