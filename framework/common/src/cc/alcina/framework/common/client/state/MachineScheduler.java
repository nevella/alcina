package cc.alcina.framework.common.client.state;

public interface MachineScheduler {
	public abstract void newEvent(MachineEvent newEvent, MachineModel model);

	public abstract void newState(MachineState newState, MachineModel model);
	
	public void scheduleDeferred(Runnable runnable);
}