package cc.alcina.framework.classmeta.rdb;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cc.alcina.framework.classmeta.rdb.RdbProxies.RdbEndpointDescriptor;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.entity.util.JacksonUtils;

class HttpAcceptorTransport extends Transport {
    HttpConnectionPair listenerPair;

    HttpConnectionPair commandPair;

    Object connectionPairMonitor = new Object();

    HttpTransportModel responseModel = new HttpTransportModel();

    private Timer notificationTimer = new Timer();

    long lastListenerCall = 0;

    public HttpAcceptorTransport(RdbEndpointDescriptor descriptor,
            Endpoint endpoint) {
        super(descriptor, endpoint);
        notificationTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (lastListenerCall != 0) {
                    endpoint.nudge();
                }
            }
        }, descriptor.transportNotificationBundleWait,
                descriptor.transportNotificationBundleWait);
    }

    @Override
    public synchronized void addPredictivePackets(
            List<Packet> predictivePackets) {
        if (predictivePackets.isEmpty()) {
            return;
        }
        synchronized (responseModel) {
            responseModel.predictivePackets.addAll(predictivePackets);
        }
    }

    public void receiveTransportModel(HttpTransportModel requestModel,
            HttpConnectionPair pair) {
        if (requestModel.close) {
            pair.response.setContentType("application/json");
            pair.response.setStatus(HttpServletResponse.SC_OK);
            try {
                pair.response.getWriter().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (lastListenerCall == 0) {
                // received 'close' on new listener/endpoint - could make this
                // drop more precise with a per debugger/debuggee uid but
                // probably no need
                //
                // ignore
                return;
            }
            packetEndpoint().close();
            return;
        }
        responseModel.eventListener = requestModel.eventListener;
        if (requestModel.eventListener) {
            synchronized (connectionPairMonitor) {
                listenerPair = pair;
                lastListenerCall = System.currentTimeMillis();
                connectionPairMonitor.notify();
            }
        } else {
            synchronized (connectionPairMonitor) {
                commandPair = pair;
                requestModel.passthroughPackets.forEach(this::receivePacket);
                connectionPairMonitor.notify();
            }
        }
        /*
         * wait for the acceptor's endpoint's main loop to call send()
         */
        while (true) {
            synchronized (pair) {
                if (pair.responseSent) {
                    return;
                }
                try {
                    pair.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    synchronized public void send() {
        HttpConnectionPair pair = null;
        while (pair == null) {
            if (commandPair != null) {
                pair = commandPair;
                commandPair = null;
                break;
            }
            if (listenerPair != null) {
                pair = listenerPair;
                listenerPair = null;
                break;
            }
            synchronized (connectionPairMonitor) {
                try {
                    connectionPairMonitor.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        String payload = null;
        synchronized (responseModel) {
            List<Packet> packets = packetEndpoint().flushOutPackets();
            responseModel.passthroughPackets = packets;
            payload = JacksonUtils.serialize(responseModel);
            responseModel = new HttpTransportModel();
        }
        try {
            pair.response.setContentType("application/json");
            pair.response.getWriter().write(payload);
            pair.response.setStatus(HttpServletResponse.SC_OK);
            synchronized (pair) {
                pair.responseSent = true;
                pair.notify();
            }
        } catch (Exception e) {
            packetEndpoint().close();
            throw new WrappedRuntimeException(e);
        }
    }

    @Override
    public boolean shouldSend() {
        // don't lock - worst that can happen is that we send an unneccesarily
        // early reply. Makes logic muuuch simpler
        //
        // if this call returns false (because we have a recent listener call),
        // wait a bit
        if (commandPair != null) {
            return true;
        } else {
            boolean should = lastListenerCall != 0 && System.currentTimeMillis()
                    - lastListenerCall > descriptor.transportNotificationBundleWait;
            if (should) {
                logger.info("Sending event notifications - {} events",
                        packetEndpoint.outPacketCount());
            }
            return should;
        }
    }

    @Override
    synchronized void close() {
        closed = true;
        if (notificationTimer != null) {
            notificationTimer.cancel();
            notificationTimer = null;
        }
        if (commandPair != null) {
            try {
                commandPair.response.getOutputStream().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            commandPair = null;
        }
        if (listenerPair != null) {
            try {
                listenerPair.response.getOutputStream().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            listenerPair = null;
        }
    }

    static class HttpConnectionPair {
        public boolean responseSent;

        HttpServletRequest request;

        HttpServletResponse response;
    }
}