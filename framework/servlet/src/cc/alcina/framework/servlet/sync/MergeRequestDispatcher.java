package cc.alcina.framework.servlet.sync;

import cc.alcina.framework.common.client.sync.SyncInterchangeModel;

/**
 * Sends a merge request to a remote recipient (if merging within a jvm, just do
 * this imperatively)
 * 
 * 
 *
 * @param <I>
 * @param <D>
 */
public interface MergeRequestDispatcher<I extends SyncInterchangeModel, D extends SyncDeltaModel> {
	public D dispatch(I interchangeModel) throws Exception;
}
