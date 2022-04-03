package cc.alcina.framework.servlet.traversal;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.entity.ResourceUtilities;

public abstract class HttpSelector implements Selector {
	private DomDocument document;

	public abstract String getUrl();

	@Override
	public void process(SelectionTraversal selectionTraversal,
			Selection selection) {
		ResourceUtilities.loadXmlDocFromUrl(getUrl());
		// sketch: l
	}
}
