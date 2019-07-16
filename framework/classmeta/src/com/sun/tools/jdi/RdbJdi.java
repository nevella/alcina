package com.sun.tools.jdi;

import com.sun.jdi.Field;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.Value;
import com.sun.tools.jdi.JDWP.ThreadGroupReference;
import com.sun.tools.jdi.JDWP.ThreadGroupReference.Parent;
import com.sun.tools.jdi.JDWP.ThreadReference.Frames;
import com.sun.tools.jdi.JDWP.ThreadReference.ThreadGroup;
import com.sun.tools.jdi.JDWP.VirtualMachine.AllThreads;
import com.sun.tools.jdi.JDWP.VirtualMachine.CapabilitiesNew;

import cc.alcina.framework.common.client.WrappedRuntimeException;

public class RdbJdi {
    /**
     * String constant for the signature of the primitive type boolean. Value is
     * <code>"Z"</code>.
     */
    static final String SIG_BOOLEAN = "Z"; //$NON-NLS-1$

    /**
     * String constant for the signature of the primitive type byte. Value is
     * <code>"B"</code>.
     */
    static final String SIG_BYTE = "B"; //$NON-NLS-1$

    /**
     * String constant for the signature of the primitive type char. Value is
     * <code>"C"</code>.
     */
    static final String SIG_CHAR = "C"; //$NON-NLS-1$

    /**
     * String constant for the signature of the primitive type double. Value is
     * <code>"D"</code>.
     */
    static final String SIG_DOUBLE = "D"; //$NON-NLS-1$

    /**
     * String constant for the signature of the primitive type float. Value is
     * <code>"F"</code>.
     */
    static final String SIG_FLOAT = "F"; //$NON-NLS-1$

    /**
     * String constant for the signature of the primitive type int. Value is
     * <code>"I"</code>.
     */
    static final String SIG_INT = "I"; //$NON-NLS-1$

    /**
     * String constant for the signature of the primitive type long. Value is
     * <code>"J"</code>.
     */
    static final String SIG_LONG = "J"; //$NON-NLS-1$

    /**
     * String constant for the signature of the primitive type short. Value is
     * <code>"S"</code>.
     */
    static final String SIG_SHORT = "S"; //$NON-NLS-1$

    /**
     * String constant for the signature of result type void. Value is
     * <code>"V"</code>.
     */
    static final String SIG_VOID = "V"; //$NON-NLS-1$

    private VirtualMachineImplExt vm;

    public RdbJdi(VirtualMachineImplExt vm) {
        this.vm = vm;
    }

    public byte[] getThreadStatusCommandBytes(byte[] bytes) {
        // TODO Auto-generated method stub
        return null;
    }

    public void predict_all_threads_handshake(byte[] command, byte[] reply)
            throws Exception {
        PredictorToken token = new PredictorToken(command, reply);
        AllThreads allThreads = AllThreads.waitForReply(vm,
                token.replyStream());
        for (ThreadReferenceImpl thread : allThreads.threads) {
            ThreadGroup threadGroup = ThreadGroup.process(vm, thread);
            ThreadGroupReferenceImpl threadGroupRef = threadGroup.group;
            while (threadGroupRef != null) {
                ThreadGroupReference.Name.process(vm, threadGroupRef);
                Parent process = ThreadGroupReference.Parent.process(vm,
                        threadGroupRef);
                threadGroupRef = process.parentGroup;
            }
            mockDetermineIfDaemonThread(thread);
            thread.status();
            /*
             * More than eclipse does - but we want to cache
             */
            thread.name();
            if (thread.isSuspended()) {
                thread.currentContendedMonitor();
            }
        }
    }

    public void predict_frames(byte[] command, byte[] reply) throws Exception {
        PredictorToken token = new PredictorToken(command, reply);
        Frames frames = Frames.waitForReply(vm, token.replyStream());
    }

    public void tmpParse(byte[] bytes) {
        try {
            PredictorToken token = new PredictorToken(null, bytes);
            CapabilitiesNew o = CapabilitiesNew.waitForReply(vm,
                    token.replyStream());
            int debug = 3;
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    private void mockDetermineIfDaemonThread(ThreadReferenceImpl thread) {
        ReferenceType referenceType = thread.referenceType();
        Field field = referenceType.fieldByName("daemon"); //$NON-NLS-1$
        if (field == null) {
            field = referenceType.fieldByName("isDaemon"); //$NON-NLS-1$
        }
        if (field != null) {
            if (field.signature().equals(SIG_BOOLEAN)) {
                Value value = thread.getValue(field);
            }
        }
    }

    class PredictorToken {
        Packet command;

        Packet reply;

        public PredictorToken(byte[] command, byte[] reply) {
            try {
                this.command = command == null ? null
                        : Packet.fromByteArray(command);
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
