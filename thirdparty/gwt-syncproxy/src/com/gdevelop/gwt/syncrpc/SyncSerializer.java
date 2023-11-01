package com.gdevelop.gwt.syncrpc;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.impl.Serializer;
import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.google.gwt.user.server.rpc.impl.SerializabilityUtil;
import com.google.gwt.user.server.rpc.impl.SerializedInstanceReference;

@SuppressWarnings({ "rawtypes", "unchecked", "unused", "deprecation" })
class SyncSerializer implements Serializer {
	private SerializationPolicy serializationPolicy;

	public SyncSerializer(SerializationPolicy serializationPolicy) {
		this.serializationPolicy = serializationPolicy;
	}

	@Override
	public void deserialize(SerializationStreamReader stream, Object instance,
			String typeSignature) throws SerializationException {
		SerializedInstanceReference serializedInstRef = SerializabilityUtil
				.decodeSerializedInstanceReference(typeSignature);
		Class<? extends Object> instanceClass = instance.getClass();
		Class<?> customSerializer = SerializabilityUtil
				.hasCustomFieldSerializer(instanceClass);
		try {
			deserializeImpl(stream, customSerializer, instanceClass, instance);
		} catch (Exception e) {
			throw new SerializationException(e);
		}
	}

	private Object deserializeImpl(SerializationStreamReader stream,
			Class<?> customSerializer, Class<?> instanceClass, Object instance)
			throws NoSuchMethodException, IllegalArgumentException,
			IllegalAccessException, InvocationTargetException,
			SerializationException, ClassNotFoundException {
		if (customSerializer != null) {
			deserializeWithCustomFieldDeserializer(stream, customSerializer,
					instanceClass, instance);
		} else if (instanceClass.isArray()) {
			instance = deserializeArray(stream, instanceClass, instance);
		} else if (instanceClass.isEnum()) {
			// Enums are deserialized when they are instantiated
		} else {
			deserializeClass(stream, instanceClass, instance);
		}
		return instance;
	}

	private void deserializeWithCustomFieldDeserializer(
			SerializationStreamReader stream, Class<?> customSerializer,
			Class<?> instanceClass, Object instance)
			throws NoSuchMethodException, IllegalAccessException,
			InvocationTargetException {
		assert (!instanceClass.isArray());
		for (Method method : customSerializer.getMethods()) {
			if ("deserialize".equals(method.getName())) {
				method.invoke(null, this, instance);
				return;
			}
		}
		throw new NoSuchMethodException("deserialize");
	}

	/**
	 * Deserialize an instance that is an array. Will default to deserializing
	 * as an Object vector if the instance is not a primitive vector.
	 * 
	 * @param stream
	 *
	 * @param instanceClass
	 * @param instance
	 * @throws SerializationException
	 */
	private Object deserializeArray(SerializationStreamReader stream,
			Class<?> instanceClass, Object instance)
			throws SerializationException {
		assert (instanceClass.isArray());
		BoundedList<Object> buffer = (BoundedList<Object>) instance;
		VectorReader instanceReader = CLASS_TO_VECTOR_READER.get(instanceClass);
		if (instanceReader != null) {
			return instanceReader.read(stream, buffer);
		} else {
			return VectorReader.OBJECT_VECTOR.read(stream, buffer);
		}
	}

	/**
	 * Enumeration used to provided typed instance readers.
	 */
	private enum ValueReader {
		BOOLEAN {
			@Override
			Object readValue(SerializationStreamReader stream)
					throws SerializationException {
				return stream.readBoolean();
			}
		},
		BYTE {
			@Override
			Object readValue(SerializationStreamReader stream)
					throws SerializationException {
				return stream.readByte();
			}
		},
		CHAR {
			@Override
			Object readValue(SerializationStreamReader stream)
					throws SerializationException {
				return stream.readChar();
			}
		},
		DOUBLE {
			@Override
			Object readValue(SerializationStreamReader stream)
					throws SerializationException {
				return stream.readDouble();
			}
		},
		FLOAT {
			@Override
			Object readValue(SerializationStreamReader stream)
					throws SerializationException {
				return stream.readFloat();
			}
		},
		INT {
			@Override
			Object readValue(SerializationStreamReader stream)
					throws SerializationException {
				return stream.readInt();
			}
		},
		LONG {
			@Override
			Object readValue(SerializationStreamReader stream)
					throws SerializationException {
				return stream.readLong();
			}
		},
		OBJECT {
			@Override
			Object readValue(SerializationStreamReader stream)
					throws SerializationException {
				return stream.readObject();
			}
		},
		SHORT {
			@Override
			Object readValue(SerializationStreamReader stream)
					throws SerializationException {
				return stream.readShort();
			}
		},
		STRING {
			@Override
			Object readValue(SerializationStreamReader stream)
					throws SerializationException {
				return stream.readString();
			}
		};

