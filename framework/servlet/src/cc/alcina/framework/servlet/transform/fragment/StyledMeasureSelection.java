package cc.alcina.framework.servlet.transform.fragment;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;

public class StyledMeasureSelection extends MeasureSelection
		implements HasStyledNode<Measure> {
	public StyledMeasureSelection(Selection parent, Measure measure) {
		super(parent, measure);
	}

	/*
	 * return the styledNode containing the start of the measure
	 */
	@Override
	public StyledNode styledNode() {
		DomNode domNode = get().start.getContainingNode();
		StyledDocument styledDocument = StyledDocument.ancestorDocument(this);
		return styledDocument.styledNode(domNode);
	}

	/** Models a container, such as a TABLE or TR that can't contain indents */
	public interface NonIndentContainer {
	}
}