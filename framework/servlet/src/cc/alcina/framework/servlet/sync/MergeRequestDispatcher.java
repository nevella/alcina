package cc.alcina.framework.servlet.sync;


public interface MergeRequestDispatcher<I extends SyncInterchangeModel, D extends SyncDeltaModel> {
	public D dispatch(I interchangeModel) throws Exception;
}
