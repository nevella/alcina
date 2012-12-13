package cc.alcina.framework.common.client.state;

public interface MachineListener<M extends MachineModel> {
	public void beforeAction(M model);
	public void afterAction(M model);
}
