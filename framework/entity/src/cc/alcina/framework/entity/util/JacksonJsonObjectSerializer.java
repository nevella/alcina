package cc.alcina.framework.entity.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.ObjectIdGenerators.PropertyGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.introspect.ObjectIdInfo;
import com.fasterxml.jackson.databind.module.SimpleModule;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.JsonObjectSerializer;

@RegistryLocation(registryPoint = JsonObjectSerializer.class, implementationType = ImplementationType.INSTANCE)
public class JacksonJsonObjectSerializer implements JsonObjectSerializer {
	private boolean withIdRefs;

	public JacksonJsonObjectSerializer withIdRefs() {
		this.withIdRefs = true;
		return this;
	}

	@Override
	public <T> T deserialize(String json, Class<T> clazz) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			return mapper.readValue(json, clazz);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	public String serialize(Object object) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			if (withIdRefs) {
				mapper.setVisibility(mapper.getSerializationConfig()
						.getDefaultVisibilityChecker()
						.withFieldVisibility(JsonAutoDetect.Visibility.NONE)
						.withGetterVisibility(JsonAutoDetect.Visibility.ANY)
						.withSetterVisibility(JsonAutoDetect.Visibility.NONE)
						.withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
				mapper.getSerializationConfig()
						.with(new AddIdAnnotationIntrospector());
				SimpleModule module = new SimpleModule("HiliIdModule",
						new Version(0, 1, 0, "alpha", "", "")) {
					@Override
					public void setupModule(SetupContext context) {
						context.insertAnnotationIntrospector(
								new AddIdAnnotationIntrospector());
						super.setupModule(context);
					}
				};
				mapper.registerModule(module);
				return mapper.writerWithDefaultPrettyPrinter()
						.writeValueAsString(object);
			} else {
				return mapper.writeValueAsString(object);
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	static class AddIdAnnotationIntrospector
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
}
