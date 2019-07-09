package cc.alcina.framework.classmeta.rdb;

import java.util.Iterator;

public interface PacketEndpoint {

    default Iterator<Packet> packets(){
     return null;   
    }

    default boolean containsResponse(Packet packet) {
        throw new UnsupportedOperationException();
    }

    default void addReplyPacket(Packet translated) {
        throw new UnsupportedOperationException();
    }

    default void addOutPacket(Packet packet) {
        throw new UnsupportedOperationException();
    }

    default void setMustSend(boolean b) {
        throw new UnsupportedOperationException();
    }

    default boolean shouldSend() {
        throw new UnsupportedOperationException();
    }

    default void send() {
        throw new UnsupportedOperationException();
    }

}
