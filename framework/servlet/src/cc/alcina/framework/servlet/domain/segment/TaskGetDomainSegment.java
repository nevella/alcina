package cc.alcina.framework.servlet.domain.segment;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.persistence.domain.segment.DomainSegment;
import cc.alcina.framework.entity.persistence.transform.TransformCommit;
import cc.alcina.framework.entity.projection.CollectionProjectionFilterWithCache;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.servlet.job.JobContext;
import cc.alcina.framework.servlet.schedule.PerformerTask;

@TypeSerialization(flatSerializable = false)
public class TaskGetDomainSegment extends PerformerTask.Remote {
	@Property.Not
	public transient DomainSegment localState;

	public String localStateSerialized;

	public DomainSegment.Definition definition;

	@Property.Not
	public transient DomainSegment result;

	public void serialize() {
		localStateSerialized = Io.write().asKryo(true).object(localState)
				.withCompress(true).toBase64String();
		logger.info("localstate size (rpc) :: {} bytes",
				localStateSerialized.length());
	}

	@Override
	public void run() throws Exception {
		localState = Io.read().base64String(localStateSerialized)
				.withDecompress(true).withKryoType(DomainSegment.class)
				.asObject();
		CollectionProjectionFilterWithCache dataFilter = definition
				.provideDataFilter();
		LooseContext.set(GraphProjection.CONTEXT_MAX_REACHED,
				String.valueOf(Integer.MAX_VALUE));
		new GraphProjection(definition.provideFieldFilter(), dataFilter)
				.project(definition.provideRoots().toList(), null);
		DetachedEntityCache cache = dataFilter.getCache();
		logger.info("Projected:\n{}\nAll values: {}", cache.sizes(),
				cache.allValues().size());
		this.result = new DomainSegment();
		result.addCache(cache);
		result.filterExisting(localState);
		if (Ax.isTest() && !TransformCommit.isCommitTestTransforms()) {
		} else {
			String b64gz = Io.write().asKryo(true).object(result)
					.withCompress(true).toBase64String();
			JobContext.get().recordLargeInMemoryResult(b64gz);
		}
	}
}
