package cc.alcina.framework.gwt.client.dirndl.model.edit;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.gwt.client.dirndl.layout.FragmentNode;

public abstract class DecoratorNode extends FragmentNode {
	/**
	 * This is only called when a chooser is *not* showing, so isValid is
	 * logically correct
	 */
	public void validate() {
		if (!isValid()) {
			DomNode firstNode = domNode().children.firstNode();
			domNode().strip();
			if (firstNode.isText()) {
				// undo split
				firstNode.text().mergeWithAdjacentTexts();
			}
		}
	}

	boolean isValid() {
		// TODO - model as entitylocator field
		return domNode().has("_entity");
	}
}