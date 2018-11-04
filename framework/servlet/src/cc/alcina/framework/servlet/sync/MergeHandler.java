package cc.alcina.framework.servlet.sync;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.sync.SyncInterchangeModel;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicSupport;

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
	public static final String TOPIC_MERGE_COMPLETED = MergeHandler.class
			.getName() + "." + "TOPIC_MERGE_COMPLETED";

	public static TopicSupport<SyncMerger> topicMergeCompleted() {
		return new TopicSupport<>(TOPIC_MERGE_COMPLETED);
	}

	protected I leftInterchangeModel;

	protected I rightInterchangeModel;

	protected FlatDeltaPersister localDeltaPersister;

	protected List<SyncMerger> syncMergers = new ArrayList<SyncMerger>();

	public D deltaModel;

	public FlatDeltaPersisterResult persisterResult;

	/**
	 * returns number of successfully merged classes with non-zero merged rows
	 */
	public void run(Logger logger) throws Exception {
		List<SyncMerger> mergeIncomplete = new ArrayList<>();
		for (SyncMerger merger : syncMergers) {
			Class mergedClass = merger.getMergedClass();
			Collection leftCollection = leftInterchangeModel
					.getCollectionFor(mergedClass);
			Collection rightCollection = rightInterchangeModel
					.getCollectionFor(mergedClass);
			if (!merger.validate(leftCollection, rightCollection, logger)) {
				this.persisterResult = new FlatDeltaPersisterResult();
				return;
			}
			merger.merge(leftCollection, rightCollection, deltaModel, logger);
			topicMergeCompleted().publish(merger);
			if (merger.wasIncomplete() || mergeIncomplete.size() > 0) {
				logger.info(Ax.format("Merger incomplete:\n\t%s",
						merger.getClass().getSimpleName()));
				mergeIncomplete.add(merger);
			}
		}
		// assure 'detached to domain'
		Preconditions
				.checkState(TransformManager.get().getTransforms().size() == 0);
		beforePersistence();
		if (shouldPersist()) {
			this.persisterResult = localDeltaPersister.apply(logger, deltaModel,
					mergeIncomplete.stream().map(c -> c.getMergedClass())
							.collect(Collectors.toList()));
			this.persisterResult.allPersisted = !this.persisterResult.mergeInterrupted
					&& mergeIncomplete.isEmpty();
		} else {
			logger.info(Ax.format("Not persisting:\n\t%s", deltaModel));
		}
	}

	protected void beforePersistence() {
	}

	protected boolean shouldPersist() {
		return true;// ResourceUtilities.is(getClass(), "shouldPersist");
	}
}
