package cc.alcina.framework.servlet.sync;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.sync.SyncInterchangeModel;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicSupport;
import cc.alcina.framework.entity.ResourceUtilities;

/**
 * Handle merge - run the mergers (generally per-interchange-model member class)
 * persist the generated delta model
 * 
 * @author nick@alcina.cc
 *
 * @param <I>
 * @param <D>
 */
public abstract class MergeHandler<I extends SyncInterchangeModel, D extends SyncDeltaModel> {
	protected I leftInterchangeModel;

	protected I rightInterchangeModel;

	protected FlatDeltaPersister localDeltaPersister;

	protected List<SyncMerger> syncMergers = new ArrayList<SyncMerger>();

	public D deltaModel;

	public FlatDeltaPersisterResult persisterResult;

	public static final String TOPIC_MERGE_COMPLETED = MergeHandler.class
			.getName() + "." + "TOPIC_MERGE_COMPLETED";

	public static TopicSupport<SyncMerger> topicMergeCompleted() {
		return new TopicSupport<>(TOPIC_MERGE_COMPLETED);
	}

	public void run(Logger logger) throws Exception {
		for (SyncMerger merger : syncMergers) {
			Class mergedClass = merger.getMergedClass();
			merger.merge(leftInterchangeModel.getCollectionFor(mergedClass),
					rightInterchangeModel.getCollectionFor(mergedClass),
					deltaModel, logger);
			topicMergeCompleted().publish(merger);
		}
		//assure 'detached to domain' 
		Preconditions.checkState(TransformManager.get().getTransforms().size()==0);
		beforePersistence();
		if (shouldPersist()) {
			this.persisterResult = localDeltaPersister.apply(deltaModel);
		}
	}

	private boolean shouldPersist() {
		return ResourceUtilities.is(getClass(), "shouldPersist");
	}

	protected void beforePersistence() {
	}
}
