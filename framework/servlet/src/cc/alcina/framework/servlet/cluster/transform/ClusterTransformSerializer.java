package cc.alcina.framework.servlet.cluster.transform;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.transform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.util.JacksonJsonObjectSerializer;
import cc.alcina.framework.entity.util.JacksonUtils;
import cc.alcina.framework.servlet.cluster.transform.ClusterTransformRequest.State;

@RegistryLocation(registryPoint = ClusterTransformSerializer.class, implementationType = ImplementationType.SINGLETON)
public class ClusterTransformSerializer {
	private static final int CHUNK_SIZE = 500000;

	private Logger logger = LoggerFactory.getLogger(getClass());

	private Multimap<String, List<KafkaPacket>> packets = new Multimap<>();

	private KafkaPacket lastPacket;

	private AtomicInteger localSequenceCounter = new AtomicInteger(1);

	public ClusterTransformRequest deserialize(byte[] data) {
		KafkaPacket packet = deserializePacket(data);
		byte[] assembled = null;
		synchronized (packets) {
			packets.add(packet.sequenceIdx, packet);
			if (packets.get(packet.sequenceIdx).size() == packet.chunkCount) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				packets.remove(packet.sequenceIdx).stream()
						.sorted(Comparator.comparing(p -> p.chunkIdx))
						.forEach(p -> {
							try {
								out.write(p.bytes);
							} catch (IOException e) {
								throw new WrappedRuntimeException(e);
							}
						});
				assembled = out.toByteArray();
			} else {
				return null;
			}
		}
		try {
			byte[] unzipped = ResourceUtilities.gunzipBytes(assembled);
			String json = new String(unzipped, StandardCharsets.UTF_8);
			json = preProcessJson(json);
			ClusterTransformRequest request = JacksonUtils.deserialize(json,
					ClusterTransformRequest.class);
			return request;
		} catch (Exception e) {
			// application-fatal. how sad
			throw new WrappedRuntimeException(e);
		}
	}

	public String getLastPartialId() {
		return Ax.format("%s::%s", lastPacket.sequenceIdx, lastPacket.chunkIdx);
	}

	public List<byte[]> serialize(DomainTransformRequestPersistent request,
			State state) {
		request = projectRequest(request);
		ClusterTransformRequest clusterRequest = new ClusterTransformRequest();
		clusterRequest.id = request.getId();
		clusterRequest.state = state;
		/*
		 * Only send payload in PRE_COMMIT phase - this ensures that the request
		 * is at least sent to kafka before db commit.
		 * 
		 * Kafka network conditions may mean it's received after a corresponding
		 * db update, but at least we won't hit gc() events etc between db
		 * update and packet send
		 */
		if (state == State.PRE_COMMIT) {
			clusterRequest.request = request;
		}
		List<byte[]> result = new ArrayList<>();
		byte[] zipped = new byte[0];
		byte[] unzipped = null;
		try {
			String json = new JacksonJsonObjectSerializer().withIdRefs()
					.withTypeInfo().withDefaults(false)
					.withMaxLength(Integer.MAX_VALUE).serialize(clusterRequest);
			unzipped = json.getBytes(StandardCharsets.UTF_8);
			zipped = ResourceUtilities.gzipBytes(unzipped);
			if (state == State.PRE_COMMIT) {
				if (zipped == null || zipped.length > 100000
						|| request.getEvents().size() > 1000) {
					logger.info(
							"Large serialized request :: {} :: {} events :: {} bytes unzipped :: {} bytes zipped",
							request.getId(), request.getEvents().size(),
							unzipped.length, zipped.length);
				}
			}
			/*
			 * Need one chunk for messages with zero-length bytes
			 */
			int chunkCount = ((zipped.length - 1) / CHUNK_SIZE) + 1;
			chunkCount = Math.max(chunkCount, 1);
			String sequenceIdx = Ax.format("%s::%s",
					EntityLayerUtils.getLocalHostName(),
					localSequenceCounter.getAndIncrement());
			for (int idx = 0; idx < chunkCount; idx += 1) {
				KafkaPacket packet = new KafkaPacket();
				packet.sequenceIdx = sequenceIdx;
				packet.chunkIdx = idx;
				packet.chunkCount = chunkCount;
				int startArrayPos = idx * CHUNK_SIZE;
				int endArrayPos = Math.min(startArrayPos + CHUNK_SIZE,
						zipped.length);
				packet.dataLength = endArrayPos - startArrayPos;
				packet.bytes = new byte[packet.dataLength];
				System.arraycopy(zipped, startArrayPos, packet.bytes, 0,
						packet.dataLength);
				ByteArrayOutputStream outBytes = new ByteArrayOutputStream(
						packet.dataLength + 100);
				DataOutputStream stream = new DataOutputStream(outBytes);
				stream.writeInt(packet.protocolVersion);
				stream.writeUTF(packet.sequenceIdx);
				stream.writeInt(packet.chunkIdx);
				stream.writeInt(packet.chunkCount);
				stream.writeInt(packet.dataLength);
				stream.write(packet.bytes);
				result.add(outBytes.toByteArray());
			}
		} catch (Exception e) {
			logger.info("Issue serializing request {}", request.getId());
			logger.warn("Issue serializing reques", e);
		}
		return result;
	}

	private KafkaPacket deserializePacket(byte[] data) {
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			DataInputStream stream = new DataInputStream(bais);
			KafkaPacket result = new KafkaPacket();
			result.protocolVersion = stream.readInt();
			result.sequenceIdx = stream.readUTF();
			result.chunkIdx = stream.readInt();
			result.chunkCount = stream.readInt();
			result.dataLength = stream.readInt();
			result.bytes = new byte[result.dataLength];
			stream.read(result.bytes);
			lastPacket = result;
			return result;
		} catch (Exception e) {
			// application-fatal. how sad
			throw new WrappedRuntimeException(e);
		}
	}

	private DomainTransformRequestPersistent
			projectRequest(DomainTransformRequestPersistent request) {
		DomainTransformRequestPersistent result = Reflections
				.newInstance(request.getClass());
		result.setId(request.getId());
		result.setEvents(
				request.getEvents().stream().collect(Collectors.toList()));
		result.setChunkUuidString(request.getChunkUuidString());
		ClientInstance originalClientInstance = request.getClientInstance();
		ClientInstance clientInstance = (ClientInstance) Reflections
				.newInstance(originalClientInstance.entityClass());
		clientInstance.setId(originalClientInstance.getId());
		clientInstance.setAuth(originalClientInstance.getAuth());
		result.setClientInstance(clientInstance);
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
		return result;
	}

	/*
	 * For subclasses, to handle multi-domain-store incoming requests
	 */
	protected String preProcessJson(String json) {
		return json;
	}

	static class KafkaPacket {
		int protocolVersion = 1;

		String sequenceIdx;

		int chunkIdx;

		int chunkCount;

		int dataLength = 0;

		byte[] bytes = new byte[0];
	}
}
