package com.sun.tools.jdi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.sun.jdi.Field;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.Value;
import com.sun.tools.jdi.JDWP.ThreadGroupReference;
import com.sun.tools.jdi.JDWP.ThreadGroupReference.Parent;
import com.sun.tools.jdi.JDWP.ThreadReference.Frames;
import com.sun.tools.jdi.JDWP.ThreadReference.Frames.Frame;
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

    Map<RefTypeMethodKey, List<StackFrameImpl>> stackFrameByTypeMethod = new LinkedHashMap<>();

    public RdbJdi(VirtualMachineImplExt vm) {
        this.vm = vm;
    }

    public void debugThisObject(byte[] command) {
        PredictorToken token = new PredictorToken(command, null);
        PacketStream commandStream = token.commandStream();
        ThreadReferenceImpl threadRef = commandStream.readThreadReference();
        long frameRef = commandStream.readFrameRef();
        debug("%s :: %s\n", threadRef.ref(), frameRef);
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
        for (Frame frame : frames.frames) {
            Method method = frame.location.method();
            ReferenceType declaringType = frame.location.declaringType();
            forceSignature(declaringType);
            declaringType.signature();
            try {
                declaringType.sourceDebugExtension();
            } catch (Exception e) {
                // squelch
            }
            try {
                declaringType.sourcePaths(null);
            } catch (Exception e) {
                // squelch
            }
        }
        ThreadReferenceImpl threadRef = token.commandStream()
                .readThreadReference();
        int frameCount = threadRef.frameCount();
        for (StackFrame frame : threadRef.frames()) {
            /*
             * force call because eclipse does (even if static/native)
             */
            JDWP.StackFrame.ThisObject.process(vm, threadRef, frameId(frame));
            ObjectReference thisObject = frame.thisObject();
            debug("+++thisObject: %s - %s\n", threadRef.ref, frameId(frame));
            if (thisObject != null) {
                ReferenceType referenceType = thisObject.referenceType();
                forceSignature(referenceType);
                try {
                    referenceType.sourceDebugExtension();
                } catch (Exception e) {
                    // squelch
                }
                try {
                    referenceType.sourcePaths(null);
                } catch (Exception e) {
                    // squelch
                }
            }
            MethodImpl method = (MethodImpl) frame.location().method();
            if (method instanceof ConcreteMethodImpl) {
                JDWP.Method.LineTable.process(vm, method.declaringType,
                        method.ref);
            }
            method.location();
            RefTypeMethodKey key = new RefTypeMethodKey(
                    ((ReferenceTypeImpl) method.declaringType()).ref,
                    ((MethodImpl) method).ref);
            stackFrameByTypeMethod.computeIfAbsent(key, k -> new ArrayList<>())
                    .add((StackFrameImpl) frame);
        }
    }

    public void predict_variable_table(byte[] command, byte[] reply)
            throws Exception {
        PredictorToken token = new PredictorToken(command, reply);
        PacketStream commandStream = token.commandStream();
        long typeRef = commandStream.readObjectRef();
        long methodRef = commandStream.readMethodRef();
        RefTypeMethodKey key = new RefTypeMethodKey(typeRef, methodRef);
        List<StackFrameImpl> zeen = stackFrameByTypeMethod.computeIfAbsent(key,
                k -> new ArrayList<>());
        if (zeen.size() > 0) {
            StackFrameImpl guessingFrame = zeen.get(zeen.size() - 1);
            guessingFrame.visibleVariables().forEach(guessingFrame::getValue);
        }
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

    private void debug(String template, Object... args) {
        // System.out.format(template, args);
    }

    private void forceSignature(ReferenceType declaringType) {
        try {
            JDWP.ReferenceType.Signature.process(vm,
                    (ReferenceTypeImpl) declaringType);
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    private long frameId(StackFrame frame) {
        try {
            java.lang.reflect.Field field = StackFrameImpl.class
                    .getDeclaredField("id");
            field.setAccessible(true);
            return (long) field.get(frame);
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

    public static class RefTypeMethodKey {
        private long ref;

        private long readMethodRef;

        public RefTypeMethodKey(long ref, long readMethodRef) {
            this.ref = ref;
            this.readMethodRef = readMethodRef;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof RefTypeMethodKey) {
                RefTypeMethodKey o = (RefTypeMethodKey) obj;
                return Objects.equals(ref, o.ref)
                        && Objects.equals(readMethodRef, o.readMethodRef);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(ref, readMethodRef);
        }
    }

    class PredictorToken {
        Packet command;

        Packet reply;

        public PredictorToken(byte[] command, byte[] reply) {
            try {
                this.command = command == null ? null
                        : Packet.fromByteArray(command);
                if (reply != null) {
                    this.reply = Packet.fromByteArray(reply);
                    this.reply.replied = true;
                }
            } catch (Exception e) {
                throw new WrappedRuntimeException(e);
            }
        }

        PacketStream commandStream() {
            return new PacketStream(vm, command);
        }

        PacketStream replyStream() {
            return new PacketStream(vm, reply);
        }
    }
}
