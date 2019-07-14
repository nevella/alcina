package com.sun.tools.jdi;

import com.sun.tools.jdi.JDWP.ThreadReference.ThreadGroup;
import com.sun.tools.jdi.JDWP.VirtualMachine.AllThreads;

import cc.alcina.framework.common.client.WrappedRuntimeException;

public class RdbJdi {
    private VirtualMachineImplExt vm;

    public RdbJdi(VirtualMachineImplExt vm) {
        this.vm = vm;
    }

    public void predict_all_threads_handshake(byte[] command, byte[] reply)
            throws Exception {
        PredictorToken token = new PredictorToken(command, reply);
        AllThreads allThreads = AllThreads.waitForReply(vm,
                token.replyStream());
        for (ThreadReferenceImpl thread : allThreads.threads) {
            ThreadGroup threadGroup = ThreadGroup.process(vm, thread);
        }
        int debug = 3;
    }

    class PredictorToken {
        Packet command;

        Packet reply;

        public PredictorToken(byte[] command, byte[] reply) {
            try {
                this.command = Packet.fromByteArray(command);
                this.reply = Packet.fromByteArray(reply);
                this.reply.replied = true;
            } catch (Exception e) {
                throw new WrappedRuntimeException(e);
            }
        }

        PacketStream replyStream() {
            return new PacketStream(vm, reply);
        }
    }
}
