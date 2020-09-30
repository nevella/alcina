package cc.alcina.framework.servlet.cluster.transform;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.AuthenticationSession;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.domaintransform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.util.JacksonJsonObjectSerializer;
import cc.alcina.framework.entity.util.JacksonUtils;

@RegistryLocation(registryPoint = ClusterTransformSerializer.class, implementationType = ImplementationType.INSTANCE)
public class ClusterTransformSerializer {
	Logger logger = LoggerFactory.getLogger(getClass());

	public ClusterTransformRequest deserialize(byte[] data) {
		ClusterTransformRequest result = new ClusterTransformRequest();
		if (data.length < 20) {
			StringDeserializer deserializer = new StringDeserializer();
			String idString = deserializer.deserialize(null, data);
			deserializer.close();
			result.id = Long.parseLong(idString);
		} else {
			try {
				byte[] unzipped = ResourceUtilities.gunzipBytes(data);
				String json = new String(unzipped, StandardCharsets.UTF_8);
				json = preProcessJson(json);
				DomainTransformRequestPersistent request = JacksonUtils
						.deserialize(json,
								DomainTransformRequestPersistent.class);
				result.request = request;
				result.id = request.getId();
			} catch (Exception e) {
				// application-fatal. how sad
				throw new WrappedRuntimeException(e);
			}
		}
		return result;
	}

	public byte[] serialize(DomainTransformRequestPersistent request) {
		request = projectRequest(request);
		byte[] zipped = null;
		byte[] unzipped = null;
		byte[] result = null;
		try {
			String json = new JacksonJsonObjectSerializer().withIdRefs()
					.withTypeInfo().withDefaults(false)
					.withMaxLength(Integer.MAX_VALUE).serialize(request);
			unzipped = json.getBytes(StandardCharsets.UTF_8);
			zipped = ResourceUtilities.gzipBytes(unzipped);
		} catch (Exception e) {
			logger.info("Issue serializing request {}", request.getId());
			logger.warn("Issue serializing reques", e);
		}
		if (zipped == null || zipped.length > 100000
				|| request.getEvents().size() > 1000) {
			logger.info(
					"Large serialized request :: {} :: {} events :: {} bytes unzipped :: {} bytes zipped",
					request.getId(), request.getEvents().size(),
					unzipped.length, zipped.length);
		}
		if (zipped.length > 900000) {
			logger.info(
					"Large serialized request :: dropping :: {} ::  {} bytes zipped",
					request.getId(), zipped.length);
		}
		if (zipped.length > 900000
				|| ResourceUtilities.is("serializeRequestIdOnly")) {
			// in these (pretty rare) events, fall back to db-based
			// transformrequest loading in the domain store
			StringSerializer serializer = new StringSerializer();
			byte[] bytes = serializer.serialize(null,
					String.valueOf(request.getId()));
			serializer.close();
			return bytes;
		} else {
			return zipped;
		}
		//
	}

	private DomainTransformRequestPersistent
			projectRequest(DomainTransformRequestPersistent request) {
		request.setEvents(
				request.getEvents().stream().collect(Collectors.toList()));
		ClientInstance originalClientInstance = request.getClientInstance();
		ClientInstance clientInstance = (ClientInstance) Reflections
				.newInstance(originalClientInstance.entityClass());
		clientInstance.setId(originalClientInstance.getId());
		clientInstance.setAuth(originalClientInstance.getAuth());
		request.setClientInstance(clientInstance);
		AuthenticationSession originalAuthenticationSession = originalClientInstance
				.getAuthenticationSession();
		AuthenticationSession authenticationSession = (AuthenticationSession) Reflections
				.newInstance(originalAuthenticationSession.entityClass());
		authenticationSession.setId(originalAuthenticationSession.getId());
		IUser originalUser = originalClientInstance.provideUser();
		IUser user = (IUser) Reflections
				.newInstance(((Entity) originalUser).entityClass());
		user.setUserName(originalUser.getUserName());
		user.setId(originalUser.getId());
		authenticationSession.setUser(user);
		clientInstance.setAuthenticationSession(authenticationSession);
		return request;
	}

	/*
	 * For subclasses, to handle multi-domain-store incoming requests
	 */
	protected String preProcessJson(String json) {
		return json;
	}
}
