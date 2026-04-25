package cc.alcina.framework.servlet.component.sequence.branch;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.search.TextCriterion;
import cc.alcina.framework.common.client.serializer.PropertySerialization;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.Sequence;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceSearchDefinition;

@TypeSerialization(
	value = "bpnsd",
	properties = { @PropertySerialization(
		name = SearchDefinition.PROPERTY_CRITERIA_GROUPS,
		types = BranchingParserNodeCriterion.CriteriaGroup.class,
		defaultProperty = true) })
public class BranchingParserNodeSearchDefinition
		extends SequenceSearchDefinition {
	@TypeSerialization("bpnsdparam")
	public static class Parameter extends
			SequenceSearchDefinition.BaseParameter<BranchingParserNodeSearchDefinition> {
	}

	@Override
	public Class<? extends Sequence> sequenceClass() {
		return BranchingParserNodeSequence.class;
	}

	@Override
	public Class<? extends Bindable> queriedBindableClass() {
		return BranchingParserNode.class;
	}

	public BranchingParserNodeSearchDefinition() {
		init();
	}

	@Override
	protected void init() {
		super.init();
		// blank txt criterion basically
		new TextCriterion().withValue(" ").addToSoleCriteriaGroup(this);
	}
}
