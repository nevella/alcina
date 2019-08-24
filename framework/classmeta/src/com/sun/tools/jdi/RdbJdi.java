package com.sun.tools.jdi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.sun.jdi.Field;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.Type;
import com.sun.jdi.Value;
import com.sun.tools.jdi.JDWP.ClassType.Superclass;
import com.sun.tools.jdi.JDWP.Event.Composite;
import com.sun.tools.jdi.JDWP.Event.Composite.Events;
import com.sun.tools.jdi.JDWP.Event.Composite.Events.Breakpoint;
import com.sun.tools.jdi.JDWP.Event.Composite.Events.EventsCommon;
import com.sun.tools.jdi.JDWP.Event.Composite.Events.SingleStep;
import com.sun.tools.jdi.JDWP.Event.Composite.Events.ThreadStart;
import com.sun.tools.jdi.JDWP.ReferenceType.Interfaces;
import com.sun.tools.jdi.JDWP.ReferenceType.Signature;
import com.sun.tools.jdi.JDWP.ThreadGroupReference;
import com.sun.tools.jdi.JDWP.ThreadGroupReference.Parent;
import com.sun.tools.jdi.JDWP.ThreadReference.Frames;
import com.sun.tools.jdi.JDWP.ThreadReference.Frames.Frame;
import com.sun.tools.jdi.JDWP.ThreadReference.ThreadGroup;
import com.sun.tools.jdi.JDWP.VirtualMachine.AllThreads;
import com.sun.tools.jdi.JDWP.VirtualMachine.CapabilitiesNew;
import com.sun.tools.jdi.JDWP.VirtualMachine.ClassesBySignature;
import com.sun.tools.jdi.JDWP.VirtualMachine.ClassesBySignature.ClassInfo;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;

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

    Set<ReferenceType> referenceTypeMetadataCalled = new LinkedHashSet<>();

    Map<ReferenceType, List<Field>> allFields = Collections
            .synchronizedMap(new LinkedHashMap<>());

    Map<Long, Integer> earlyBreakpointResumes = Collections
            .synchronizedMap(new LinkedHashMap<>());

    public RdbJdi(VirtualMachineImplExt vm) {
        this.vm = vm;
    }

    public void debug_is_collected(byte[] bytes) {
        try {
            PredictorToken token = new PredictorToken(bytes, null);
            long objectRefId = token.commandStream().readObjectRef();
            int debug = 3;
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    public void debugThisObject(byte[] command) {
        PredictorToken token = new PredictorToken(command, null);
        PacketStream commandStream = token.commandStream();
        ThreadReferenceImpl threadRef = commandStream.readThreadReference();
        long frameRef = commandStream.readFrameRef();
        debug("%s :: %s\n", threadRef.ref(), frameRef);
    }

    public boolean handle_composite(byte[] bytes) {
        try {
            boolean notifySuspended = false;
            PredictorToken token = new PredictorToken(bytes, null);
            Composite composite = new Composite(vm, token.commandStream());
            Ax.out("Composite: suspend: " + composite.suspendPolicy);
            for (Events events : composite.events) {
                Ax.out(events.aEventsCommon);
                EventsCommon common = events.aEventsCommon;
                if (common instanceof ThreadStart) {
                    ThreadReferenceImpl thread = ((ThreadStart) common).thread;
                    Ax.out("Thread start: ref %s", thread.ref);
                    predictThreadMetadataCalls(thread);
                }
                if (common instanceof Breakpoint) {
                    Breakpoint breakpoint = (Breakpoint) common;
                    // predictThreadMetadataCalls(breakpoint.thread);
                    Ax.out("Breakpoint: ref %s", breakpoint.thread.ref);
                    if (breakpoint.location.declaringType().name()
                            .equals("java.lang.Thread")) {
                        // instantly resume - this was set by Eclipse to monitor
                        // name changes
                        // Resume.enqueueCommand(vm, breakpoint.thread);
                        // earlyBreakpointResumes.merge(breakpoint.thread.ref,
                        // 1,
                        // (k, v) -> v + 1);
                        int debug = 3;
                    } else {
                        Ax.out("Breakpoint: ref %s (real breakpoint)",
                                breakpoint.thread.ref);
                        notifySuspended = true;
                    }
                }
                if (common instanceof SingleStep) {
                    notifySuspended = true;
                }
                if (common instanceof com.sun.tools.jdi.JDWP.Event.Composite.Events.Exception) {
                    notifySuspended = true;
                }
            }
            Ax.out("--------\n");
            return notifySuspended;
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    public void predict_all_threads_handshake(byte[] command, byte[] reply)
            throws Exception {
        PredictorToken token = new PredictorToken(command, reply);
        AllThreads allThreads = AllThreads.waitForReply(vm,
                token.replyStream());
        for (ThreadReferenceImpl thread : allThreads.threads) {
            predictThreadMetadataCalls(thread);
        }
    }

    public void predict_classes_by_signature(byte[] command, byte[] reply)
            throws Exception {
        PredictorToken token = new PredictorToken(command, reply);
        ClassesBySignature classesBySignature = ClassesBySignature
                .waitForReply(vm, token.replyStream());
        for (ClassInfo classInfo : classesBySignature.classes) {
            ReferenceTypeImpl referenceTypeImpl = vm
                    .referenceType(classInfo.typeID, classInfo.refTypeTag);
            getClassMetadata(referenceTypeImpl);
            for (Method method : referenceTypeImpl.allMethods()) {
                getMethodData((MethodImpl) method);
            }
        }
    }

    public void predict_frames(byte[] command, byte[] reply) throws Exception {
        PredictorToken token = new PredictorToken(command, reply);
        Frames frames = null;
        try {
            frames = Frames.waitForReply(vm, token.replyStream());
        } catch (JDWPException e) {
            if (e.errorCode() == 13) {// thread not suspended{
                return;
            }
        }
        for (Frame frame : frames.frames) {
            Method method = frame.location.method();
            ReferenceType declaringType = frame.location.declaringType();
            forceSignature(declaringType);
            getClassMetadata(declaringType);
        }
        ThreadReferenceImpl threadRef = token.commandStream()
                .readThreadReference();
        /*
         * Reset manually (for normal jdwp usage resetting would be done by
         * threadRef.resume())
         */
        java.lang.reflect.Method resetLocalCache = threadRef.getClass()
                .getDeclaredMethod("resetLocalCache");
        resetLocalCache.setAccessible(true);
        resetLocalCache.invoke(threadRef);
        int frameCount = threadRef.frameCount();
        for (StackFrame frame : threadRef.frames()) {
            /*
             * force call because eclipse does (even if static/native)
             */
            try {
                JDWP.StackFrame.ThisObject.process(vm, threadRef,
                        frameId(frame));
                ObjectReference thisObject = frame.thisObject();
                debug("+++thisObject: %s - %s\n", threadRef.ref,
                        frameId(frame));
                if (thisObject != null) {
                    ReferenceType referenceType = thisObject.referenceType();
                    getClassMetadata(referenceType);
                }
                MethodImpl method = (MethodImpl) frame.location().method();
                getMethodData(method);
                RefTypeMethodKey key = new RefTypeMethodKey(
                        ((ReferenceTypeImpl) method.declaringType()).ref,
                        ((MethodImpl) method).ref);
                stackFrameByTypeMethod
                        .computeIfAbsent(key, k -> new ArrayList<>())
                        .add((StackFrameImpl) frame);
            } catch (JDWPException e) {
                e.printStackTrace();
            }
        }
    }

    public void predict_get_values_array_reference(byte[] command, byte[] reply)
            throws Exception {
        PredictorToken token = new PredictorToken(command, reply);
        PacketStream commandStream = token.commandStream();
        try {
            ArrayReferenceImpl arrayRef = (ArrayReferenceImpl) commandStream
                    .readObjectReference();
            int firstIndex = commandStream.readInt();
            int length = commandStream.readInt();
            if (length == 1) {
                for (int idx = firstIndex; idx < arrayRef.length()
                        && idx < firstIndex + 100; idx++) {
                    Value value = arrayRef.getValue(idx);
                    if (value instanceof ObjectReferenceImpl) {
                        getClassMetadata(
                                ((ObjectReferenceImpl) value).referenceType());
                    }
                    if (value instanceof ArrayReferenceImpl) {
                        ((ArrayReferenceImpl) value).length();
                    }
                }
            }
        } catch (Exception e) {
            // TODO ...hmmmm arrayref classcast?
            e.printStackTrace();
        }
        // ThreadReferenceImpl threadRef = commandStream.readThreadReference();
        // long frameRef = commandStream.readFrameRef();
        // for (StackFrame frame : threadRef.frames()) {
        // if (frameId(frame) == frameRef) {
        // predictStackFrame((StackFrameImpl) frame);
        // }
        // }
    }

    public void predict_get_values_object_reference(byte[] command,
            byte[] reply) throws Exception {
        Ax.err("predict_get_values_object_reference");
        PredictorToken token = new PredictorToken(command, reply);
        PacketStream commandStream = token.commandStream();
        ObjectReferenceImpl objectRef = commandStream.readObjectReference();
        getClassMetadata(objectRef.referenceType());
        int fieldCount = commandStream.readInt();
        List<Field> allFields = objectRef.referenceType().allFields();
        /*
         * cache 'em all
         */
        for (Field field : allFields) {
            Value value = objectRef.getValue(field);
            if (value instanceof ObjectReferenceImpl) {
                getClassMetadata(((ObjectReferenceImpl) value).referenceType());
            }
            if (value instanceof ArrayReferenceImpl) {
                ((ArrayReferenceImpl) value).length();
            }
        }
        // ThreadReferenceImpl threadRef = commandStream.readThreadReference();
        // long frameRef = commandStream.readFrameRef();
        // for (StackFrame frame : threadRef.frames()) {
        // if (frameId(frame) == frameRef) {
        // predictStackFrame((StackFrameImpl) frame);
        // }
        // }
    }

    public void predict_get_values_reference_type(byte[] command, byte[] reply)
            throws Exception {
        Ax.err("predict_get_values_reference_type");
        PredictorToken token = new PredictorToken(command, reply);
        PacketStream commandStream = token.commandStream();
        ClassObjectReferenceImpl classObject = commandStream
                .readClassObjectReference();
        getClassMetadata(classObject.referenceType());
        Ax.out("Class object: id:%s name:%s", classObject.ref, classObject);
        int fieldCount = commandStream.readInt();
        Ax.out("Field count: %s", fieldCount);
        ReferenceTypeImpl referenceType = (ReferenceTypeImpl) classObject
                .reflectedType();
        for (int idx = 0; idx < fieldCount; idx++) {
            long fieldRef = commandStream.readFieldRef();
            Field field = referenceType.getFieldMirror(fieldRef);
            Ax.out("Field id: %s", fieldRef);
        }
        List<Field> allFields = allFields(referenceType);
        /*
         * cache 'em all
         */
        for (Field field : allFields) {
            if (!field.isStatic()) {
                continue;
            }
            Value value = referenceType.getValue(field);
            if (value instanceof ObjectReferenceImpl) {
                getClassMetadata(((ObjectReferenceImpl) value).referenceType());
            }
        }
        // ThreadReferenceImpl threadRef = commandStream.readThreadReference();
        // long frameRef = commandStream.readFrameRef();
        // for (StackFrame frame : threadRef.frames()) {
        // if (frameId(frame) == frameRef) {
        // predictStackFrame((StackFrameImpl) frame);
        // }
        // }
    }

    public void predict_get_values_stack_frame(byte[] command, byte[] reply)
            throws Exception {
        PredictorToken token = new PredictorToken(command, reply);
        PacketStream commandStream = token.commandStream();
        ThreadReferenceImpl threadRef = commandStream.readThreadReference();
        long frameRef = commandStream.readFrameRef();
        for (StackFrame frame : threadRef.frames()) {
            if (frameId(frame) == frameRef) {
                predictStackFrame((StackFrameImpl) frame);
            }
        }
    }

    public void predict_reference_type(byte[] command, byte[] reply)
            throws Exception {
        PredictorToken token = new PredictorToken(command, reply);
        com.sun.tools.jdi.JDWP.ObjectReference.ReferenceType rt = com.sun.tools.jdi.JDWP.ObjectReference.ReferenceType
                .waitForReply(vm, token.replyStream());
        try {
            ReferenceTypeImpl referenceTypeImpl = vm.referenceType(rt.typeID,
                    rt.refTypeTag);
            getClassMetadata(referenceTypeImpl);
            for (Method method : referenceTypeImpl.allMethods()) {
                getMethodData((MethodImpl) method);
            }
        } catch (Exception e) {
            e.printStackTrace();
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
            try {
                predictStackFrame(guessingFrame);
            } catch (Exception e) {
                // invalidate, retry, notify exception if recurs
                // this exception is reasonable - our frame prediction is a
                // little wooly
                stackFrameByTypeMethod.clear();
                zeen = stackFrameByTypeMethod.computeIfAbsent(key,
                        k -> new ArrayList<>());
                if (zeen.size() > 0) {
                    try {
                        guessingFrame = zeen.get(zeen.size() - 1);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
    }

    public boolean send_resume_command(byte[] bytes) {
        try {
            PredictorToken token = new PredictorToken(bytes, null);
            long thread = token.commandStream().readThreadReference().ref;
            int sentCount = earlyBreakpointResumes.getOrDefault(thread, 0);
            earlyBreakpointResumes.put(thread,
                    sentCount <= 0 ? 0 : sentCount - 1);
            if (sentCount != 0) {
                int debug = 3;
            }
            return sentCount == 0;
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
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

    private List<Field> allFields(ReferenceTypeImpl type) {
        return allFields.computeIfAbsent(type, t -> t.allFields());
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

    private void getClassMetadata(ReferenceType type) {
        if (!referenceTypeMetadataCalled.add(type)) {
            return;
        }
        type.signature();
        type.modifiers();
        type.allFields();
        type.allMethods();
        try {
            type.sourceDebugExtension();
        } catch (Exception e) {
            // squelch
        }
        try {
            type.sourcePaths(null);
        } catch (Exception e) {
            // squelch
        }
        ReferenceTypeImpl impl = (ReferenceTypeImpl) type;
        impl.getInterfaces().forEach(this::getClassMetadata);
        impl.inheritedTypes().forEach(this::getClassMetadata);
        try {
            /*
             * force call because eclipse does
             */
            Signature.process(vm, impl);
            if (impl instanceof ClassTypeImpl) {
                Superclass.process(vm, (ClassTypeImpl) impl);
            }
            Interfaces.process(vm, impl);
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    private Field getFieldMirror(ReferenceTypeImpl referenceType, long ref) {
        // Fetch all fields for the class, check performance impact
        // Needs no synchronization now, since fields() returns
        // unmodifiable local data
        Iterator<Field> it = allFields(referenceType).iterator();
        while (it.hasNext()) {
            FieldImpl field = (FieldImpl) it.next();
            if (field.ref() == ref) {
                return field;
            }
        }
        throw new IllegalArgumentException("Invalid field id: " + ref);
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

    private void predictStackFrame(StackFrameImpl frame) throws Exception {
        List<LocalVariable> visibleVariables = frame.visibleVariables();
        for (LocalVariable localVariable : visibleVariables) {
            Value value = frame.getValue(localVariable);
            if (value != null) {
                Type type = value.type();
                if (type instanceof ReferenceTypeImpl) {
                    ReferenceTypeImpl refType = (ReferenceTypeImpl) type;
                    getClassMetadata(refType);
                }
            }
        }
    }

    private void predictThreadMetadataCalls(ThreadReferenceImpl thread) {
        try {
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
        } catch (Exception e) {
            Ax.out("predictThreadMetadataCalls-%s:: %s",
                    e.getClass().getSimpleName(), thread);
        }
    }

    protected void getMethodData(MethodImpl method) throws JDWPException {
        if (method instanceof ConcreteMethodImpl) {
            JDWP.Method.LineTable.process(vm, method.declaringType, method.ref);
        }
        method.location();
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
