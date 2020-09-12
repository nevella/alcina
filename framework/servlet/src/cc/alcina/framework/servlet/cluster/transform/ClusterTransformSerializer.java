package cc.alcina.framework.servlet.cluster.transform;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.domaintransform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.util.JacksonJsonObjectSerializer;
import cc.alcina.framework.entity.util.JacksonUtils;

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
		byte[] result = null;
		String json = new JacksonJsonObjectSerializer().withIdRefs()
				.withTypeInfo().withDefaults(false).serialize(request);
		byte[] unzipped = json.getBytes(StandardCharsets.UTF_8);
		byte[] zipped = ResourceUtilities.gzipBytes(unzipped);
		if (zipped.length > 100000 || request.getEvents().size() > 1000) {
			logger.info(
					"Large serialized request :: {} :: {} events :: {} bytes",
					request.getId(), request.getEvents().size(), zipped.length);
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
	}
}
