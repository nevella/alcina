package cc.alcina.framework.entity.util;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
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
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.JsonObjectSerializer;
import cc.alcina.framework.entity.ResourceUtilities;

@RegistryLocation(registryPoint = JsonObjectSerializer.class, implementationType = ImplementationType.INSTANCE)
public class JacksonJsonObjectSerializer implements JsonObjectSerializer {
	private boolean withIdRefs;

	private boolean withTypeInfo;

	private boolean withAllowUnknownProperties;

	private boolean withBase64Encoding;

	private int maxLength = ResourceUtilities
			.getInteger(JacksonJsonObjectSerializer.class, "maxLength");

	@Override
	public <T> T deserialize(String json, Class<T> clazz) {
		try {
			if (withBase64Encoding) {
				json = new String(Base64.getDecoder().decode(json),
						StandardCharsets.UTF_8);
			}
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
			if (withIdRefs) {
				mapper.setVisibility(mapper.getSerializationConfig()
						.getDefaultVisibilityChecker()
						.withFieldVisibility(JsonAutoDetect.Visibility.ANY)
						.withGetterVisibility(JsonAutoDetect.Visibility.ANY)
						.withSetterVisibility(JsonAutoDetect.Visibility.NONE)
						.withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
				mapper.setAnnotationIntrospector(
						new AddIdAnnotationIntrospector());
			}
			return mapper.readValue(json, clazz);
		} catch (Exception e) {
			return deserialize_v1(json, clazz);
		}
	}

	@Override
	public String serialize(Object object) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			if (withTypeInfo) {
				mapper.enableDefaultTyping(); // default to using
												// DefaultTyping.OBJECT_AND_NON_CONCRETE
				mapper.enableDefaultTyping(
						ObjectMapper.DefaultTyping.NON_FINAL);
			}
			StringWriter writer = new LengthLimitedStringWriter(maxLength);
			if (withIdRefs) {
				mapper.setVisibility(mapper.getSerializationConfig()
						.getDefaultVisibilityChecker()
						.withFieldVisibility(JsonAutoDetect.Visibility.ANY)
						.withGetterVisibility(JsonAutoDetect.Visibility.ANY)
						.withSetterVisibility(JsonAutoDetect.Visibility.NONE)
						.withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
				mapper.setAnnotationIntrospector(
						new AddIdAnnotationIntrospector());
			}
			mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
			mapper.writerWithDefaultPrettyPrinter().writeValue(writer, object);
			String json = writer.toString();
			if (withBase64Encoding) {
				json = Base64.getEncoder()
						.encodeToString(json.getBytes(StandardCharsets.UTF_8));
			}
			return json;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public JacksonJsonObjectSerializer withAllowUnknownProperties() {
		this.withAllowUnknownProperties = true;
		return this;
	}

	public JacksonJsonObjectSerializer withBase64Encoding() {
		this.withBase64Encoding = true;
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
}
