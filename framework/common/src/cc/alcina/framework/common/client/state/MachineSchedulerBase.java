package cc.alcina.framework.common.client.state;


public abstract class MachineSchedulerBase implements MachineScheduler {

	protected void setModelState(MachineState newState,MachineModel model){
		model.setState(newState);
	}
	protected void setModelEvent(MachineEvent newEvent,MachineModel model){
		model.setEvent(newEvent);
	}
	@Override
	public abstract void newEvent(MachineEvent newEvent,MachineModel model);

	@Override
	public abstract void newState(MachineState newState,MachineModel model);
}
