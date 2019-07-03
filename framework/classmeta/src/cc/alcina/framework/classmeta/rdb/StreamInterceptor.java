package cc.alcina.framework.classmeta.rdb;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import cc.alcina.framework.classmeta.rdb.RdbProxy.StreamListener;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.util.AtEndOfEventSeriesTimer;

class StreamInterceptor {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    byte[] lastSent;

    String name;

    InputStream fromStream;

    OutputStream toStream;

    StreamListener streamListener;

    private AtEndOfEventSeriesTimer seriesTimer = new AtEndOfEventSeriesTimer(
            5, new Runnable() {
                @Override
                public void run() {
                    writeToToStream();
                }
            }).maxDelayFromFirstAction(5);

    StreamInterceptor(String name, StreamListener streamListener,
            InputStream inputStream, OutputStream outputStream) {
        this.name = name;
        this.streamListener = streamListener;
        this.fromStream = inputStream;
        this.toStream = outputStream;
        Ax.out("[%s] : %s >> %s", name, fromStream, toStream);
    }

    protected void writeToToStream() {
        try {
            synchronized (buffer) {
                if (buffer.size() == 0) {
                    return;
                }
                byte[] bytes = buffer.toByteArray();
                buffer.reset();
                lastSent = bytes;
                toStream.write(bytes);
                toStream.flush();
            }
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    void start() {
        new Thread(name) {
            @Override
            public void run() {
                try {
                    while (true) {
                        int b = fromStream.read();
                        if (b == -1) {
                            break;
                        }
                        streamListener.byteReceived(StreamInterceptor.this,
                                b);
                        synchronized (buffer) {
                            buffer.write(b);
                        }
                        seriesTimer.triggerEventOccurred();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}