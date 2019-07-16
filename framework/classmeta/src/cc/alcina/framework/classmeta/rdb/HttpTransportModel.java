package cc.alcina.framework.classmeta.rdb;

import java.util.ArrayList;
import java.util.List;

class HttpTransportModel {
    List<Packet> passthroughPackets = new ArrayList<>();

    List<Packet> predictivePackets = new ArrayList<>();

    boolean eventListener;

    String endpointName;

    public boolean close;
}