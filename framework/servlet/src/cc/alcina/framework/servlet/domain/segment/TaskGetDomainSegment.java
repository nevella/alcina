package cc.alcina.framework.servlet.domain.segment;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.KryoUtils;
import cc.alcina.framework.entity.persistence.AppPersistenceBase;
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
	}

	@Override
	public void run() throws Exception {
		localState = Io.read().base64String(localStateSerialized)
				.withDecompress(true).withKryoType(DomainSegment.class)
				.asObject();
		CollectionProjectionFilterWithCache dataFilter = new CollectionProjectionFilterWithCache();
		LooseContext.set(GraphProjection.CONTEXT_MAX_REACHED,
				String.valueOf(Integer.MAX_VALUE));
		LooseContext.setTrue(KryoUtils.CONTEXT_USE_COMPATIBLE_FIELD_SERIALIZER);
		LooseContext.setBoolean(KryoUtils.CONTEXT_USE_UNSAFE_FIELD_SERIALIZER,
				false);
		new GraphProjection(definition.provideProjectionFilter(), dataFilter)
				.project(definition.provideRoots().toList(), null);
		DetachedEntityCache cache = dataFilter.getCache();
		Ax.out("Projected:\n%s\nAll values: %s", cache.sizes(),
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
