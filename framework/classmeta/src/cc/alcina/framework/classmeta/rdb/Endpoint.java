package cc.alcina.framework.classmeta.rdb;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sun.tools.jdi.VirtualMachineImplExt;

import cc.alcina.framework.classmeta.rdb.RdbProxies.RdbEndpointDescriptor;
import cc.alcina.framework.common.client.WrappedRuntimeException;

abstract class Endpoint {
    protected Transport transport;

    protected JdwpStreams streams;

    protected RdbEndpointDescriptor descriptor;

    protected VirtualMachineImplExt vm = new VirtualMachineImplExt();

    PacketCategories categories = new PacketCategories();

    public Endpoint(RdbEndpointDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    void launch() {
        startJdwpStream();
        startTransportStream();
        try {
            startMainLoop();
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    private Object trafficMonitor = new Object();

    private void startMainLoop() throws Exception {
        while (true) {
            synchronized (trafficMonitor) {
                trafficMonitor.wait();
                inPackets(streams);
                inPackets(transport);
                outPackets(streams);
                outPackets(transport);
            }
            //
        }
    }

    private void outPackets(PacketEndpoint packetEndpoint) {
        if(packetEndpoint.shouldSend()){
            packetEndpoint.send();
        }
    }

    private void inPackets(PacketEndpoint packetEndpoint) {
        PacketEndpoint otherEndpoint = otherEndpoint(packetEndpoint);
        Iterator<Packet> packets = packetEndpoint.packets();
        while(packets.hasNext()){
            //this is the naive (raw) packet from the endpoint. 
            Packet packet = packets.next();
            //what's our coordinate space, Scotty?
            categories.analysePacket(this,packetEndpoint,packet);
            if(otherEndpoint.containsResponse(packet)){
                Packet translated =packet.translate(otherEndpoint,packetEndpoint);
                packetEndpoint.addReplyPacket(translated);
                continue;
            }
            packetEndpoint.addOutPacket(packet);
            if(packet.meta.mustSend){
                packetEndpoint.setMustSend(true);
                break;
            }
        }
    }

    private PacketEndpoint otherEndpoint(PacketEndpoint packetEndpoint) {
        return packetEndpoint==streams?transport:streams;
    }

    private void startTransportStream() {
        // FIXME - e.g. debuggee sharedvm - this
    }

    protected void startJdwpStream() {
        // FIXME - implement
    }
}
