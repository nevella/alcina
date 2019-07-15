package cc.alcina.framework.classmeta.rdb;

import cc.alcina.framework.classmeta.rdb.Packet.EventSeries;

class DebuggerState {
    boolean calledAllThreads = false;

    boolean expectingPredictive = false;

    EventSeries currentSeries = EventSeries.early_handshake;

    public DebuggerState() {
        updateState();
    }

    void setExpectingPredictive(boolean expectingPredictive) {
        this.expectingPredictive = expectingPredictive;
        updateState();
    }

    void updateState() {
        switch (currentSeries) {
        case early_handshake:
            if (calledAllThreads) {
                currentSeries = EventSeries.all_threads_handshake;
            }
            break;
        case all_threads_handshake: {
            if (!expectingPredictive) {
                currentSeries = EventSeries.unknown_post_handshake;
            }
            break;
        }
        }
    }
}
