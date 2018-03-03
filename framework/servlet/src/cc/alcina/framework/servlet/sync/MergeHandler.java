package cc.alcina.framework.servlet.sync;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.sync.SyncInterchangeModel;
import cc.alcina.framework.common.client.util.LooseContext;
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

	private static final String CONTEXT_LEFT_MODEL = MergeHandler.class
			.getName() + ".CONTEXT_LEFT_MODEL";

	private static final String CONTEXT_RIGHT_MODEL = MergeHandler.class
			.getName() + ".CONTEXT_RIGHT_MODEL";

	public static <I extends SyncInterchangeModel> I leftModel() {
		return LooseContext.get(CONTEXT_LEFT_MODEL);
	}

	public static <I extends SyncInterchangeModel> I rightModel() {
		return LooseContext.get(CONTEXT_RIGHT_MODEL);
	}

	public void run(Logger logger) throws Exception {
		try {
			LooseContext.push();
			LooseContext.set(CONTEXT_LEFT_MODEL,leftInterchangeModel);
			LooseContext.set(CONTEXT_RIGHT_MODEL,rightInterchangeModel);
			for (SyncMerger merger : syncMergers) {
				Class mergedClass = merger.getMergedClass();
				merger.merge(leftInterchangeModel.getCollectionFor(mergedClass),
						rightInterchangeModel.getCollectionFor(mergedClass),
						deltaModel, logger);
				topicMergeCompleted().publish(merger);
			}
		} finally {
			LooseContext.pop();
		}
		beforePersistence();
		this.persisterResult = localDeltaPersister.apply(deltaModel);
	}

	protected void beforePersistence() {
	}
}
