package cc.alcina.framework.servlet.component.sequence.branch;

import java.util.Set;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.domain.DomainFilter;
import cc.alcina.framework.common.client.domain.search.DomainDefinitionHandler;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.search.TextCriterion;
import cc.alcina.framework.common.client.serializer.PropertySerialization;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.traversal.layer.BranchToken.Group;
import cc.alcina.framework.common.client.util.AlcinaCollections;
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
	static class SearchContext {
		static final LooseContext.Key<SearchContext> CONTEXT_SEACH_CONTEXT = LooseContext
				.key(SearchContext.class, "CONTEXT_SEACH_CONTEXT");

		static void register() {
			CONTEXT_SEACH_CONTEXT.set(new SearchContext());
		}

		static SearchContext get() {
			return CONTEXT_SEACH_CONTEXT.getTyped();
		}

		Set<Group> seenTopLevelGroups = AlcinaCollections.newHashSet();

		boolean addBranch(BranchingParserNode node) {
			return seenTopLevelGroups.add(node.branchNode.branch.group);
		}
	}

	static class DomainDefinitionHandlerImpl extends
			DomainDefinitionHandler<BranchingParserNodeSearchDefinition> {
		@Override
		public DomainFilter getFilter(BranchingParserNodeSearchDefinition sc) {
			SearchContext.register();
			return new DomainFilter(o -> true);
		}

		@Override
		public Class<BranchingParserNodeSearchDefinition>
				handlesSearchDefinition() {
			return BranchingParserNodeSearchDefinition.class;
		}
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
