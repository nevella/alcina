package cc.alcina.framework.entity.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.JsonObjectSerializer;

@RegistryLocation(registryPoint = JsonObjectSerializer.class, implementationType = ImplementationType.INSTANCE)
public class JacksonJsonObjectSerializer implements JsonObjectSerializer {
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
			return mapper.writeValueAsString(object);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}
}
