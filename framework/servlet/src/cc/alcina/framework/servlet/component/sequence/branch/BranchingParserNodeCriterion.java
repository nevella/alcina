package cc.alcina.framework.servlet.component.sequence.branch;

import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlType;

import cc.alcina.framework.common.client.domain.DomainFilter;
import cc.alcina.framework.common.client.domain.search.DomainCriterionHandler;
import cc.alcina.framework.common.client.domain.search.EntityCriteriaGroup;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.search.BaseEnumCriterion;
import cc.alcina.framework.common.client.search.BooleanEnum;
import cc.alcina.framework.common.client.search.BooleanEnumCriterion;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.search.TextCriterion;
import cc.alcina.framework.common.client.serializer.PropertySerialization;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.traversal.layer.BranchingParser.Branch;
import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.common.client.util.TextUtils;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;
import cc.alcina.framework.gwt.client.objecttree.search.packs.BaseEnumCriterionPack.BaseEnumCriterionHandler;
import cc.alcina.framework.gwt.client.objecttree.search.packs.SearchUtils;
import cc.alcina.framework.servlet.component.sequence.branch.BranchingParserNode.GroupType;
import cc.alcina.framework.servlet.component.sequence.branch.BranchingParserNode.MatchType;

public class BranchingParserNodeCriterion {
	@TypeSerialization(
		properties = @PropertySerialization(
			name = CriteriaGroup.PROPERTY_CRITERIA,
			defaultProperty = true,
			types = {
			//@formatter:off
					TextCriterion.class,
					OncePerToplevelToken.class,
					MatchTypeCriterion.class,
					GroupTypeCriterion.class,
					NameDepthCriterion.class,
					BranchMinDepthCriterion.class
				//@formatter:on
			}))
	@XmlType(name = "BranchingParserNodeSearchDefinition_CriteriaGroup")
	public static class CriteriaGroup extends EntityCriteriaGroup {
	}

	static class SearchContext {
	}

	abstract static class CriterionHandler<SC extends SearchCriterion> extends
			DomainCriterionHandler<SC> implements Registration.AllSubtypes {
		@Override
		public Class<? extends SearchDefinition> handlesSearchDefinition() {
			return BranchingParserNodeSearchDefinition.class;
		}

		BranchingParserNodeSearchDefinition.SearchContext getContext() {
			return BranchingParserNodeSearchDefinition.SearchContext.get();
		}
	}

	static class TextCriterionHandler extends CriterionHandler<TextCriterion> {
		@Override
		public DomainFilter getFilter(TextCriterion sc) {
			String text = TextUtils.normalisedLcKey(sc.getValue());
			if (text.isEmpty()) {
				return null;
			}
			return new DomainFilter(new Predicate<BranchingParserNode>() {
				Pattern p = Pattern.compile(text, Pattern.CASE_INSENSITIVE);

				@Override
				public boolean test(BranchingParserNode o) {
					return p.matcher(o.provideStringRepresentation()).find();
				}
			}).invertIf(sc
					.getOperator() == StandardSearchOperator.DOES_NOT_CONTAIN);
		}
	}

	@TypeSerialization("oncepertopleveltoken")
	static class OncePerToplevelToken extends BooleanEnumCriterion {
		static class Handler extends CriterionHandler<OncePerToplevelToken>
				implements
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

	@TypeSerialization("match")
	static class MatchTypeCriterion extends BaseEnumCriterion<MatchType> {
		static class Handler extends CriterionHandler<MatchTypeCriterion>
				implements
				BaseEnumCriterionHandler<BranchingParserNode, MatchType, MatchTypeCriterion> {
			static Branch debugBranch = null;

			@Override
			public boolean test(BranchingParserNode node, MatchType value) {
				if (value == null) {
					return true;
				}
				if (node.matchType == null) {
					return false;
				}
				switch (node.matchType) {
				case MATCH:
					return true;
				case DESCENDANT_MATCH:
					return value == node.matchType;
				default:
					throw new UnsupportedOperationException();
				}
			}
		}
	}

	enum Depth implements HasDisplayName {
		_1, _2, _3, _4, _5, _6, _7, _8, _9, _10, _11, _12;

		@Override
		public String displayName() {
			return toString().substring(1);
		}
	}

	@TypeSerialization("namedepth")
	static class NameDepthCriterion extends BaseEnumCriterion<Depth> {
		static class Handler extends CriterionHandler<NameDepthCriterion>
				implements
				BaseEnumCriterionHandler<BranchingParserNode, Depth, NameDepthCriterion> {
			@Override
			public boolean test(BranchingParserNode node, Depth value) {
				if (value == null) {
					return true;
				}
				return node.getNameDepth() <= (value.ordinal());
			}
		}
	}

	@TypeSerialization("branchmindepth")
	static class BranchMinDepthCriterion extends BaseEnumCriterion<Depth> {
		static class Handler extends CriterionHandler<BranchMinDepthCriterion>
				implements
				BaseEnumCriterionHandler<BranchingParserNode, Depth, BranchMinDepthCriterion> {
			@Override
			public boolean test(BranchingParserNode node, Depth value) {
				if (value == null) {
					return true;
				}
				return node.containingBranchMaxLength >= (value.ordinal());
			}
		}
	}

	@TypeSerialization("group")
	static class GroupTypeCriterion extends BaseEnumCriterion<GroupType> {
		static class Handler extends CriterionHandler<GroupTypeCriterion>
				implements
				BaseEnumCriterionHandler<BranchingParserNode, GroupType, GroupTypeCriterion> {
			static Branch debugBranch = null;

			@Override
			public boolean test(BranchingParserNode node, GroupType value) {
				if (value == null) {
					return true;
				}
				if (value == GroupType.NAMED) {
					return node.branchNode.branch.group.isNamed()
							|| node.branchNode.match != null;
				}
				return value == node.groupType;
			}
		}
	}
}
