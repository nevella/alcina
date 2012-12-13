package cc.alcina.framework.common.client.state;

public class SimpleTransitionHandler implements MachineTransitionHandler {

	private final MachineEvent newEvent;

	public SimpleTransitionHandler(
			MachineEvent newEvent) {
		this.newEvent = newEvent;
	}

	@Override
	public void performTransition(MachineModel model) {
		model.getMachine().newEvent( newEvent);
	}
}