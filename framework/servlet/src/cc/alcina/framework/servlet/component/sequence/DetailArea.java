package cc.alcina.framework.servlet.component.sequence;

import cc.alcina.framework.common.client.util.HasStringRepresentation;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedRenderer.TransformRenderer;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.layout.Tables;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

@Directed(tag = "detail")
class DetailArea extends Model.Fields {
	@Directed
	Heading header = new Heading("Detail");

	@Directed(tag = "properties")
	@Directed(renderer = TransformRenderer.class)
	@Directed.Transform(Tables.Single.class)
	Object transformedSequenceElement;

	@Directed(tag = "string-representation")
	MarkupHighlights stringRepresentation;

	Page page;

	DetailArea(Page page) {
		this.page = page;
		Object sequenceElement = page.getSelectedSequenceElement();
		if (sequenceElement == null) {
			return;
		}
		// todo - use a declarative transform on DetailArea.sequenceElement
		transformedSequenceElement = ((ModelTransform) page.sequence
				.getDetailTransform()).apply(sequenceElement);
		if (transformedSequenceElement instanceof HasStringRepresentation) {
			String rep = ((HasStringRepresentation) transformedSequenceElement)
					.provideStringRepresentation();
			if (rep != null) {
				stringRepresentation = new MarkupHighlights(rep, false,
						page.getSelectedElementHighlights());
				int selectedElementHighlightIndex = page
						.getSelectedElementHighlightIndex();
				if (selectedElementHighlightIndex != -1) {
					Client.eventBus().queued().deferred()
							.lambda(() -> stringRepresentation
									.goToRange(selectedElementHighlightIndex))
							.dispatch();
				}
			}
		}
	}
}
