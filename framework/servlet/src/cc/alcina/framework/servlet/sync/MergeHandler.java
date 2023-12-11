package cc.alcina.framework.servlet.sync;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.sync.SyncInterchangeModel;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.servlet.sync.FlatDeltaPersister.PersistElementResult;

/**
 * Handle merge - run the mergers (generally per-interchange-model member class)
 * persist the generated delta model
 *
 * 
 *
 * @param <I>
 * @param <D>
 */
public abstract class MergeHandler<I extends SyncInterchangeModel, D extends SyncDeltaModel> {
	public Topic<SyncMerger> topicMergeCompleted = Topic.create();

	public Topic<SyncMerger> topicBeforeMerge = Topic.create();

	public Topic<SyncMerger> topicDeltaPersisted = Topic.create();

	public Topic<FlatDeltaPersister.PersistElementResult> topicElementPersisted = Topic
			.create();

	protected I leftInterchangeModel;

	protected I rightInterchangeModel;

	protected FlatDeltaPersister localDeltaPersister;

	protected List<SyncMerger> syncMergers = new ArrayList<SyncMerger>();

	public int getSyncMergerCount() {
		return syncMergers.size();
	}

	public D deltaModel;

	public FlatDeltaPersisterResult persisterResult;

	public String getName() {
		String simpleName = getClass().getSimpleName();
		String regex = "([A-Z][a-z]+)([A-Z][a-z]+)MergeHandler";
		if (simpleName.matches(regex)) {
			return simpleName.replaceFirst(regex, "$1 >> $2");
		} else {
			return simpleName;
		}
	}

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
			topicBeforeMerge.publish(merger);
			merger.merge(leftCollection, rightCollection, deltaModel, logger);
			topicMergeCompleted.publish(merger);
			if (merger.wasIncomplete() || mergeIncomplete.size() > 0) {
				logger.info(Ax.format("Merger incomplete:\n\t%s",
						merger.getClass().getSimpleName()));
				mergeIncomplete.add(merger);
			}
		}
		// assure 'detached to domain' (the current transaction job/status
		// transforms )
		Preconditions.checkState(TransformManager.get().getTransforms().stream()
				.filter(t -> !Job.class.isAssignableFrom(t.getObjectClass()))
				.count() == 0);
		beforePersistence();
		if (shouldPersist()) {
			this.persisterResult = localDeltaPersister.apply(logger, deltaModel,
					mergeIncomplete.stream().map(c -> c.getMergedClass())
							.collect(Collectors.toList()),
					e -> topicElementPersisted
							.publish((PersistElementResult) e));
			this.persisterResult.allPersisted = !this.persisterResult.mergeInterrupted
					&& mergeIncomplete.isEmpty();
		} else {
			logger.info(Ax.format("Not persisting:\n\t%s", deltaModel));
		}
	}

	protected void beforePersistence() {
	}

	protected boolean shouldPersist() {
		return true;// Configuration.is( "shouldPersist");
	}
}
