package cc.alcina.framework.classmeta.rdb;

import cc.alcina.framework.classmeta.rdb.Packet.EventSeries;

class DebuggerState {
    boolean calledAllThreads = false;

    Packet expectingPredictiveAfterPacket = null;

    EventSeries currentSeries = EventSeries.early_handshake;

    public Packet currentPacket;

    public DebuggerState() {
        updateState();
    }

    void setExpectingPredictiveAfter(Packet packet) {
        if (packet == null) {
            if (currentPacket == expectingPredictiveAfterPacket) {
                packet = expectingPredictiveAfterPacket;
            }
        }
        expectingPredictiveAfterPacket = packet;
    }

    void updateState() {
        String name = currentPacket == null ? "" : currentPacket.messageName;
        switch (name) {
        case "Composite":
        case "IsCollected":
            // doesn't change state
            return;
        }
        boolean expectingPredictive = expectingPredictiveAfterPacket != null;
        boolean hadDelta = false;
        EventSeries next = null;
        while (next != currentSeries) {
            next = currentSeries;
            switch (currentSeries) {
            case early_handshake:
                switch (name) {
                case "AllThreads": {
                    calledAllThreads = true;
                    setExpectingPredictiveAfter(currentPacket);
                    break;
                }
                }
                if (calledAllThreads) {
                    next = EventSeries.all_threads_handshake;
                }
                break;
            case all_threads_handshake: {
                if (!expectingPredictive) {
                    next = EventSeries.admin_post_handshake;
                }
                break;
            }
            case unknown_post_handshake:
            case admin_post_handshake: {
                switch (name) {
                case "CapabilitiesNew":
                case "IsCollected":
                    next = EventSeries.admin_post_handshake;
                    break;
                case "Set":
                    next = EventSeries.breakpoint_set;
                    break;
                case "CurrentContendedMonitor":
                    next = EventSeries.contended_monitor_check;
                    break;
                case "Suspend":
                    next = EventSeries.suspend;
                    break;
                case "Frames":
                    next = EventSeries.frames;
                    break;
                case "VariableTableWithGeneric":
                    next = EventSeries.variable_table;
                    break;
                default:
                    next = EventSeries.unknown_post_handshake;
                    break;
                }
                break;
            }
            case breakpoint_set: {
                switch (name) {
                case "Set":
                case "ClassesBySignature":
                case "Signature":
                    break;
                default:
                    next = EventSeries.unknown_post_handshake;
                    break;
                }
                break;
            }
            case contended_monitor_check: {
                switch (name) {
                case "CurrentContendedMonitor":
                    break;
                default:
                    next = EventSeries.unknown_post_handshake;
                    break;
                }
                break;
            }
            case suspend: {
                switch (name) {
                case "Suspend":
                case "Status":
                    int debug = 3;
                    break;
                default:
                    next = EventSeries.unknown_post_handshake;
                    break;
                }
                break;
            }
            case frames: {
                switch (name) {
                case "Frames":
                case "FrameCount":
                    break;
                default:
                    next = EventSeries.unknown_post_handshake;
                    break;
                }
                break;
            }
            case variable_table: {
                switch (name) {
                case "VariableTableWithGeneric":
                    break;
                default:
                    // next = EventSeries.unknown_post_handshake;
                    int debug = 3;
                    break;
                }
                break;
            }
            default:
                throw new UnsupportedOperationException();
            }
            if (next != currentSeries) {
                hadDelta = true;
                currentSeries = next;
            }
        }
        if (currentPacket != null && currentPacket.meta != null && hadDelta) {
            currentPacket.meta.series = currentSeries;
        }
    }
}
