package cc.alcina.framework.classmeta.rdb;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RdbPackets {
    public List<RdbPacket> packets = new ArrayList<>();

    public Optional<RdbPacket> find(int commandSet, int commandId) {
        Optional<RdbPacket> findFirst = packets.stream()
                .filter(p -> p.commandSet() == commandSet
                        && p.commandId() == commandId && p.fromDebugger)
                .findFirst();
        return findFirst;
    }

    public RdbPacket findReply(int id) {
        Optional<RdbPacket> findFirst = packets.stream()
                .filter(p -> p.id() == id && !p.fromDebugger).findFirst();
        if (!findFirst.isPresent()) {
            int debug = 3;
        }
        return findFirst.get();
    }
}