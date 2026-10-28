package cc.alcina.framework.servlet.transform.fragment;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.traversal.DocumentSelection;
import cc.alcina.framework.common.client.traversal.Selection;

/*
 * Transform the incoming DomDocument payload selection to a Document selection
 * 
 * Unlike most layer subtypes, this class is re-used (since generic) so the
 * input type must be specified in the constructor, not as a generic type
 * parameter. Normally the subsequent layer (this is pretty much always an
 * intermediate layer) will be an instance of InputsFromPreviousSibling
 * 
 * 
 */
public class DomDocumentLayer<U extends Selection<DomDocument>>
		extends DocumentSelection.TransformLayer<U, DomDocumentLayer.Document> {
	public static class Document extends DocumentSelection
			implements StyledDocument.AccessorSelection {
		StyledDocument styledDocument;

		public Document(Selection<DomDocument> parent,
				StyledDocument styledDocument) {
			super(parent, parent.get());
			this.styledDocument = styledDocument;
		}

		@Override
		public StyledDocument getStyledDocument() {
			return styledDocument;
		}
	}
}