		abstract Object readValue(SerializationStreamReader stream)
				throws SerializationException;
	}

	/**
	 * Enumeration used to provided typed instance readers for vectors.
	 */
	private enum VectorReader {
		BOOLEAN_VECTOR {
			@Override
			protected Object readSingleValue(SerializationStreamReader stream)
					throws SerializationException {
				return stream.readBoolean();
			}

			@Override
			protected void setSingleValue(Object array, int index,
					Object value) {
				Array.setBoolean(array, index, (Boolean) value);
			}
		},
		BYTE_VECTOR {
			@Override
			protected Object readSingleValue(SerializationStreamReader stream)
					throws SerializationException {
				return stream.readByte();
			}

			@Override
			protected void setSingleValue(Object array, int index,
					Object value) {
				Array.setByte(array, index, (Byte) value);
			}
		},
		CHAR_VECTOR {
			@Override
			protected Object readSingleValue(SerializationStreamReader stream)
					throws SerializationException {
				return stream.readChar();
			}

			@Override
			protected void setSingleValue(Object array, int index,
					Object value) {
				Array.setChar(array, index, (Character) value);
			}
		},
		DOUBLE_VECTOR {
			@Override
			protected Object readSingleValue(SerializationStreamReader stream)
					throws SerializationException {
				return stream.readDouble();
			}

			@Override
			protected void setSingleValue(Object array, int index,
					Object value) {
				Array.setDouble(array, index, (Double) value);
			}
		},
		FLOAT_VECTOR {
			@Override
			protected Object readSingleValue(SerializationStreamReader stream)
					throws SerializationException {
				return stream.readFloat();
			}

			@Override
			protected void setSingleValue(Object array, int index,
					Object value) {
				Array.setFloat(array, index, (Float) value);
			}
		},
		INT_VECTOR {
			@Override
			protected Object readSingleValue(SerializationStreamReader stream)
					throws SerializationException {
				return stream.readInt();
			}

			@Override
			protected void setSingleValue(Object array, int index,
					Object value) {
				Array.setInt(array, index, (Integer) value);
			}
		},
		LONG_VECTOR {
			@Override
			protected Object readSingleValue(SerializationStreamReader stream)
					throws SerializationException {
				return stream.readLong();
			}

			@Override
			protected void setSingleValue(Object array, int index,
					Object value) {
				Array.setLong(array, index, (Long) value);
			}
		},
		OBJECT_VECTOR {
			@Override
			protected Object readSingleValue(SerializationStreamReader stream)
					throws SerializationException {
				return stream.readObject();
			}

			@Override
			protected void setSingleValue(Object array, int index,
					Object value) {
				Array.set(array, index, value);
			}
		},
		SHORT_VECTOR {
			@Override
			protected Object readSingleValue(SerializationStreamReader stream)
					throws SerializationException {
				return stream.readShort();
			}

			@Override
			protected void setSingleValue(Object array, int index,
					Object value) {
				Array.setShort(array, index, (Short) value);
			}
		},
		STRING_VECTOR {
			@Override
			protected Object readSingleValue(SerializationStreamReader stream)
					throws SerializationException {
				return stream.readString();
			}

			@Override
			protected void setSingleValue(Object array, int index,
					Object value) {
				Array.set(array, index, value);
			}
		};

		protected abstract Object readSingleValue(
				SerializationStreamReader stream) throws SerializationException;

		protected abstract void setSingleValue(Object array, int index,
				Object value);

