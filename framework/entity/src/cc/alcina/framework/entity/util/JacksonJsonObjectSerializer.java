package cc.alcina.framework.entity.util;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.introspect.ObjectIdInfo;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CachingMap;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.JsonObjectSerializer;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.persistence.mvcc.MvccObject;

@Registration(JsonObjectSerializer.class)
public class JacksonJsonObjectSerializer implements JsonObjectSerializer {
	public static final String CONTEXT_WITHOUT_MAPPER_POOL = JacksonJsonObjectSerializer.class
			+ ".CONTEXT_NO_MAPPER";

	private static CachingMap<JacksonJsonObjectSerializer, ObjectMapperPool> objectMappersPool = new CachingConcurrentMap<>(
			serializer -> new ObjectMapperPool(serializer), 10);

	private boolean withIdRefs;

	private boolean withTypeInfo;

	private boolean withAllowUnknownProperties;

	private boolean withBase64Encoding;

	private int maxLength;

	private boolean withPrettyPrint;

	private boolean withDefaults = true;

	private boolean withDuplicateIdRefCheck = false;

	private boolean truncateAtMaxLength;

	private boolean withWrapRootValue;

	public JacksonJsonObjectSerializer() {
		maxLength = ResourceUtilities.getInteger(
				JacksonJsonObjectSerializer.class, "maxLength", 10000000);
	}

	public <T> T deserialize(Reader reader, Class<T> clazz) {
		return runWithObjectMapper(mapper -> {
			try {
				return mapper.readValue(reader, clazz);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		});
	}

	@Override
	public <T> T deserialize(String deserJson, Class<T> clazz) {
		return runWithObjectMapper(mapper -> {
			String json = deserJson;
			try {
				if (withBase64Encoding) {
					json = new String(Base64.getDecoder().decode(json),
							StandardCharsets.UTF_8);
				}
				if (withDuplicateIdRefCheck
						&& hasDuplicateIds(mapper.readTree(json))) {
				} else {
					return mapper.readValue(json, clazz);
				}
			} catch (Exception e) {
				Ax.err(e.getMessage());
				int debug = 3;
				// deserialization issue
			}
			return deserialize_v1(json, clazz);
		});
	}

	public JsonNode deserializeJson(String deserJson) {
		return runWithObjectMapper(mapper -> {
			String json = deserJson;
			try {
				return mapper.readTree(json);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
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
					o.withDefaults, withWrapRootValue, o.withWrapRootValue);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(withIdRefs, withTypeInfo,
				withAllowUnknownProperties, withBase64Encoding, maxLength,
				withPrettyPrint, withDefaults, withWrapRootValue);
	}

	@Override
	public String serialize(Object object) {
		return runWithObjectMapper(mapper -> {
			try {
				StringWriter writer = new LengthConstrainedStringWriter(
						maxLength, truncateAtMaxLength);
				if (withPrettyPrint) {
					SerializationConfig config = mapper
							.getSerializationConfig();
					DefaultPrettyPrinter prettyPrinter = (DefaultPrettyPrinter) config
							.constructDefaultPrettyPrinter();
					prettyPrinter.indentArraysWith(
							DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
					ObjectWriter objectWriter = mapper.writer(prettyPrinter);
					objectWriter.writeValue(writer, object);
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

	public String serializeJson(JsonNode node) {
		return runWithObjectMapper(mapper -> {
			try {
				return mapper.writeValueAsString(node);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		});
	}

	public void serializeToStream(Object object, OutputStream outputStream) {
		runWithObjectMapper(mapper -> {
			try {
				OutputStreamWriter writer = new OutputStreamWriter(outputStream,
						StandardCharsets.UTF_8);
				mapper.writeValue(writer, object);
				return null;
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

	@Deprecated
	public /*
			 * Ambiguity of empty string/null means false can cause
			 * deserialization havoc (if used as a true serialization format)
			 */
	JacksonJsonObjectSerializer withDefaults(boolean withDefaults) {
		this.withDefaults = withDefaults;
		return this;
	}

	public JacksonJsonObjectSerializer
			withDefaultsIgnoreDeserializationAmbiguity(boolean withDefaults) {
		this.withDefaults = withDefaults;
		return this;
	}

	public JacksonJsonObjectSerializer
			withDuplicateIdRefCheck(boolean withDuplicateIdRefCheck) {
		this.withDuplicateIdRefCheck = withDuplicateIdRefCheck;
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

	public JacksonJsonObjectSerializer
			withTruncateAtMaxLength(boolean truncateAtMaxLength) {
		this.truncateAtMaxLength = truncateAtMaxLength;
		return this;
	}

	public JacksonJsonObjectSerializer withTypeInfo() {
		this.withTypeInfo = true;
		return this;
	}

	public JacksonJsonObjectSerializer withWrapRootValue() {
		this.withWrapRootValue = true;
		return this;
	}

	private <T> T deserialize_v1(String json, Class<T> clazz) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			if (withTypeInfo) {
				// default to using
				mapper.enableDefaultTyping();
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

	private boolean hasDuplicateIds(JsonNode root) {
		Set<String> refIds = new LinkedHashSet<>();
		Stack<JsonNode> nodes = new Stack<>();
		nodes.push(root);
		while (nodes.size() > 0) {
			JsonNode node = nodes.pop();
			if (node.has("ref_id")) {
				if (!refIds.add(node.get("ref_id").asText())) {
					return true;
				}
			}
			node.elements().forEachRemaining(nodes::push);
		}
		return false;
	}

	private <T> T
			runWithObjectMapper(Function<ObjectMapper, T> mapperFunction) {
		if (LooseContext.is(CONTEXT_WITHOUT_MAPPER_POOL)) {
			ObjectMapper mapper = createObjectMapper();
			return mapperFunction.apply(mapper);
		} else {
			ObjectMapperPool pool = objectMappersPool.get(this);
			ObjectMapper mapper = pool.borrow();
			try {
				return mapperFunction.apply(mapper);
			} finally {
				pool.returnObject(mapper);
			}
		}
	}

	ObjectMapper createObjectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		if (withTypeInfo) {
			// default to using
			mapper.enableDefaultTyping();
			// DefaultTyping.OBJECT_AND_NON_CONCRETE
			mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
		}
		if (withAllowUnknownProperties) {
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
					false);
		}
		if (withWrapRootValue) {
			mapper.enable(SerializationFeature.WRAP_ROOT_VALUE);
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
			if (annotatedResult == null
					&& Entity.class.isAssignableFrom(ann.getRawType())) {
				if (MvccObject.class.isAssignableFrom(ann.getRawType())) {
					throw new RuntimeException(
							"Cannot jackson/serialize mvcc objects - project first");
				}
				PropertyName name = PropertyName.construct("ref_id");
				return new ObjectIdInfo(name, Object.class,
						EntityStringIdGenerator.class,
						com.fasterxml.jackson.annotation.SimpleObjectIdResolver.class);
			} else {
				return annotatedResult;
			}
		}
	}

	public static class EntityStringIdGenerator
			extends ObjectIdGenerator<String> {
		private static final long serialVersionUID = 1L;

		public EntityStringIdGenerator() {
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
			return ((Entity) forPojo).toStringEntity();
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
