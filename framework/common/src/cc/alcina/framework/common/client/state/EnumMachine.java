package cc.alcina.framework.common.client.state;

import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.common.client.state.MachineEvent.MachineEventImpl;
import cc.alcina.framework.common.client.state.MachineState.MachineStateImpl;
import cc.alcina.framework.common.client.util.CommonUtils;

public abstract class EnumMachine<E extends Enum, M extends MachineModel>
		extends Machine<M> {
	protected Class<E> enumClass;

	protected void initModel() {
		model = (M) new MachineModel();
		model.setDebug(false);
	}

	Map<E, EnumMachineState<E>> statesForElements = new LinkedHashMap<E, EnumMachineState<E>>();

	public void init(Class<E> enumClass) {
		this.enumClass = enumClass;
		initModel();
		registerDefaultStatesEventsAndHandlers();
		E[] constants = enumClass.getEnumConstants();
		MachineState current = MachineState.START;
		for (int i = 0; i < constants.length + 1; i++) {
			MachineState state;
			if (i == constants.length) {
				state = MachineState.END;
			} else {
				E e = constants[i];
				state = new EnumMachineState<E>(e);
				statesForElements.put(e, (EnumMachineState<E>) state);
			}
			MachineEventImpl event = new MachineEventImpl("transition-" + i,
					current, state);
			if (current == MachineState.START) {
				registerTransitionHandler(current, null,
						new SimpleTransitionHandler(event));
			} else {
				registerTransitionHandler(
						current,
						null,
						getTransitionHandlerFor((EnumMachineState<E>) current,
								event));
			}
			current = state;
		}
	}

	public EnumMachine() {
	}

	protected abstract MachineTransitionHandler getTransitionHandlerFor(
			EnumMachineState<E> state, MachineEventImpl event);

	public static class EnumMachineState<E extends Enum> extends
			MachineStateImpl {
		private E enumValue;

		public EnumMachineState(E e) {
			name = CommonUtils.simpleClassName(e.getClass()) + "."
					+ e.toString();
			this.enumValue = e;
		}

		public E getEnumValue() {
			return this.enumValue;
		}
	}

	public EnumMachineState<E> getStateFor(E e) {
		return this.statesForElements.get(e);
	}
}
