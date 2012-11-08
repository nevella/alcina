package cc.alcina.framework.gwt.client.logic.state;

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