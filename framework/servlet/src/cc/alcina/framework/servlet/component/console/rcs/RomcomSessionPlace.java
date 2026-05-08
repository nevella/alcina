package cc.alcina.framework.servlet.component.console.rcs;

import cc.alcina.framework.common.client.search.BooleanEnum;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequencePlace;
import cc.alcina.framework.servlet.component.console.ServerConsolePlace;
import cc.alcina.framework.servlet.component.console.rcs.RomcomSessionCriterion.ActiveCriterion;

public class RomcomSessionPlace extends ServerConsolePlace {
	public SequencePlace activePlace = new SequencePlace();

	public SequencePlace inactivePlace = new SequencePlace();

	@Override
	public RomcomSessionPlace copy() {
		return (RomcomSessionPlace) super.copy();
	}

	public RomcomSessionPlace() {
		{
			SequencePlace place = activePlace;
			place.instanceQuery = RomcomSessionSequence.createInstanceQuery();
			RomcomSessionSearchDefinition def = new RomcomSessionSearchDefinition();
			new ActiveCriterion().withValue(BooleanEnum.TRUE)
					.addToSoleCriteriaGroup(def);
			place.search = def;
		}
		{
			SequencePlace place = inactivePlace;
			place.instanceQuery = RomcomSessionSequence.createInstanceQuery();
			RomcomSessionSearchDefinition def = new RomcomSessionSearchDefinition();
			new ActiveCriterion().withValue(BooleanEnum.FALSE)
					.addToSoleCriteriaGroup(def);
			place.search = def;
		}
	}

	public RomcomSessionPlace(SequencePlace activePlace,
			SequencePlace inactivePlace) {
		this.activePlace = activePlace;
		this.inactivePlace = inactivePlace;
	}

	public static class Tokenizer
			extends ServerConsolePlace.Tokenizer<RomcomSessionPlace> {
	}

	@Override
	public String getDescription() {
		return "List + manipulate romcom session";
	}
}
