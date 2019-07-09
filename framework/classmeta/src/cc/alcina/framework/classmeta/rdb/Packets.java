package cc.alcina.framework.classmeta.rdb;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

 class Packets {
    public List<Packet> packets = new ArrayList<>();
    
    transient Map<Integer,Packet> sentById = new LinkedHashMap<>();

    public Optional<Packet> find(int commandSet, int commandId) {
        Optional<Packet> findFirst = packets.stream()
                .filter(p -> p.commandSet() == commandSet
                        && p.commandId() == commandId && p.fromDebugger)
                .findFirst();
        return findFirst;
    }

    public Packet findReply(int id) {
        return sentById.get(id);
    }

    public void add(Packet packet) {
        packets.add(packet);
        if(packet.commandSet()!=0){
            sentById.put(packet.id(), packet);
        }
    }
}