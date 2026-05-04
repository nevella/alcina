package cc.alcina.framework.servlet.component.console.rcs;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.search.TextCriterion;
import cc.alcina.framework.common.client.serializer.PropertySerialization;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.Sequence;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceSearchDefinition;

@TypeSerialization(
	value = "romcomsession",
	properties = { @PropertySerialization(
		name = SearchDefinition.PROPERTY_CRITERIA_GROUPS,
		types = RomcomSessionCriterion.CriteriaGroup.class,
		defaultProperty = true) })
public class RomcomSessionSearchDefinition extends SequenceSearchDefinition {
	@TypeSerialization("ingestassistsearchdefinition")
	public static class Parameter extends
			SequenceSearchDefinition.BaseParameter<RomcomSessionSearchDefinition> {
	}

	@Override
	public Class<? extends Sequence> sequenceClass() {
		return RomcomSessionSequence.class;
	}

	@Override
	public Class<? extends Bindable> queriedBindableClass() {
		return RomcomSessionEntry.class;
	}

	public RomcomSessionSearchDefinition() {
		init();
	}

	@Override
	protected void init() {
		super.init();
		new TextCriterion().withValue(" ").addToSoleCriteriaGroup(this);
	}
}
