package cc.alcina.framework.classmeta.rdb;

import java.util.List;

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

    public HttpAcceptorTransport(RdbEndpointDescriptor descriptor,
            Endpoint endpoint) {
        super(descriptor, endpoint);
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
        responseModel.eventListener = requestModel.eventListener;
        if (requestModel.eventListener) {
            synchronized (connectionPairMonitor) {
                listenerPair = pair;
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
            pair.response.setStatus(HttpServletResponse.SC_OK);
            pair.response.getWriter().write(payload);
            synchronized (pair) {
                pair.responseSent = true;
                pair.notify();
            }
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    static class HttpConnectionPair {
        public boolean responseSent;

        HttpServletRequest request;

        HttpServletResponse response;
    }
}