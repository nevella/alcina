package cc.alcina.framework.common.client.traversal.layer.diff;

import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.layer.diff.MeasureDiff.Peer;
import cc.alcina.framework.common.client.traversal.layer.diff.MergeInputNode.Word;
import cc.alcina.framework.common.client.traversal.layer.diff.RootLayer.RootSelection;

/**
 * Create a set of input nodes, a superset of the union of the dom trees.
 * Additional nodes wrap the per-word (in dom text nodes) measures which are the
 * basis of the diff
 */
/*
 * @formatter:off
 
 so:
 <div>my blah</div>
 
 --->

 div
	#text
		#text[my]
		#text[blah]
 
 * @formatter:on
 */
class GenerateInputNodes extends Layer<RootSelection> {
	Peer peer;

	RootSelection rootSelection;

	GenerateInputNodes() {
	}

	@Override
	protected void onBeforeIteration() {
		super.onBeforeIteration();
		peer = state.traversalContext(MeasureDiff.Peer.class);
	}

	@Override
	public void process(RootSelection selection) throws Exception {
		this.rootSelection = selection;
		create(selection.get().attributes.left, true);
		create(selection.get().attributes.right, false);
		// noop so far
	}

	Map<DomNode, MergeInputNode> domNodeInputNode = new LinkedHashMap<>();

	Map<Measure, MergeInputNode> measureInputNode = new LinkedHashMap<>();

	void create(DomNode root, boolean left) {
		root.stream().flatMap(peer::createMergeMeasures)
				.map(measure -> MergeInputNode
						.create(getInputParent(root, measure), measure, left))
				.forEach(mergeNode -> {
					domNodeInputNode.putIfAbsent(
							mergeNode.get().containingNode(), mergeNode);
					measureInputNode.put(mergeNode.get(), mergeNode);
					select(mergeNode);
				});
	}

	Selection getInputParent(DomNode root, Measure measure) {
		if (measure.containingNode() == root) {
			return rootSelection;
		} else {
			return domNodeInputNode.get(measure.containingNode().parent());
		}
	}
}
