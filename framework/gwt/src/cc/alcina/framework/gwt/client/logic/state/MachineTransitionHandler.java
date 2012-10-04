package cc.alcina.framework.gwt.client.logic.state;

public interface MachineTransitionHandler<M extends MachineModel> {
	public void performTransition(M model);
}
