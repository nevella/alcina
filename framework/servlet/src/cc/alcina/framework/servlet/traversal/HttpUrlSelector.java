package cc.alcina.framework.servlet.traversal;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.traversal.DomSelection;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.Selector;
import cc.alcina.framework.common.client.traversal.UrlSelection;
import cc.alcina.framework.common.client.traversal.DomSelection.Document;
import cc.alcina.framework.entity.ResourceUtilities;

public abstract class HttpUrlSelector<I extends UrlSelection, O extends Selection>
		implements Selector<I, O> {
	public static class Dom<I extends UrlSelection, O extends DomSelection.Document>
			extends HttpUrlSelector<I, O> {
		@Override
		public void process(SelectionTraversal traversal, I selection) {
			DomDocument domDocument = ResourceUtilities
					.loadXmlDocFromUrl(selection.get());
			Document<DomDocument> out = new DomSelection.Document<DomDocument>(
					selection, domDocument);
			traversal.select(out);
		}
	}
}
