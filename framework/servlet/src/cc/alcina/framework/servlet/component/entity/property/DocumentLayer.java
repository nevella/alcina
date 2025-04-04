package cc.alcina.framework.servlet.component.entity.property;

import cc.alcina.framework.common.client.traversal.DocumentSelection;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.PlainTextSelection;
import cc.alcina.framework.servlet.component.entity.property.PropertyFilterParser.Query;

/*
 * Emits a Document selection containing a single-element DOM document, the
 * element containing the normalised input text
 */
public class DocumentLayer extends Layer<Query> {
	@Override
	public void process(Query selection) throws Exception {
		select(new Document(selection));
	}

	static class Document extends DocumentSelection {
		public Document(PlainTextSelection parent) {
			super(parent, false);
		}
	}
}