package cc.alcina.framework.common.client.state;

public interface MachineTransitionHandler<M extends MachineModel> {
	public void performTransition(M model);
}
