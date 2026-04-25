package cc.alcina.framework.servlet.component.sequence.branch;

import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.process.ProcessObserver;
import cc.alcina.framework.common.client.service.InstanceOracle.Query;
import cc.alcina.framework.common.client.service.InstanceProvider;
import cc.alcina.framework.common.client.service.InstanceQuery;
import cc.alcina.framework.common.client.traversal.layer.BranchingParser;
import cc.alcina.framework.common.client.traversal.layer.BranchingParser.BranchNode;
import cc.alcina.framework.common.client.traversal.layer.BranchingParser.Exit;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.Sequence;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceSearchDefinition;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public class BranchingParserNodeSequence
		extends Sequence.Abstract<BranchingParserNode> {
	@Override
	public SequenceSearchDefinition getDefaultSearchDefinition() {
		return new BranchingParserNodeSearchDefinition();
	}

	@Display.AllProperties
	static class BranchingParserNodeView extends Model.Fields
			implements Model.MultiNodeModel {
		String path;

		String match;

		BranchingParserNodeView(BranchingParserNode node) {
			this.path = node.getPath();
			this.match = node.getMatch();
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
		return BranchingParserNodeView::new;
	}

	public static InstanceQuery
			createInstanceQuery(BranchingParserNodeSearchDefinition def) {
		return new InstanceQuery().withType(BranchingParserNodeSequence.class)
				.addParameters(
						new BranchingParserNodeSearchDefinition.Parameter()
								.withValue(def));
	}

	@InstanceProvider.Parameter(BranchingParserNodeSearchDefinition.Parameter.class)
	public static class InstanceProviderImpl
			implements InstanceProvider<BranchingParserNodeSequence> {
		@Override
		public BranchingParserNodeSequence provide(
				Query<BranchingParserNodeSequence> query) throws Exception {
			BranchingParserNodeSearchDefinition.Parameter parameter = query
					.typedParameter(
							BranchingParserNodeSearchDefinition.Parameter.class);
			List<BranchingParserNode> sessions = getNodes(parameter.getValue());
			return new BranchingParserNodeSequence().withElements(sessions);
		}

		@Override
		public boolean isOneOff() {
			return true;
		}

		List<BranchingParserNode>
				getNodes(BranchingParserNodeSearchDefinition def) {
			return lastSequence.stream().map(BranchingParserNode::new).toList();
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
