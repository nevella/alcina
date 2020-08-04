package com.gdevelop.gwt.syncrpc;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.gdevelop.gwt.syncrpc.SyncClientSerializationStreamReader.BoundedList;
import com.gdevelop.gwt.syncrpc.SyncClientSerializationStreamReader.ValueReader;
import com.gdevelop.gwt.syncrpc.SyncClientSerializationStreamReader.VectorReader;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.impl.Serializer;
import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.google.gwt.user.server.rpc.impl.SerializabilityUtil;
import com.google.gwt.user.server.rpc.impl.SerializedInstanceReference;

class SyncProxySerializer implements Serializer {
    private SerializationPolicy serializationPolicy;

    public SyncProxySerializer(SerializationPolicy serializationPolicy) {
        this.serializationPolicy = serializationPolicy;
    }

    @Override
    public void deserialize(SerializationStreamReader streamReader, Object instance, String typeSignature)
            throws SerializationException {
        Class<?> customSerializer = SerializabilityUtil.hasCustomFieldSerializer(instance.getClass());
        try {
            deserializeImpl(streamReader, customSerializer, instance.getClass(), instance);
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    private Object deserializeImpl(SerializationStreamReader streamReader, Class<?> customSerializer,
            Class<?> instanceClass, Object instance) throws Exception {
        if (customSerializer != null) {
            deserializeWithCustomFieldDeserializer(streamReader, customSerializer, instanceClass, instance);
        } else if (instanceClass.isArray()) {
            instance = deserializeArray(streamReader, instanceClass, instance);
        } else if (instanceClass.isEnum()) {
            // Enums are deserialized when they are instantiated
        } else {
            deserializeClass(streamReader, instanceClass, instance);
        }
        return instance;
    }

    private Object deserializeArray(SerializationStreamReader streamReader, Class<?> instanceClass, Object instance)
            throws SerializationException {
        assert (instanceClass.isArray());
        BoundedList<Object> buffer = (BoundedList<Object>) instance;
        VectorReader instanceReader = SyncClientSerializationStreamReader.CLASS_TO_VECTOR_READER.get(instanceClass);
        if (instanceReader != null) {
            return instanceReader.read(streamReader, buffer);
        } else {
            return SyncClientSerializationStreamReader.VectorReader.OBJECT_VECTOR.read(streamReader, buffer);
        }
    }

    private void deserializeClass(SerializationStreamReader streamReader, Class<?> instanceClass, Object instance)
            throws Exception {
        Set<String> clientFieldNames = serializationPolicy.getClientFieldNamesForEnhancedClass(instanceClass);
        if (clientFieldNames != null) {
            streamReader.readString();// and toss...
        }
        Field[] serializableFields = SerializabilityUtil.applyFieldSerializationPolicy(instanceClass);
        for (Field declField : serializableFields) {
            assert (declField != null);
            Object value = deserializeValue(streamReader, declField.getType());
            boolean isAccessible = declField.isAccessible();
            boolean needsAccessOverride = !isAccessible && !Modifier.isPublic(declField.getModifiers());
            if (needsAccessOverride) {
                // Override access restrictions
                declField.setAccessible(true);
            }
            declField.set(instance, value);
        }
        Class<?> superClass = instanceClass.getSuperclass();
        if (serializationPolicy.shouldDeserializeFields(superClass)) {
            deserializeImpl(streamReader, SerializabilityUtil.hasCustomFieldSerializer(superClass), superClass,
                    instance);
        }
    }

    Object deserializeValue(SerializationStreamReader streamReader, Class<?> type) throws SerializationException {
        ValueReader valueReader = SyncClientSerializationStreamReader.CLASS_TO_VALUE_READER.get(type);
        if (valueReader != null) {
            return valueReader.readValue(streamReader);
        } else {
            // Arrays of primitive or reference types need to go through
            // readObject.
            return SyncClientSerializationStreamReader.ValueReader.OBJECT.readValue(streamReader);
        }
    }

    private void deserializeWithCustomFieldDeserializer(SerializationStreamReader streamReader,
            Class<?> customSerializer, Class<?> instanceClass, Object instance)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        assert (!instanceClass.isArray());
        for (Method method : customSerializer.getMethods()) {
            if ("deserialize".equals(method.getName())) {
                method.invoke(null, streamReader, instance);
                return;
            }
        }
        throw new NoSuchMethodException("deserialize");
    }

    @Override
    public String getSerializationSignature(Class<?> clazz) {
        throw new UnsupportedOperationException();
    }

    Map<String, Class> typeLookup = new LinkedHashMap<String, Class>();

    protected Class<?> getClassCached(String cn) throws Exception {
        Class clazz = typeLookup.get(cn);
        if (clazz == null) {
            clazz = Class.forName(cn);
            typeLookup.put(cn, clazz);
        }
        return clazz;
    }

    @Override
    public Object instantiate(SerializationStreamReader streamReader, String typeSignature)
            throws SerializationException {
        try {
            Object instance = null;
            SerializedInstanceReference serializedInstRef = SerializabilityUtil
                    .decodeSerializedInstanceReference(typeSignature);
            String cn = serializedInstRef.getName();
            Class<?> instanceClass = getClassCached(cn);
            Class<?> customSerializer = SerializabilityUtil.hasCustomFieldSerializer(instanceClass);
            return instantiate(streamReader, customSerializer, instanceClass);
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void serialize(SerializationStreamWriter stream, Object instance, String typeSignature)
            throws SerializationException {
        throw new UnsupportedOperationException();
    }

    Object instantiate(SerializationStreamReader stream, Class<?> customSerializer, Class<?> instanceClass)
            throws Exception {
        if (customSerializer != null) {
            for (Method method : customSerializer.getMethods()) {
                if ("instantiate".equals(method.getName())) {
                    return method.invoke(null, stream);
                }
            }
            // Ok to not have one.
        }
        if (instanceClass.isArray()) {
            int length = stream.readInt();
            // We don't pre-allocate the array; this prevents an allocation
            // attack
            return new BoundedList<Object>(instanceClass.getComponentType(), length);
        } else if (instanceClass.isEnum()) {
            Enum<?>[] enumConstants = (Enum[]) instanceClass.getEnumConstants();
            int ordinal = stream.readInt();
            assert (ordinal >= 0 && ordinal < enumConstants.length);
            return enumConstants[ordinal];
        } else {
            Constructor<?> constructor = instanceClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        }
    }
}