package cc.alcina.framework.servlet.sync;

import java.util.ArrayList;
import java.util.List;


public abstract class MergeHandler<I extends SyncInterchangeModel, D extends SyncDeltaModel> {
	protected I leftInterchangeModel;

	protected I rightInterchangeModel;

	protected FlatDeltaPersister localDeltaPersister;

	protected List<SyncMerger> syncMergers = new ArrayList<SyncMerger>();

	public D deltaModel;

	public FlatDeltaPersisterResult persisterResult;

	public void run() throws Exception {
		for (SyncMerger merger : syncMergers) {
			merger.merge(leftInterchangeModel.getCollectionFor(merger
					.getMergedClass()), rightInterchangeModel
					.getCollectionFor(merger.getMergedClass()), deltaModel);
		}
		this.persisterResult = localDeltaPersister.apply(deltaModel);
	}
}
