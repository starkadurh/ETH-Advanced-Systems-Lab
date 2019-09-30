import ch.ethz.asltest.MyMiddleware;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;

class MyMiddlewareTest {

    private MyMiddleware mw = null;

    @BeforeEach
    void setUp() {
        String myIp = "localhost";
        int myPort = 12345;
        List<String> mcAddresses = new ArrayList<>();
        mcAddresses.add("localhost:11211");
        int numThreadsPTP = 1;
        boolean readSharded = false;
        this.mw = new MyMiddleware(myIp, myPort, mcAddresses, numThreadsPTP, readSharded);
    }

    @AfterEach
    void tearDown() {

    }

    @Test
    public void run() {
        try {
            Class c = Class.forName("ch.ethz.asltest.MyMiddleware");
            Method method = c.getDeclaredMethod("createWorkerThreads", null);
            method.setAccessible(true);
            Object obj = method.invoke(mw);
            System.out.println(obj.toString());
        } catch (Exception e) {

        }

    }

}