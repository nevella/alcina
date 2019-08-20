package cc.alcina.framework.classmeta.rdb;

import java.util.Arrays;

import cc.alcina.framework.classmeta.rdb.Packet.EventSeries;
import cc.alcina.framework.classmeta.rdb.Packet.HandshakePacket;

class DebuggerState {
    boolean calledAllThreads = false;

    Packet expectingPredictiveAfterPacket = null;

    EventSeries currentSeries = EventSeries.early_handshake;

    public Packet currentPacket;

    boolean seenSuspend;

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
        CommandSet commandSet = currentPacket == null
                || currentPacket instanceof HandshakePacket ? null
                        : CommandSet.byId(currentPacket.commandSet());
        switch (name) {
        case "Composite":
        case "IsCollected":
            // doesn't change state
            return;
        }
        switch (name) {
        case "Suspend":
            seenSuspend = true;
            break;
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
                switch (name) {
                case "CapabilitiesNew":
                case "IsCollected":
                    break;
                default:
                    if (!expectingPredictive) {
                        next = EventSeries.admin_post_handshake;
                    }
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
                case "GetValues":
                    switch (commandSet) {
                    case StackFrame:
                        next = EventSeries.get_values_stack_frame;
                        break;
                    case ReferenceType:
                        next = EventSeries.get_values_reference_type;
                        break;
                    case ObjectReference:
                        next = EventSeries.get_values_object_reference;
                        break;
                    case ArrayReference:
                        next = EventSeries.get_values_array_reference;
                        break;
                    default:
                        next = EventSeries.unknown_post_handshake;
                        break;
                    }
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
                    next = EventSeries.unknown_post_handshake;
                    break;
                }
                break;
            }
            case get_values_stack_frame: {
                switch (name) {
                case "GetValues":
                    switch (commandSet) {
                    case StackFrame:
                        break;
                    default:
                        next = EventSeries.unknown_post_handshake;
                        break;
                    }
                    break;
                default:
                    next = EventSeries.unknown_post_handshake;
                    break;
                }
                break;
            }
            case get_values_array_reference: {
                switch (name) {
                case "GetValues":
                    switch (commandSet) {
                    case ArrayReference:
                        break;
                    default:
                        next = EventSeries.unknown_post_handshake;
                        break;
                    }
                    break;
                default:
                    next = EventSeries.unknown_post_handshake;
                    break;
                }
                break;
            }
            case get_values_object_reference: {
                switch (name) {
                case "GetValues":
                    switch (commandSet) {
                    case ObjectReference:
                        break;
                    default:
                        next = EventSeries.unknown_post_handshake;
                        break;
                    }
                    break;
                default:
                    next = EventSeries.unknown_post_handshake;
                    break;
                }
                break;
            }
            case get_values_reference_type: {
                switch (name) {
                case "GetValues":
                    switch (commandSet) {
                    case ReferenceType:
                        break;
                    default:
                        next = EventSeries.unknown_post_handshake;
                        break;
                    }
                    break;
                default:
                    next = EventSeries.unknown_post_handshake;
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

    enum CommandSet {
        VirtualMachine(1), ReferenceType(2), ClassType(3), ArrayType(4),
        InterfaceType(5), Method(6), Field(8), ObjectReference(9),
        StringReference(10), ThreadReference(11), ThreadGroupReference(12),
        ArrayReference(13), ClassLoaderReference(14), EventRequest(15),
        StackFrame(16), ClassObjectReference(17), Event(64);
        static CommandSet byId(int id) {
            return Arrays.stream(values()).filter(v -> v.id == id).findAny()
                    .get();
        }

        private int id;

        CommandSet(int id) {
            this.id = id;
        }
    }
}
