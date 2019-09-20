package cc.alcina.framework.entity.util;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.introspect.ObjectIdInfo;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CachingMap;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.JsonObjectSerializer;
import cc.alcina.framework.entity.ResourceUtilities;

@RegistryLocation(registryPoint = JsonObjectSerializer.class, implementationType = ImplementationType.INSTANCE)
public class JacksonJsonObjectSerializer implements JsonObjectSerializer {
	private static CachingMap<JacksonJsonObjectSerializer, ObjectMapperPool> objectMappersPool = new CachingConcurrentMap<>(
			serializer -> new ObjectMapperPool(serializer), 10);

	private boolean withIdRefs;

	private boolean withTypeInfo;

	private boolean withAllowUnknownProperties;

	private boolean withBase64Encoding;

	private int maxLength;

	public JacksonJsonObjectSerializer() {
		maxLength = ResourceUtilities
				.getInteger(JacksonJsonObjectSerializer.class, "maxLength",10000000);
		
	}

	private boolean withPrettyPrint;

	private boolean withDefaults = true;

	@Override
	public <T> T deserialize(String json, Class<T> clazz) {
		return runWithObjectMapper(mapper -> {
			try {
				String fJson = json;
				if (withBase64Encoding) {
					fJson = new String(Base64.getDecoder().decode(fJson),
							StandardCharsets.UTF_8);
				}
				return mapper.readValue(fJson, clazz);
			} catch (Exception e) {
				return deserialize_v1(json, clazz);
			}
		});
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof JacksonJsonObjectSerializer) {
			JacksonJsonObjectSerializer o = (JacksonJsonObjectSerializer) obj;
			return CommonUtils.equals(withIdRefs, o.withIdRefs, withTypeInfo,
					o.withTypeInfo, withAllowUnknownProperties,
					o.withAllowUnknownProperties, withBase64Encoding,
					o.withBase64Encoding, maxLength, o.maxLength,
					withPrettyPrint, o.withPrettyPrint, withDefaults,
					o.withDefaults);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(withIdRefs, withTypeInfo,
				withAllowUnknownProperties, withBase64Encoding, maxLength,
				withPrettyPrint, withDefaults);
	}

	@Override
	public String serialize(Object object) {
		return runWithObjectMapper(mapper -> {
			try {
				StringWriter writer = new LengthLimitedStringWriter(maxLength);
				if (withPrettyPrint) {
					mapper.writerWithDefaultPrettyPrinter().writeValue(writer,
							object);
				} else {
					mapper.writeValue(writer, object);
				}
				String json = writer.toString();
				if (withBase64Encoding) {
					json = Base64.getEncoder().encodeToString(
							json.getBytes(StandardCharsets.UTF_8));
				}
				return json;
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		});
	}

	public JacksonJsonObjectSerializer withAllowUnknownProperties() {
		this.withAllowUnknownProperties = true;
		return this;
	}

	public JacksonJsonObjectSerializer withBase64Encoding() {
		this.withBase64Encoding = true;
		return this;
	}

	public JacksonJsonObjectSerializer withDefaults(boolean withDefaults) {
		this.withDefaults = withDefaults;
		return this;
	}

	public JacksonJsonObjectSerializer withIdRefs() {
		this.withIdRefs = true;
		return this;
	}

	public JacksonJsonObjectSerializer withMaxLength(int maxLength) {
		this.maxLength = maxLength;
		return this;
	}

	public JacksonJsonObjectSerializer withPrettyPrint() {
		this.withPrettyPrint = true;
		return this;
	}

	public JacksonJsonObjectSerializer withTypeInfo() {
		this.withTypeInfo = true;
		return this;
	}

	private <T> T deserialize_v1(String json, Class<T> clazz) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			if (withTypeInfo) {
				mapper.enableDefaultTyping(); // default to using
												// DefaultTyping.OBJECT_AND_NON_CONCRETE
				mapper.enableDefaultTyping(
						ObjectMapper.DefaultTyping.NON_FINAL);
			}
			if (withAllowUnknownProperties) {
				mapper.configure(
						DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
						false);
			}
			return mapper.readValue(json, clazz);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private <T> T
			runWithObjectMapper(Function<ObjectMapper, T> mapperFunction) {
		ObjectMapperPool pool = objectMappersPool.get(this);
		ObjectMapper mapper = pool.borrow();
		try {
			return mapperFunction.apply(mapper);
		} finally {
			pool.returnObject(mapper);
		}
	}

	ObjectMapper createObjectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		if (withTypeInfo) {
			mapper.enableDefaultTyping(); // default to using
											// DefaultTyping.OBJECT_AND_NON_CONCRETE
			mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
		}
		if (withAllowUnknownProperties) {
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
					false);
		}
		if (withIdRefs) {
			mapper.setVisibility(mapper.getSerializationConfig()
					.getDefaultVisibilityChecker()
					.withFieldVisibility(JsonAutoDetect.Visibility.ANY)
					.withGetterVisibility(JsonAutoDetect.Visibility.ANY)
					.withSetterVisibility(JsonAutoDetect.Visibility.NONE)
					.withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
			mapper.setAnnotationIntrospector(new AddIdAnnotationIntrospector());
		}
		if (!withDefaults) {
			mapper.setSerializationInclusion(Include.NON_DEFAULT);
		}
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		return mapper;
	}

