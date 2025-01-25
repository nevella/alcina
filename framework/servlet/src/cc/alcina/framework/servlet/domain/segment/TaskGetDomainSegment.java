package cc.alcina.framework.servlet.domain.segment;

import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.persistence.domain.segment.DomainSegment;
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

	public void serialize() {
		localStateSerialized = Io.write().object(localState).withCompress(true)
				.toBase64String();
	}

	@Override
	public void run() throws Exception {
		localState = Io.read().base64String(localStateSerialized)
				.withDecompress(true).asObject();
		CollectionProjectionFilterWithCache dataFilter = new CollectionProjectionFilterWithCache();
		new GraphProjection(definition.getProjectionFilter(), dataFilter)
				.project(definition.getRoots().toList(), null);
		DetachedEntityCache cache = dataFilter.getCache();
		DomainSegment result = new DomainSegment();
		result.addCache(cache);
		result.filterExisting(localState);
		String b64gz = Io.write().object(result).withCompress(true)
				.toBase64String();
		JobContext.get().recordLargeInMemoryResult(b64gz);
	}
}
