package cc.alcina.framework.servlet.sync;

import cc.alcina.framework.common.client.sync.SyncInterchangeModel;


public interface MergeRequestDispatcher<I extends SyncInterchangeModel, D extends SyncDeltaModel> {
	public D dispatch(I interchangeModel) throws Exception;
}
