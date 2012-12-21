package cc.alcina.framework.common.client.state;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.state.MachineEvent.MachineEventImpl;

public interface MachineState {
	public String name();

	public Map<MachineEvent, MachineState> getEmittedEvents();

	public class MachineStateImpl implements MachineState {
		protected String name;

		protected Map<MachineEvent, MachineState> emittedEvents = new LinkedHashMap<MachineEvent, MachineState>();
public MachineStateImpl() {
}
		public MachineStateImpl(String name) {
			this.name = name;
		}

		public String name() {
			return this.name;
		}

		@Override
		public Map<MachineEvent, MachineState> getEmittedEvents() {
			return this.emittedEvents;
		}
	}
	public static final MachineStateImpl START = new MachineStateImpl("start");
	public static final MachineStateImpl END = new MachineStateImpl("end");
	public static final MachineStateImpl ERR = new MachineStateImpl("err");
}
