package cc.alcina.framework.servlet.component.console.rcs;

import cc.alcina.framework.common.client.search.BooleanEnum;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequencePlace;
import cc.alcina.framework.servlet.component.console.ServerConsolePlace;
import cc.alcina.framework.servlet.component.console.rcs.RomcomSessionCriterion.ActiveCriterion;

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
			RomcomSessionSearchDefinition def = new RomcomSessionSearchDefinition();
			new ActiveCriterion().withValue(BooleanEnum.TRUE)
					.addToSoleCriteriaGroup(def);
			place.search = def;
		}
	}

	public RomcomSessionPlace(SequencePlace sequencePlace) {
		this.sequencePlace = sequencePlace;
	}

	public static class Tokenizer
			extends ServerConsolePlace.Tokenizer<RomcomSessionPlace> {
	}

	@Override
	public String getDescription() {
		return "List + manipulate romcom session";
	}
}
