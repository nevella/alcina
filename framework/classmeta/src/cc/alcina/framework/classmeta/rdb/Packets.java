package cc.alcina.framework.classmeta.rdb;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import cc.alcina.framework.classmeta.rdb.Packet.PacketPayload;

class Packets {
    private List<Packet> packets = new ArrayList<>();

    private transient Map<Integer, Packet> byIdCommand = new LinkedHashMap<>();

    private transient Map<Integer, Packet> byIdReply = new LinkedHashMap<>();

    private transient Map<PacketPayload, Packet> byPayload = new LinkedHashMap<>();

    public void clear() {
        packets.clear();
        ;
        byIdCommand.clear();
        byIdReply.clear();
        byPayload.clear();
    }

    public boolean hasPackets() {
        return packets.size() > 0;
    }

    synchronized void add(Packet packet) {
        packets.add(packet);
        if (packet.id() != 0) {
            if (packet.isReply) {
                byIdReply.put(packet.id(), packet);
            } else {
                byIdCommand.put(packet.id(), packet);
            }
            byPayload.put(packet.payload(), packet);
        }
    }

    synchronized Packet byId(int id, boolean command) {
        return command ? byIdCommand.get(id) : byIdReply.get(id);
    }

    synchronized Optional<Packet> byPayload(Packet other) {
        if (byPayload.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(byPayload.get(other.payload()));
    }

    synchronized Optional<Packet> byPayloadResponse(Packet packet) {
        Optional<Packet> byPayload = byPayload(packet);
        return byPayload
                .map(byPayloadCommand -> byId(byPayloadCommand.id(), false));
    }

    synchronized Optional<Packet> find(int commandSet, int commandId) {
        Optional<Packet> findFirst = packets.stream()
                .filter(p -> p.commandSet() == commandSet
                        && p.commandId() == commandId && p.fromDebugger)
                .findFirst();
        return findFirst;
    }

    synchronized void removeIf(Predicate<Packet> test) {
        // if (packets.isEmpty()) {
        // return;
        // }
        // packets.removeIf(test);
    }
}