package cc.alcina.framework.classmeta.rdb;

import java.util.List;

import cc.alcina.framework.classmeta.rdb.RdbProxies.RdbEndpointDescriptor;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.ResourceUtilities.SimplePost;
import cc.alcina.framework.entity.util.JacksonUtils;

class HttpInitiatorTransport extends Transport {
    private Thread receiver;

    public HttpInitiatorTransport(RdbEndpointDescriptor descriptor,
            Endpoint endpoint) {
        super(descriptor, endpoint);
    }

    @Override
    public void addPredictivePackets(List<Packet> predictivePackets) {
        if (predictivePackets.isEmpty()) {
            return;
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public void send() {
        // main loop, so blocks - no need to queue
        List<Packet> packets = packetEndpoint().flushOutPackets();
        if (!packets.isEmpty()) {
            HttpTransportModel model = new HttpTransportModel();
            model.passthroughPackets = packets;
            dispatchModel(model);
        }
    }

    private void maybeSimulateTransportDelay() throws InterruptedException {
        if (descriptor.transportDelay > 0) {
            Thread.sleep(descriptor.transportDelay);
        }
    }

    @Override
    protected void launch() {
        receiver = new Thread(
                Ax.format("%s::transport::receiver", descriptor.name)) {
            @Override
            public void run() {
                while (true) {
                    HttpTransportModel model = new HttpTransportModel();
                    model.eventListener = true;
                    dispatchModel(model);
                }
            }
        };
        receiver.start();
    }

    @Override
    void close() {
        HttpTransportModel model = new HttpTransportModel();
        model.close = true;
        dispatchModel(model);
    }

    void dispatchModel(HttpTransportModel model) {
        String url = descriptor.transportUrl;
        model.endpointName = descriptor.transportEndpointName;
        String payload = JacksonUtils.serialize(model);
        try {
            SimplePost post = new SimplePost(url, payload, null);
            maybeSimulateTransportDelay();
            String strResponse = post.asString();
            Ax.err("received: %s chars", strResponse.length());
            maybeSimulateTransportDelay();
            HttpTransportModel response = JacksonUtils.deserialize(strResponse,
                    HttpTransportModel.class);
            if (response.eventListener) {
                /*
                 * this is about the hardest bit - make sure we don't interrupt
                 * the free flow of predictive packets
                 * 
                 * cos once the debugger sends a packet that predictives can't
                 * handle, all bets are off...
                 * 
                 */
                packetEndpoint().waitForPredictivePacketMiss();
            }
            receivePredictivePackets(response.predictivePackets);
            response.passthroughPackets.forEach(this::receivePacket);
        } catch (Exception e) {
            e.printStackTrace();
            packetEndpoint().close();
        }
    }
}
