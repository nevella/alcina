package cc.alcina.framework.classmeta.rdb;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.classmeta.rdb.Packet.PacketPair;
import cc.alcina.framework.classmeta.rdb.Packet.PacketPayload;
import cc.alcina.framework.common.client.util.Ax;

class Packets {
    private List<Packet> packets = new ArrayList<>();

    private transient Map<Integer, Packet> byIdCommand = new LinkedHashMap<>();

    private transient Map<Integer, Packet> byIdReply = new LinkedHashMap<>();

    private Set<Integer> recentIds = new LinkedHashSet<>();

    private transient Map<PacketPayload, Packet> byPayload = new LinkedHashMap<>();

    public void clear() {
        packets.clear();
        byIdCommand.clear();
        byIdReply.clear();
        byPayload.clear();
    }

    public void clearRecentList() {
        recentIds.clear();
    }

    public boolean hasPackets() {
        return packets.size() > 0;
    }

    public Stream<Packet> streamRecentReplies() {
        return recentIds.stream().filter(byIdReply::containsKey)
                .map(id -> byIdReply.get(id));
    }

    private void removeFromLookups(Packet packet) {
        if (packet.id() != 0) {
            byIdReply.remove(packet.id());
            byIdCommand.remove(packet.id());
            byPayload.remove(packet.payload());
        }
    }

    synchronized void add(Packet packet) {
        recentIds.add(packet.id());
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

    synchronized List<Packet> listByIds(int commandSet, int commandId) {
        return packets.stream()
                .filter(p -> p.commandSet() == commandSet
                        && p.commandId() == commandId && p.fromDebugger)
                .collect(Collectors.toList());
    }

    synchronized void removeIf(Predicate<Packet> test,
            int predictivePacketsHit) {
        if (packets.isEmpty()) {
            return;
        }
        List<Packet> toRemove = packets.stream().filter(test)
                .collect(Collectors.toList());
        if (toRemove.isEmpty()) {
            return;
        }
        Ax.err("Removing predictive packets... (%s hits)",
                predictivePacketsHit);
        packets.removeIf(test);
        for (Packet packet : toRemove) {
            removeFromLookups(packet);
        }
    }

    synchronized Stream<Packet> streamByName(String name) {
        return packets.stream()
                .filter(p -> Objects.equals(p.messageName, name));
    }

    PacketPair toPacketPair(Packet replyPacket) {
        return new PacketPair(byIdCommand.get(replyPacket.id()), replyPacket);
    }
}