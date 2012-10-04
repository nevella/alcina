package cc.alcina.framework.gwt.client.logic.state;

public interface MachineListener<M extends MachineModel> {
	public void beforeAction(M model);
	public void afterAction(M model);
}