		/**
		 * Convert a BoundedList to an array of the correct type. This
		 * implementation consumes the BoundedList.
		 */
		protected Object toArray(Class<?> componentType,
				BoundedList<Object> buffer) throws SerializationException {
			if (buffer.getExpectedSize() != buffer.size()) {
				throw new SerializationException(
						"Inconsistent number of elements received. Received "
								+ buffer.size() + " but expecting "
								+ buffer.getExpectedSize());
			}
			Object arr = Array.newInstance(componentType, buffer.size());
			for (int i = 0, n = buffer.size(); i < n; i++) {
				setSingleValue(arr, i, buffer.removeFirst());
			}
			return arr;
		}

		Object read(SerializationStreamReader stream,
				BoundedList<Object> instance) throws SerializationException {
			for (int i = 0, n = instance.getExpectedSize(); i < n; ++i) {
				instance.add(readSingleValue(stream));
			}
			return toArray(instance.getComponentType(), instance);
		}
	}

	/**
	 * Map of {@link Class} objects to {@link ValueReader}s.
	 */
	private static final Map<Class<?>, ValueReader> CLASS_TO_VALUE_READER = new IdentityHashMap<Class<?>, ValueReader>();

	/**
	 * Map of {@link Class} objects to {@link VectorReader}s.
	 */
	private static final Map<Class<?>, VectorReader> CLASS_TO_VECTOR_READER = new IdentityHashMap<Class<?>, VectorReader>();
	{
		CLASS_TO_VECTOR_READER.put(boolean[].class,
				VectorReader.BOOLEAN_VECTOR);
		CLASS_TO_VECTOR_READER.put(byte[].class, VectorReader.BYTE_VECTOR);
		CLASS_TO_VECTOR_READER.put(char[].class, VectorReader.CHAR_VECTOR);
		CLASS_TO_VECTOR_READER.put(double[].class, VectorReader.DOUBLE_VECTOR);
		CLASS_TO_VECTOR_READER.put(float[].class, VectorReader.FLOAT_VECTOR);
		CLASS_TO_VECTOR_READER.put(int[].class, VectorReader.INT_VECTOR);
		CLASS_TO_VECTOR_READER.put(long[].class, VectorReader.LONG_VECTOR);
		CLASS_TO_VECTOR_READER.put(Object[].class, VectorReader.OBJECT_VECTOR);
		CLASS_TO_VECTOR_READER.put(short[].class, VectorReader.SHORT_VECTOR);
		CLASS_TO_VECTOR_READER.put(String[].class, VectorReader.STRING_VECTOR);
		CLASS_TO_VALUE_READER.put(boolean.class, ValueReader.BOOLEAN);
		CLASS_TO_VALUE_READER.put(byte.class, ValueReader.BYTE);
		CLASS_TO_VALUE_READER.put(char.class, ValueReader.CHAR);
		CLASS_TO_VALUE_READER.put(double.class, ValueReader.DOUBLE);
		CLASS_TO_VALUE_READER.put(float.class, ValueReader.FLOAT);
		CLASS_TO_VALUE_READER.put(int.class, ValueReader.INT);
		CLASS_TO_VALUE_READER.put(long.class, ValueReader.LONG);
		CLASS_TO_VALUE_READER.put(Object.class, ValueReader.OBJECT);
		CLASS_TO_VALUE_READER.put(short.class, ValueReader.SHORT);
		CLASS_TO_VALUE_READER.put(String.class, ValueReader.STRING);
	}

	private void deserializeClass(SerializationStreamReader stream,
			Class<?> instanceClass, Object instance)
			throws SerializationException, IllegalAccessException,
			NoSuchMethodException, InvocationTargetException,
			ClassNotFoundException {
		Set<String> clientFieldNames = serializationPolicy
				.getClientFieldNamesForEnhancedClass(instanceClass);
		if (clientFieldNames != null) {
			stream.readString();// and toss...
		}
		Field[] serializableFields = SerializabilityUtil
				.applyFieldSerializationPolicy(instanceClass);
		for (Field declField : serializableFields) {
			assert (declField != null);
			Object value = deserializeValue(stream, declField.getType());
			boolean isAccessible = declField.isAccessible();
			boolean needsAccessOverride = !isAccessible
					&& !Modifier.isPublic(declField.getModifiers());
			if (needsAccessOverride) {
				// Override access restrictions
				declField.setAccessible(true);
			}
			declField.set(instance, value);
		}
		Class<?> superClass = instanceClass.getSuperclass();
		if (serializationPolicy.shouldDeserializeFields(superClass)) {
			deserializeImpl(stream,
					SerializabilityUtil.hasCustomFieldSerializer(superClass),
					superClass, instance);
		}
	}

