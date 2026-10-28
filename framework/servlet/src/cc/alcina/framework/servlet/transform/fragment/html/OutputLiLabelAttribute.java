package cc.alcina.framework.servlet.transform.fragment.html;

import com.google.common.base.Preconditions;

import cc.alcina.framework.servlet.transform.fragment.output.MeasureNode;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.traversal.Layer;

/*
 * This class copies the content of an <inline class="li-label">abc</inline> to
 * the <li> parent
 */
public class OutputLiLabelAttribute extends Layer<MeasureNode> {
	@Override
	public void process(MeasureNode selection) {
		DomNode outputNode = selection.get().domNode();
		if (outputNode.tagAndClassIs("inline", "li-label")) {
			DomNode liAncestor = null;
			DomNode cursor = outputNode.parent();
			if (cursor.tagIs("li")) {
				liAncestor = cursor;
			} else {
				cursor = cursor.parent();
				if (cursor.tagIs("li")) {
					liAncestor = cursor;
				}
			}
			Preconditions.checkState(liAncestor != null);
			liAncestor.setAttr("label", outputNode.ntc());
		}
	}
}