package ch.ethz.asltest;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * The abstract class Request
 * Used to be able to have a data structure with type Request
 * to contain both GetRequest and SetRequest
 */
public abstract class Request {

    protected final static String CRLF = "" + (char) 0x0D + (char) 0x0A;
    private final SocketChannel socketChannel;
    private final long timeStamp;

    public Request() {
        this.socketChannel = null;
        this.timeStamp = System.nanoTime();
    }

    public Request(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
        this.timeStamp = System.nanoTime();
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public abstract String getMethod();
    public abstract void print();

    @Override
    public abstract String toString();

    public ByteBuffer toByteBuffer() {
        byte[] bs = this.toString().getBytes();
        return ByteBuffer.wrap(bs);
    }

    public long getTimeStamp() {
        return timeStamp;
    }
}
