package cc.alcina.framework.servlet.component.sequence.branch;

import javax.xml.bind.annotation.XmlType;

import cc.alcina.framework.common.client.domain.search.DomainCriterionHandler;
import cc.alcina.framework.common.client.domain.search.EntityCriteriaGroup;
import cc.alcina.framework.common.client.search.BooleanEnum;
import cc.alcina.framework.common.client.search.BooleanEnumCriterion;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.search.TextCriterion;
import cc.alcina.framework.common.client.serializer.PropertySerialization;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.gwt.client.objecttree.search.packs.BaseEnumCriterionPack.BaseEnumCriterionHandler;

public class BranchingParserNodeCriterion {
	@TypeSerialization(
		properties = @PropertySerialization(
			name = CriteriaGroup.PROPERTY_CRITERIA,
			defaultProperty = true,
			types = {
			//@formatter:off
					TextCriterion.class
					,
					OncePerToplevelToken.class
				//@formatter:on
			}))
	@XmlType(name = "BranchingParserNodeSearchDefinition_CriteriaGroup")
	public static class CriteriaGroup extends EntityCriteriaGroup {
	}

	static class SearchContext {
	}

	abstract static class CriterionHandler<SC extends SearchCriterion>
			extends DomainCriterionHandler<SC> {
		@Override
		public Class<? extends SearchDefinition> handlesSearchDefinition() {
			return BranchingParserNodeSearchDefinition.class;
		}

		BranchingParserNodeSearchDefinition.SearchContext getContext() {
			return BranchingParserNodeSearchDefinition.SearchContext.get();
		}
	}

	@TypeSerialization("oncepertopleveltoken")
	static class OncePerToplevelToken extends BooleanEnumCriterion {
		public static class Handler
				extends CriterionHandler<OncePerToplevelToken> implements
				BaseEnumCriterionHandler<BranchingParserNode, BooleanEnum, OncePerToplevelToken> {
			@Override
			public boolean test(BranchingParserNode node, BooleanEnum value) {
				if (!BooleanEnum.is(value)) {
					return true;
				}
				if (node.isTopLvel()) {
					return getContext().addBranch(node);
				} else {
					return true;
				}
			}
		}
	}
}
