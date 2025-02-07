package cc.alcina.framework.servlet.component.sequence;

import cc.alcina.framework.common.client.collections.PublicCloneable;
import cc.alcina.framework.common.client.util.HasStringRepresentation;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedRenderer.TransformRenderer;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.layout.Tables;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.MarkupHighlights;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.sequence.SequenceBrowser.Ui;

@Directed(tag = "detail")
class DetailArea extends Model.Fields {
	@Directed
	Heading header = new Heading("Detail");

	@Directed(tag = "properties", bindToModel = false)
	@Directed(renderer = TransformRenderer.class)
	@Directed.Transform(Tables.Single.class)
	Object transformedSequenceElement;

	@Directed(tag = "string-representation")
	MarkupHighlights markupHighlights;

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
		if (transformedSequenceElement instanceof PublicCloneable) {
			// need to clone, cos may be bound
			transformedSequenceElement = ((PublicCloneable) transformedSequenceElement)
					.clone();
		}
		if (transformedSequenceElement instanceof HasStringRepresentation) {
			String rep = ((HasStringRepresentation) transformedSequenceElement)
					.provideStringRepresentation();
			if (rep != null) {
				markupHighlights = new MarkupHighlights(rep, false,
						page.getSelectedElementHighlights(),
						page.getSelectedElementHighlightIndex());
				bindings().from(page.ui).on(Ui.properties.place)
						.accept(place -> markupHighlights.goToRange(
								page.getSelectedElementHighlightIndex()));
			}
		}
	}
}