	public Object deserializeValue(SerializationStreamReader stream,
			Class<?> type) throws SerializationException {
		ValueReader valueReader = CLASS_TO_VALUE_READER.get(type);
		if (valueReader != null) {
			return valueReader.readValue(stream);
		} else {
			// Arrays of primitive or reference types need to go through
			// readObject.
			return ValueReader.OBJECT.readValue(stream);
		}
	}

	/**
	 * Used to accumulate elements while deserializing array types. The generic
	 * type of the BoundedList will vary from the component type of the array it
	 * is intended to create when the array is of a primitive type.
	 *
	 * @param <T>
	 *            The type of object used to hold the data in the buffer
	 */
	private static class BoundedList<T> extends LinkedList<T> {
		private final Class<?> componentType;

		private final int expectedSize;

		public BoundedList(Class<?> componentType, int expectedSize) {
			this.componentType = componentType;
			this.expectedSize = expectedSize;
		}

		@Override
		public boolean add(T o) {
			assert size() < getExpectedSize();
			return super.add(o);
		}

		public Class<?> getComponentType() {
			return componentType;
		}

		public int getExpectedSize() {
			return expectedSize;
		}
	}

	@Override
	public String getSerializationSignature(Class<?> clazz) {
		return SerializabilityUtil.getSerializationSignature(clazz,
				serializationPolicy);
	}

	private Object instantiate(SerializationStreamReader stream,
			Class<?> customSerializer, Class<?> instanceClass)
			throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SerializationException {
		if (customSerializer != null) {
			for (Method method : customSerializer.getMethods()) {
				if ("instantiate".equals(method.getName())) {
					return method.invoke(null, this);
				}
			}
			// Ok to not have one.
		}
		if (instanceClass.isArray()) {
			int length = stream.readInt();
			// We don't pre-allocate the array; this prevents an allocation
			// attack
			return new BoundedList<Object>(instanceClass.getComponentType(),
					length);
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

	protected Class<?> getClassCached(String cn) throws ClassNotFoundException {
		Class clazz = typeLookup.get(cn);
		if (clazz == null) {
			clazz = Class.forName(cn);
			typeLookup.put(cn, clazz);
		}
		return clazz;
	}

	Map<String, Class> typeLookup = new LinkedHashMap<String, Class>();

	@Override
	public Object instantiate(SerializationStreamReader stream,
			String typeSignature) throws SerializationException {
		Object instance = null;
		SerializedInstanceReference serializedInstRef = SerializabilityUtil
				.decodeSerializedInstanceReference(typeSignature);
		try {
			Class<?> instanceClass = getClassCached(
					serializedInstRef.getName());
			assert (serializationPolicy != null);
			try {
				serializationPolicy.validateDeserialize(instanceClass);
			} catch (SerializationException e) {
				System.err.println("WARN: " + e.getMessage());
			}
			Class<?> customSerializer = SerializabilityUtil
					.hasCustomFieldSerializer(instanceClass);
			return instantiate(stream, customSerializer, instanceClass);
		} catch (ClassNotFoundException e) {
			throw new SerializationException(e);
		} catch (InstantiationException e) {
			throw new SerializationException(e);
		} catch (IllegalAccessException e) {
			throw new SerializationException(e);
		} catch (IllegalArgumentException e) {
			throw new SerializationException(e);
		} catch (InvocationTargetException e) {
			throw new SerializationException(e);
		} catch (NoSuchMethodException e) {
			throw new SerializationException(e);
		}
	}

	@Override
	public void serialize(SerializationStreamWriter stream, Object instance,
			String typeSignature) throws SerializationException {
		throw new UnsupportedOperationException();
	}
}