	public static class AddIdAnnotationIntrospector
			extends JacksonAnnotationIntrospector {
		@Override
		public ObjectIdInfo findObjectIdInfo(Annotated ann) {
			ObjectIdInfo annotatedResult = super.findObjectIdInfo(ann);
			if (annotatedResult == null && HasIdAndLocalId.class
					.isAssignableFrom(ann.getRawType())) {
				PropertyName name = PropertyName.construct("ref_id");
				return new ObjectIdInfo(name, Object.class,
						HiliStringIdGenerator.class,
						com.fasterxml.jackson.annotation.SimpleObjectIdResolver.class);
			} else {
				return annotatedResult;
			}
		}
	}

	public static class HiliStringIdGenerator
			extends ObjectIdGenerator<String> {
		private static final long serialVersionUID = 1L;

		public HiliStringIdGenerator() {
		}

		// Should be usable for generic Opaque String ids?
		@Override
		public boolean canUseFor(ObjectIdGenerator<?> gen) {
			return true;
		}

		// Can just return base instance since this is essentially scopeless
		@Override
		public ObjectIdGenerator<String> forScope(Class<?> scope) {
			return this;
		}

		@Override
		public String generateId(Object forPojo) {
			return ((HasIdAndLocalId) forPojo).toStringHili();
		}

		@Override
		public Class<?> getScope() {
			return Object.class;
		}

		@Override
		public IdKey key(Object key) {
			if (key == null) {
				return null;
			}
			return new IdKey(getClass(), null, key);
		}

		// Can just return base instance since this is essentially scopeless
		@Override
		public ObjectIdGenerator<String> newForSerialization(Object context) {
			return this;
		}
	}

	public static class LengthLimitedStringWriter extends StringWriter {
		private int maxLength;

		public LengthLimitedStringWriter(int maxLength) {
			this.maxLength = maxLength;
		}

		@Override
		public StringWriter append(char c) {
			checkLength(1);
			return super.append(c);
		}

		@Override
		public StringWriter append(CharSequence csq) {
			checkLength(csq.length());
			return super.append(csq);
		}

		@Override
		public StringWriter append(CharSequence csq, int start, int end) {
			checkLength(end - start);
			return super.append(csq, start, end);
		}

		@Override
		public void write(char[] cbuf, int off, int len) {
			checkLength(len);
			super.write(cbuf, off, len);
		}

		@Override
		public void write(int c) {
			checkLength(1);
			super.write(c);
		}

		@Override
		public void write(String str) {
			checkLength(str.length());
			super.write(str);
		}

		@Override
		public void write(String str, int off, int len) {
			checkLength(len);
			super.write(str, off, len);
		}

		private void checkLength(int len) {
			if (maxLength == 0) {
				return;
			}
			if (getBuffer().length() + len > maxLength) {
				String first = "";
				String last = "";
				if (getBuffer().length() <= 1000) {
					first = getBuffer().toString();
				} else {
					first = getBuffer().substring(0, 1000);
					last = getBuffer().substring(getBuffer().length() - 1000);
				}
				// stacktraces may be truncated - so print the top too
				List<StackTraceElement> frames = Arrays
						.asList(new Exception().getStackTrace());
				int fromIndex = Math.max(0, frames.size() - 200);
				List<StackTraceElement> topOfTrace = frames.subList(fromIndex,
						frames.size());
				throw Ax.runtimeException(
						"Limited writer overflow - %s bytes ::\n (0-1000): \n%s\n(last 1000)"
								+ ":\n%s\n\ntop of stack:\n%s",
						maxLength, first, last,
						CommonUtils.joinWithNewlines(topOfTrace));
			}
		}
	}

	static class ObjectMapperCache {
	}

	static class ObjectMapperPool {
		private GenericObjectPool<ObjectMapper> objectPool;

		private ObjectMapperFactory factory = new ObjectMapperFactory();

		private JacksonJsonObjectSerializer serializer;

		public ObjectMapperPool(JacksonJsonObjectSerializer serializer) {
			this.serializer = serializer;
			objectPool = new GenericObjectPool<ObjectMapper>(factory);
			objectPool.setMaxTotal(10);
		}

		public void returnObject(ObjectMapper objectMapper) {
			if (objectPool != null) {
				try {
					objectPool.returnObject(objectMapper);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		synchronized ObjectMapper borrow() {
			try {
				if (objectPool == null) {
					return factory.create();
				} else {
					return objectPool.borrowObject();
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		class ObjectMapperFactory
				extends BasePooledObjectFactory<ObjectMapper> {
			@Override
			public ObjectMapper create() throws Exception {
				return serializer.createObjectMapper();
			}

			@Override
			public PooledObject<ObjectMapper> wrap(ObjectMapper mapper) {
				return new DefaultPooledObject<ObjectMapper>(mapper);
			}
		}
	}
}
