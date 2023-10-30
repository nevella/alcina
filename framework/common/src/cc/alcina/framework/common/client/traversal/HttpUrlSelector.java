package cc.alcina.framework.common.client.traversal;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomEnvironment;
import cc.alcina.framework.common.client.traversal.DomSelection.Document;

public abstract class HttpUrlSelector<I extends AbstractUrlSelection, O extends Selection>
		implements Selector<I, O> {
	public static class Dom<I extends AbstractUrlSelection, O extends DomSelection.Document>
			extends HttpUrlSelector<I, O> {
		@Override
		public void process(I selection) {
			DomDocument domDocument = DomEnvironment.get()
					.loadFromUrl(selection.get());
			Document<DomDocument> out = new DomSelection.Document<DomDocument>(
					selection, domDocument,
					domDocument.getDocumentElementNode().name());
			// state.select(out);
		}
	}
}
