package cc.alcina.framework.classmeta.rdb;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JdwpPackets {
    public List<JdwpPacket> packets = new ArrayList<>();
    
    transient Map<Integer,JdwpPacket> sentById = new LinkedHashMap<>();

    public Optional<JdwpPacket> find(int commandSet, int commandId) {
        Optional<JdwpPacket> findFirst = packets.stream()
                .filter(p -> p.commandSet() == commandSet
                        && p.commandId() == commandId && p.fromDebugger)
                .findFirst();
        return findFirst;
    }

    public JdwpPacket findReply(int id) {
        return sentById.get(id);
    }

    public void add(JdwpPacket packet) {
        packets.add(packet);
        if(packet.commandSet()!=0){
            sentById.put(packet.id(), packet);
        }
    }
}