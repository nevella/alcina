package cc.alcina.framework.servlet.transform.fragment;

import cc.alcina.framework.common.client.traversal.AbstractSelection;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FragmentDocumentNode;
import cc.alcina.framework.servlet.transform.fragment.output.MeasureNode;

/*
 * These selections are not reused (by subsequent selections), but their changes
 * to the parent MeasureNode FragmentDocumentNode value are.
 *
 * The process parent MeasureNode is - to a degree - mutable - it contains the
 * history of FragmentDocumentNode changes corresponding to the input Measure
 */
public class OutputMutationSelection extends
		AbstractSelection<FragmentDocumentNode> implements Selection.Output {
	public OutputMutationSelection(MeasureNode parent,
			FragmentDocumentNode value) {
		super(parent, value);
		parent.set(value);
	}
}