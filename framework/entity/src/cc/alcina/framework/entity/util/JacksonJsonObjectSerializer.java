package cc.alcina.framework.entity.util;

import java.io.StringWriter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.ObjectIdGenerators.PropertyGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.introspect.ObjectIdInfo;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.JsonObjectSerializer;
import cc.alcina.framework.entity.ResourceUtilities;

@RegistryLocation(registryPoint = JsonObjectSerializer.class, implementationType = ImplementationType.INSTANCE)
public class JacksonJsonObjectSerializer implements JsonObjectSerializer {
	private boolean withIdRefs;

	private boolean withTypeInfo;

	private boolean withAllowUnknownProperties;

	private int maxLength = ResourceUtilities
			.getInteger(JacksonJsonObjectSerializer.class, "maxLength");

	@Override
	public <T> T deserialize(String json, Class<T> clazz) {
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
			mapper.writerWithDefaultPrettyPrinter().writeValue(writer, object);
			return writer.toString();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public JacksonJsonObjectSerializer withAllowUnknownProperties() {
		this.withAllowUnknownProperties = true;
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

	public static class AddIdAnnotationIntrospector
			extends JacksonAnnotationIntrospector {
		@Override
		public ObjectIdInfo findObjectIdInfo(Annotated ann) {
			ObjectIdInfo annotatedResult = super.findObjectIdInfo(ann);
			if (annotatedResult == null && HasIdAndLocalId.class
					.isAssignableFrom(ann.getRawType())) {
				PropertyName name = PropertyName.construct("id");
				return new ObjectIdInfo(name, Object.class,
						PropertyGenerator.class,
						com.fasterxml.jackson.annotation.SimpleObjectIdResolver.class);
			} else {
				return annotatedResult;
			}
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
				throw Ax.runtimeException(
						"Limited writer overflow - %s bytes ::\n (0-1000): \n%s\n(last 1000)"
								+ ":\n%s",
						maxLength, first, last);
			}
		}
	}
}
