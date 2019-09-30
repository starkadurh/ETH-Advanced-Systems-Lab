import ch.ethz.asltest.GetRequest;
import ch.ethz.asltest.Request;
import ch.ethz.asltest.SetRequest;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class RequestTest {
    public Request request;
    private final static String CRLF = "" + (char) 0x0D + (char) 0x0A; // carriage return line feed

    private final static String setRequest = "set test 0 420 10";
    private final static String setRequestData = "xxxxxxxxxx";
    private final static String setRequestString = setRequest +CRLF+ setRequestData +CRLF;
    private final static String[] setRequestParams = {"set", "test", "0" , "420", "10"};

    private final static String getRequestString = "get test" + CRLF;
    private final static String multiGetRequestString = "get test1 test2" + CRLF;
    private final static String[] getParams = {"get", "test"};
    private final static String[] multiGetParams = {"get", "test1", "test2"};

    @Test
    void setRequestConstructorStringTest() {
        // Testing set requests
        SetRequest sr = new SetRequest(null, setRequestString);
        testParameters(sr);

        String[] params = {"set", "noDataTest", "2", "1", "0"};
        sr = new SetRequest(null, "set noDataTest 2 1 0" + CRLF);
        testParameters(sr, params);

        sr = new SetRequest(null, "set noDataTest 2 1 0" + CRLF + CRLF);
        testParameters(sr, params);

    }

    @Test
    void setRequestConstructorTwoStringsTest() {
        // Testing set requests
        SetRequest sr = new SetRequest(null, setRequest, setRequestData);
        testParameters(sr);

        String[] params = {"set", "noDataTest", "2", "1", "0"};

        sr = new SetRequest(null, "set noDataTest 2 1 0"+CRLF);
        testParameters(sr, params);

        sr = new SetRequest(null, "set noDataTest 2 1 0"+CRLF+CRLF);
        testParameters(sr, params);

    }

//    @Test
//    void setRequestConstructorStringArrayTest() {
//        // Testing set requests
//        ch.ethz.asltest.SetRequest r = new ch.ethz.asltest.SetRequest(setRequestParams);
//        r.setData(setRequestData);
//        testParameters(r);
//
//        // Testing get requests
//
//    }

    @Test
    void setRequestConstructorEqualityTest() {
        SetRequest r1 = new SetRequest(null, setRequestString);
        SetRequest r2 = new SetRequest(null, setRequest, setRequestData);
        testParameters(r1, r2);
    }

    @Test
    void setVariablesTest() {
        String[] params = {"set", "changed", "10" , "500000", "5"};
        SetRequest r = new SetRequest(null, setRequestString);
        r.setVariables(params);

        testParameters(r, params);

    }

//    @Test
//    void setDataTest() {
//        ch.ethz.asltest.SetRequest r = new ch.ethz.asltest.SetRequest(setRequestParams);
//
//        r.setData(setRequestData);
//        assertEquals(setRequestData, r.getData());
//
//        r.setData(setRequestData +CRLF);
//        assertEquals(setRequestData, r.getData());
//
//    }


    @Test
    void getRequestConstructorStringTest() {

        // Testing get requests
        GetRequest gr = new GetRequest(null, getRequestString);
        testParameters(gr, getParams);

        gr = new GetRequest(null, multiGetRequestString);
        testParameters(gr, multiGetParams);
    }

    @Test
    void toStringTest() {

        Request r = new GetRequest(null, getRequestString);
        assertEquals(getRequestString, r.toString());

        r = new SetRequest(null, setRequestString);
        assertEquals(setRequestString, r.toString());
    }

    private void testParameters(Request r, String[] params) {
        assertEquals(params[0], r.getMethod());

        SetRequest sr;
        if(r instanceof SetRequest) {
            sr = (SetRequest) r;
            assertEquals(params[1], sr.getKey());
            assertEquals(Integer.parseInt(params[2]), sr.getFlags());
            assertEquals(Integer.parseInt(params[3]), sr.getExptime());
            assertEquals(Integer.parseInt(params[4]), sr.getDataLengthInBytes());
        } else {
            GetRequest gr = (GetRequest) r;
            String[] keysRef = Arrays.copyOfRange(params, 1, params.length);
            String[] keys = gr.getKeys();
            testKeys(keysRef, keys);
        }
    }

    private void testParameters(Request r) {


        if(r instanceof SetRequest) {
            SetRequest sr = (SetRequest) r;
            assertEquals("set", r.getMethod());
            assertEquals("test", sr.getKey());
            assertEquals(0, sr.getFlags());
            assertEquals(420, sr.getExptime());
            assertEquals(10, sr.getDataLengthInBytes());
            assertEquals("xxxxxxxxxx".length(), sr.getDataLengthInBytes());
            assertEquals("xxxxxxxxxx", sr.getData());
        } else {
            GetRequest gr = (GetRequest) r;
            assertEquals("get", r.getMethod());
            testKeys(new String[]{"test"}, gr.getKeys());
        }
    }

    private void testParameters(Request r1, Request r2) {
        assertEquals(r1.getMethod(), r2.getMethod());

        if(r1 instanceof SetRequest && r2 instanceof SetRequest) {
            SetRequest sr1 = (SetRequest) r1;
            SetRequest sr2 = (SetRequest) r2;
            assertEquals(sr1.getKey(), sr2.getKey());
            assertEquals(sr1.getFlags(), sr2.getFlags());
            assertEquals(sr1.getExptime(), sr2.getExptime());
            assertEquals(sr1.getDataLengthInBytes(), sr2.getDataLengthInBytes());
            assertEquals(sr1.getData(), sr2.getData());
        }
    }

    private void testKeys(String[] keysRef, String[] keys) {
        assertEquals(keysRef.length, keys.length);
        for(int i = 0; i < keys.length; i++) {
            assertEquals(keysRef[i], keys[i]);
        }
    }
}