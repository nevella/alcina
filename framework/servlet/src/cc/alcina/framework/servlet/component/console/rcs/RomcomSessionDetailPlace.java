package cc.alcina.framework.servlet.component.console.rcs;

import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequencePlace;
import cc.alcina.framework.servlet.component.console.ServerConsolePlace;
import cc.alcina.framework.servlet.component.sequence.adapter.FlightEventSearchDefinition;
import cc.alcina.framework.servlet.logging.FlightEventRecorder;

public class RomcomSessionDetailPlace extends ServerConsolePlace {
	public String path;

	public SequencePlace sequencePlace = new SequencePlace();

	public SequencePlace getSequencePlace() {
		return sequencePlace;
	}

	public RomcomSessionDetailPlace() {
	}

	public RomcomSessionDetailPlace(String path) {
		this.path = path;
		SequencePlace place = sequencePlace;
		place.instanceQuery = FlightEventRecorder.createInstanceQuery(path);
	}

	public static class Tokenizer
			extends ServerConsolePlace.Tokenizer<RomcomSessionDetailPlace> {
		@Override
		protected RomcomSessionDetailPlace getPlace1(String token,
				boolean retry) {
			RomcomSessionDetailPlace place = super.getPlace1(token, retry);
			if (place.sequencePlace.search == null) {
				place.sequencePlace.search = new FlightEventSearchDefinition();
			}
			return place;
		}
	}

	RomcomSessionDetailPlace
			withUpdatedSequencePlace(SequencePlace sequencePlace) {
		RomcomSessionDetailPlace place = new RomcomSessionDetailPlace(path);
		place.sequencePlace = sequencePlace;
		return place;
	}

	@Override
	public String getDescription() {
		// not top-level
		throw new UnsupportedOperationException();
	}
}
