import ch.ethz.asltest.GetRequest;
import ch.ethz.asltest.ReadHandler;
import ch.ethz.asltest.Request;
import ch.ethz.asltest.SetRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import java.util.LinkedList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ReadHandlerTest {

    private final static String CRLF = "" + (char) 0x0D + (char) 0x0A; // carriage return line feed
    private final static String setMessage = "set test 0 420 10"+CRLF+"xxxxxxxxxx"+CRLF;
    private final static String[] setRequestParams = {"set", "test", "0" , "420", "10"};
    private final static String startData = "xxxxxxxxxx";

    private final static String getRequestString = "get test" + CRLF;
    private final static String multiGetRequestString = "get test1 test2" + CRLF;
    private final static String[] getParams = {"get", "test"};
    private final static String[] multiGetParams = {"get", "test1", "test2"};

    @Disabled
    @Test
    void readCorrectSetRequestTest() {
        // To run this test the socketChannel read has to be temporarily removed
        ReadHandler rh = new ReadHandler();
//        assertEquals(0, rh.getRequestQueueLength());
        LinkedList<Request> requests = new LinkedList<Request>();

        LinkedList<Request> r = readHelper(rh, "set test");
        assertEquals(requests, r);
        r = readHelper(rh, " 0 420 10");
        assertEquals(requests, r);
        r = readHelper(rh, CRLF);
        assertEquals(requests, r);
        r = readHelper(rh, "xxxxx");
        assertEquals(requests, r);
        r = readHelper(rh, "xxxxx");
        assertEquals(requests, r);
        r = readHelper(rh, CRLF);

        requests.add(new SetRequest(null, "set test 0 420 10","xxxxxxxxxx"));
        assertEquals(requests.size(), r.size());
        assertEquals(requests.getFirst().toString(), r.getFirst().toString());
    }

    @Disabled
    @Test
    void readIncorrectSetRequestTest() {
        // To run this test the socketChannel read has to be temporarily removed
        ReadHandler rh = new ReadHandler();
//        assertEquals(0, rh.getRequestQueueLength());
        LinkedList<Request> requests = new LinkedList<Request>();

        LinkedList<Request> r = readHelper(rh, "wrong test");
        assertEquals(requests, r);
        r = readHelper(rh, " 0 420 10");
        assertEquals(requests, r);
        r = readHelper(rh, CRLF);
        assertEquals(requests, r);
        r = readHelper(rh, "xxxxx");
        r = readHelper(rh, CRLF);
        assertEquals(requests, r);
        assertEquals(requests.size(), r.size());

        // correct set request {
        r = readHelper(rh, "set test");
        assertEquals(requests, r);
        r = readHelper(rh, " 0 420 10");
        assertEquals(requests, r);
        r = readHelper(rh, CRLF);
        assertEquals(requests, r);
        r = readHelper(rh, "xxxxx");
        assertEquals(requests, r);
        r = readHelper(rh, "xxxxx");
        assertEquals(requests, r);
        r = readHelper(rh, CRLF);

        requests.add(new SetRequest(null, "set test 0 420 10","xxxxxxxxxx"));
        assertEquals(requests.size(), r.size());
        assertEquals(requests.getFirst().toString(), r.getFirst().toString());
        // }
    }

    @Disabled
    @Test
    void readCorrectGetRequestTest() {
        // To run this test the socketChannel read has to be temporarily removed
        ReadHandler rh = new ReadHandler();
//        assertEquals(0, rh.getRequestQueueLength());
        LinkedList<Request> requests = new LinkedList<Request>();

        LinkedList<Request> r = readHelper(rh, "get test");
        assertEquals(requests, r);
        r = readHelper(rh, " test2");
        assertEquals(requests, r);
        r = readHelper(rh, CRLF);

        requests.add(new GetRequest(null, "get test test2"));
        assertEquals(requests.size(), r.size());
        assertEquals(requests.getFirst().toString(), r.getFirst().toString());
    }

    @Disabled
    @Test
    void readIncorrectGetRequestTest() {
        // To run this test the socketChannel read has to be temporarily removed
        ReadHandler rh = new ReadHandler();
//        assertEquals(0, rh.getRequestQueueLength());
        LinkedList<Request> requests = new LinkedList<Request>();

        LinkedList<Request> r = readHelper(rh, "fet test");
        assertEquals(requests, r);
        r = readHelper(rh, " test2");
        assertEquals(requests, r);
        r = readHelper(rh, CRLF);
        assertEquals(requests, r);

        // correct set request {
        r = readHelper(rh, "get test");
        assertEquals(requests, r);
        r = readHelper(rh, " test2");
        assertEquals(requests, r);
        r = readHelper(rh, CRLF);

        requests.add(new GetRequest(null, "get test test2"));
        assertEquals(requests.size(), r.size());
        assertEquals(requests.getFirst().toString(), r.getFirst().toString());
        assertEquals(requests.getLast().toString(), r.getLast().toString());
        // }
    }
    @Test
    void extractMessageTest() {
        StringBuilder sb = new StringBuilder();
        ReadHandler rh = new ReadHandler();
        rh.setStringBuilder(sb);

        // StringBuilder is empty
        assertEquals("Not Recognized", rh.extractMessage());

        // Add set request and data to stringBuilder
        rh.appendToStringBuilder(setMessage);

        // StringBuilder should return set request
        assertEquals("set test 0 420 10", rh.extractMessage());
        // StringBuilder only contains data and should return "Not Recognized"
        assertEquals("Not Recognized", rh.extractMessage());


        rh.setStringBuilder(new StringBuilder());
        rh.appendToStringBuilder(getRequestString);

        assertEquals("get test", rh.extractMessage());
    }

    @Test
    void extractDataTest() {
        StringBuilder sb = new StringBuilder();
        ReadHandler rh = new ReadHandler();
        rh.setStringBuilder(sb);

        // StringBuilder should be empty
        assertEquals(null, rh.extractData(1));

        // Add set request and data to stringBuilder
        rh.appendToStringBuilder(setMessage);

        // stringBuilder should return set request
        assertEquals("set test 0 420 10", rh.extractMessage());

        // StringBuilder only contains data "Not Recognized"
        assertEquals(null, rh.extractData(9));
        assertEquals(null, rh.extractData(11));
        assertEquals("xxxxxxxxxx", rh.extractData(10));
        assertEquals(null, rh.extractData(0));

        // Empty data
        rh.appendToStringBuilder("set test 0 420 0"+CRLF+CRLF);
        assertEquals("set test 0 420 0", rh.extractMessage());
        assertEquals("", rh.extractData(0));

    }

    @Test
    void getDataLengthTest() {
        ReadHandler rh = new ReadHandler();

        rh.setMessage("set test 0 420 10");
        int length = rh.getDataLength();
        assertEquals(10, length);

        rh.setMessage("set test 0 420 10"+CRLF);
        length = rh.getDataLength();
        assertEquals(-1, length);

        rh.setMessage("xxxxxxxxx10");
        length = rh.getDataLength();
        assertEquals(-1, length);

        rh.setMessage("10");
        length = rh.getDataLength();
        assertEquals(10, length);

    }

    @Test
    void incompleteRequestDataTest() {

        StringBuilder sb = new StringBuilder();
        ReadHandler rh = new ReadHandler();
        rh.setStringBuilder(sb);

        // StringBuilder should be empty
        assertEquals("Not Recognized", rh.extractMessage());
        assertEquals(null, rh.extractData(1));

        // Add set request and data to stringBuilder
        rh.appendToStringBuilder("set test");
        assertEquals("Not Recognized", rh.extractMessage());

        rh.appendToStringBuilder(" 0 420 10");
        assertEquals("Not Recognized", rh.extractMessage());

        rh.appendToStringBuilder(CRLF);
        assertEquals("set test 0 420 10", rh.extractMessage());

        rh.appendToStringBuilder("xxxxx");
        assertEquals(null, rh.extractData(12));

        rh.appendToStringBuilder(CRLF);
        assertEquals(null, rh.extractData(12));

        rh.appendToStringBuilder("xxxxx");
        assertEquals(null, rh.extractData(12));

        rh.appendToStringBuilder(CRLF);
        assertEquals("xxxxx"+CRLF+"xxxxx", rh.extractData(12));
    }

    @Test
    void largeRequestDataTest() {

        StringBuilder sb = new StringBuilder();
        ReadHandler rh = new ReadHandler();
        rh.setStringBuilder(sb);

        // StringBuilder should be empty
        assertEquals("Not Recognized", rh.extractMessage());
        assertEquals(null, rh.extractData(1));

        // Add set request and data to stringBuilder
        rh.appendToStringBuilder("set test");
        assertEquals("Not Recognized", rh.extractMessage());

        rh.appendToStringBuilder(" 0 420 1024");
        assertEquals("Not Recognized", rh.extractMessage());

        rh.appendToStringBuilder(CRLF);
        assertEquals("set test 0 420 1024", rh.extractMessage());

        int len = 4069;
        String s = Stream.generate(() -> String.valueOf('x')).limit(len).collect(Collectors.joining());
        rh.appendToStringBuilder(s);
        assertEquals(null, rh.extractData(len));

        rh.appendToStringBuilder(CRLF);
        assertEquals(s, rh.extractData(len));
    }
    private LinkedList<Request> readHelper(ReadHandler rh, String s) {
        rh.appendToStringBuilder(s);
        try {
            return rh.read();
        } catch (Exception e) {
            fail(e);
            return null;
        }
    }
}