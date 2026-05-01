package cc.alcina.framework.servlet.component.sequence.branch;

import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.process.ProcessObserver;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.service.InstanceOracle.Query;
import cc.alcina.framework.common.client.service.InstanceProvider;
import cc.alcina.framework.common.client.service.InstanceQuery;
import cc.alcina.framework.common.client.traversal.layer.BranchingParser;
import cc.alcina.framework.common.client.traversal.layer.BranchingParser.Branch;
import cc.alcina.framework.common.client.traversal.layer.BranchingParser.BranchNode;
import cc.alcina.framework.common.client.traversal.layer.BranchingParser.Exit;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.Sequence;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceSearchDefinition;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.sequence.branch.BranchingParserNode.GroupType;
import cc.alcina.framework.servlet.component.sequence.branch.BranchingParserNode.MatchType;

@TypeSerialization("brpns")
public class BranchingParserNodeSequence
		extends Sequence.Abstract.Passthrough<BranchingParserNode> {
	@Override
	public SequenceSearchDefinition getDefaultSearchDefinition() {
		return new BranchingParserNodeSearchDefinition();
	}

	@Display.AllProperties
	static class BranchingParserNodeView extends Model.Fields
			implements Model.MultiNodeModel {
		@Directed(className = "path")
		String path;

		@Directed(className = "measure")
		String measure;

		@Directed(className = "match")
		String match;

		BranchingParserNodeView(BranchingParserNode node) {
			this.path = node.getPath();
			this.measure = node.getMeasure();
			this.match = node.getMatch();
		}
	}

	@Display.AllProperties
	static class BranchingParserNodeDetail extends Model.Fields
			implements Model.MultiNodeModel {
		@Directed(className = "path")
		String path;

		@Directed(className = "measure")
		String measure;

		@Directed(className = "match")
		String match;

		@Directed(className = "match")
		String result;

		BranchingParserNodeDetail(BranchingParserNode node) {
			this.path = node.getPath();
			this.measure = node.getMeasure();
			this.match = node.getMatch();
			if (node.branchNode.match != null) {
				result = node.branchNode.branch.toResult().toStructuredString();
			}
		}
	}

	@Override
	public ModelTransform<BranchingParserNode, ? extends Model>
			getRowTransform() {
		return BranchingParserNodeView::new;
	}

	@Override
	public ModelTransform<BranchingParserNode, ? extends Model>
			getDetailTransform() {
		return BranchingParserNodeDetail::new;
	}

	public static InstanceQuery createInstanceQuery() {
		return new InstanceQuery().withType(BranchingParserNodeSequence.class);
	}

	public static class InstanceProviderImpl
			implements InstanceProvider<BranchingParserNodeSequence> {
		@Override
		public BranchingParserNodeSequence provide(
				Query<BranchingParserNodeSequence> query) throws Exception {
			List<BranchingParserNode> nodes = getNodes();
			return new BranchingParserNodeSequence().withElements(nodes);
		}

		List<BranchingParserNode> getNodes() {
			List<BranchingParserNode> list = lastSequence.stream()
					.map(BranchingParserNode::new).toList();
			Map<Branch, BranchingParserNode> branchBranchNode = list.stream()
					.collect(AlcinaCollectors
							.toKeyMap(BranchingParserNode::getBranch));
			list.forEach(bpn -> {
				bpn.groupType = GroupType.GROUP;
				if (bpn.branchNode.match != null
						|| !bpn.branchNode.branch.group.isComplex()) {
					bpn.groupType = GroupType.LEAF_OR_MATCH;
				}
				if (bpn.branchNode.match != null) {
					bpn.matchType = MatchType.MATCH;
					BranchingParser.Result result = bpn.branchNode.branch
							.toResult();
					result.stream().map(e -> e.getLastBranch())
							.filter(branch -> branch != bpn.branchNode.branch)
							.forEach(branch -> branchBranchNode.get(
									branch).matchType = MatchType.DESCENDANT_MATCH);
				}
			});
			return list;
		}
	}

	static List<BranchNode> lastSequence = List.of();

	public static void observe() {
		BranchingParser.SequenceObserver.observe = true;
		new SequenceObserver().bind();
	}

	static class SequenceObserver
			implements ProcessObserver<BranchingParser.Exit> {
		@Override
		public void topicPublished(Exit message) {
			lastSequence = message.getNodes();
		}
	}
}
