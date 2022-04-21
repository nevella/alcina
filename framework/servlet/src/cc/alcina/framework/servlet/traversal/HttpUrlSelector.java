package cc.alcina.framework.servlet.traversal;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.servlet.traversal.DomSelection.Document;

public abstract class HttpUrlSelector<I extends UrlSelection, O extends Selection>
		implements Selector<I, O> {
	public static class Dom<I extends UrlSelection, O extends DomSelection.Document>
			extends HttpUrlSelector<I, O> {
		@Override
		public void process(SelectionTraversal traversal, I selection) {
			DomDocument domDocument = ResourceUtilities
					.loadXmlDocFromUrl(selection.get());
			Document<DomDocument> out = new DomSelection.Document<DomDocument>(
					selection, domDocument,
					domDocument.getDocumentElementNode().name());
			traversal.select(out);
		}
	}
}
