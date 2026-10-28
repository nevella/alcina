package cc.alcina.framework.servlet.transform.fragment.output;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.dom.Location.Range;
import cc.alcina.framework.common.client.traversal.AbstractSelection;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FragmentDocumentNode;

/**
 * <p>
 * These selections correspond to nodes of the output (transformed-to) document
 * - so if you want to do a further transformation of the *output* document,
 * traversing the MeasureNode selections is one way to do it
 * 
 * <p>
 * To go from a rendered FragmentDocumentNode (FragmentDocument/FragmentModel)
 * back to the MeasureNode (Traversal/Selection), see {@link ReverseLookup}
 */
public class MeasureNode extends AbstractSelection<FragmentDocumentNode>
		implements HasDisplayName, Selection.ImmutableInput, Selection.Output,
		Selection.WithRange<FragmentDocumentNode> {
	public static final transient String CONTEXT_IN_DISPLAY_NAME = MeasureNode.class
			.getName() + ".CONTEXT_IN_DISPLAY_NAME";

	List<MeasureNode> treeChildren = new ArrayList<>();

	MeasureNode treeParent;

	MeasureSelection measureSelection;

	List<FragmentDocumentNode> nodeHistory = new ArrayList<>();

	MeasureNode(MeasureSelection parent, MeasureNode treeParent,
			FragmentDocumentNode rendered) {
		super(parent, rendered);
		this.treeParent = treeParent;
		this.measureSelection = parent;
		if (treeParent != null) {
			treeParent.treeChildren.add(this);
		}
		nodeHistory.add(get());
	}

	@Override
	public void set(FragmentDocumentNode value) {
		FragmentDocumentNode old_value = get();
		super.set(value);
		if (value != old_value) {
			nodeHistory.add(value);
		}
	}

	@Override
	public String toString() {
		return Ax.format("%s :: %s", super.toString(), get());
	}

	List<MeasureNode> getTreeChildren() {
		return this.treeChildren;
	}

	@Override
	public String displayName() {
		try {
			LooseContext.pushWithTrue(CONTEXT_IN_DISPLAY_NAME);
			return Ax.ntrim(Ax.trim(toString(), 250));
		} finally {
			LooseContext.pop();
		}
	}

	@Override
	public Range provideRange() {
		return get().provideIsBound() ? get().domNode().asRange() : null;
	}

	public static class ReverseLookup {
		Map<FragmentDocumentNode, MeasureNode> nodeMeasureNode;

		/**
		 *
		 * @param selectionTraversal
		 * @param fragmentDocumentNode
		 * @return the corresponding source node, or null if no exact
		 *         correspondent exists
		 */
		public MeasureNode getMeasureNode(SelectionTraversal selectionTraversal,
				FragmentDocumentNode fragmentDocumentNode) {
			if (nodeMeasureNode == null) {
				nodeMeasureNode = selectionTraversal.selections()
						.get(MeasureNode.class).stream()
						.collect(AlcinaCollectors.toKeyMap(MeasureNode::get));
			}
			return nodeMeasureNode.get(fragmentDocumentNode);
		}

		/**
		 * 
		 * @param selectionTraversal
		 * @param fragmentDocumentNode
		 * @return the smallest source node containing the fragmentDocumentNode
		 *         - equivalent to 'the lowest ancestor-or-self of
		 *         fragmentDocumentNode with a corresponding source node'
		 */
		public MeasureNode getContainingMeasureNode(
				SelectionTraversal selectionTraversal,
				FragmentDocumentNode fragmentDocumentNode) {
			FragmentDocumentNode cursor = fragmentDocumentNode;
			while (cursor != null) {
				MeasureNode correspondingSourceNode = getMeasureNode(
						selectionTraversal, cursor);
				if (correspondingSourceNode != null) {
					return correspondingSourceNode;
				} else {
					cursor = (FragmentDocumentNode) cursor.parent();
				}
			}
			throw new IllegalStateException();
		}
	}
}