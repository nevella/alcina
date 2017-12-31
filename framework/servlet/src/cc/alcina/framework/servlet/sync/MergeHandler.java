package cc.alcina.framework.servlet.sync;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.sync.SyncInterchangeModel;

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

	public void run(Logger logger) throws Exception {
		for (SyncMerger merger : syncMergers) {
			merger.merge(
					leftInterchangeModel
							.getCollectionFor(merger.getMergedClass()),
					rightInterchangeModel.getCollectionFor(
							merger.getMergedClass()),
					deltaModel, logger);
		}
		beforePersistence();
		this.persisterResult = localDeltaPersister.apply(deltaModel);
	}

	protected void beforePersistence() {
	}
}
