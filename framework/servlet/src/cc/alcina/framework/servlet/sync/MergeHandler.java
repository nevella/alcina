package cc.alcina.framework.servlet.sync;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.sync.SyncInterchangeModel;


public abstract class MergeHandler<I extends SyncInterchangeModel, D extends SyncDeltaModel> {
	protected I leftInterchangeModel;

	protected I rightInterchangeModel;

	protected FlatDeltaPersister localDeltaPersister;

	protected List<SyncMerger> syncMergers = new ArrayList<SyncMerger>();

	public D deltaModel;

	public FlatDeltaPersisterResult persisterResult;

	public void run(Logger logger) throws Exception {
		for (SyncMerger merger : syncMergers) {
			merger.merge(leftInterchangeModel.getCollectionFor(merger
					.getMergedClass()), rightInterchangeModel
					.getCollectionFor(merger.getMergedClass()), deltaModel,logger);
		}
		this.persisterResult = localDeltaPersister.apply(deltaModel);
	}
}
