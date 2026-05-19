package cc.alcina.framework.servlet.component.console.rcs;

import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequencePlace;
import cc.alcina.framework.servlet.component.console.ServerConsolePlace;
import cc.alcina.framework.servlet.component.console.rcs.RomcomSessionSearchDefinition.Preset;

public class RomcomSessionPlace extends ServerConsolePlace {
	public SequencePlace sequencePlace = new SequencePlace();

	@Override
	public RomcomSessionPlace copy() {
		return (RomcomSessionPlace) super.copy();
	}

	public RomcomSessionPlace() {
		{
			SequencePlace place = sequencePlace;
			place.instanceQuery = RomcomSessionSequence.createInstanceQuery();
		}
	}

	public RomcomSessionPlace(SequencePlace sequencePlace) {
		this.sequencePlace = sequencePlace;
	}

	public static class Tokenizer
			extends ServerConsolePlace.Tokenizer<RomcomSessionPlace> {
		@Override
		protected RomcomSessionPlace getPlace1(String token, boolean retry) {
			RomcomSessionPlace place = super.getPlace1(token, retry);
			if (place.sequencePlace.search == null) {
				place.sequencePlace.search = Preset.All.getDefinition();
			}
			return place;
		}
	}

	@Override
	public String getDescription() {
		return "List + manipulate romcom session";
	}
}
