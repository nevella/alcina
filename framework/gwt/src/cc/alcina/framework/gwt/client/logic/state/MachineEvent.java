package cc.alcina.framework.gwt.client.logic.state;

public interface MachineEvent {
	public String name();

	public interface MachineEventWithEndpoints extends MachineEvent {
		public MachineState getFrom();

		public MachineState getTo();
	}

	public class MachineEventImpl implements MachineEventWithEndpoints {
		private String name;

		private MachineState from;

		private MachineState to;

		public MachineEventImpl(String name) {
			this.name = name;
		}

		public MachineEventImpl(String name, MachineState from, MachineState to) {
			this.name = name;
			this.from = from;
			this.to = to;
		}

		public String name() {
			return this.name;
		}

		public MachineState getFrom() {
			return this.from;
		}

		public MachineState getTo() {
			return this.to;
		}
	}

	public static final MachineEvent CANCEL = new MachineEventImpl("cancel");
	public static final MachineEvent RUNTIME_EXCEPTION = new MachineEventImpl("runtime-exception");
}
