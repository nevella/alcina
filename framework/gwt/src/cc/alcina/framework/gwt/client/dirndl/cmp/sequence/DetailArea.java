package cc.alcina.framework.gwt.client.dirndl.cmp.sequence;

import cc.alcina.framework.common.client.collections.PublicCloneable;
import cc.alcina.framework.common.client.util.HasStringRepresentation;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.DirectedContextResolver;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedRenderer.TransformRenderer;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.layout.Tables;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.MarkupHighlights;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.component.StringArea;

@Directed(tag = "detail")
@DirectedContextResolver(StringArea.StringAreaResolver.class)
class DetailArea extends Model.Fields {
	@Directed
	Heading header = new Heading("Detail");

	@Directed(tag = "properties", bindToModel = false)
	@Directed(renderer = TransformRenderer.class)
	@Directed.Transform(Tables.Single.class)
	Object transformedSequenceElement;

	@Directed(tag = "properties", bindToModel = false)
	Object transformedAdditional;

	@Directed(tag = "string-representation")
	MarkupHighlights markupHighlights;

	SequenceArea sequenceArea;

	DetailArea(SequenceArea sequenceArea) {
		this.sequenceArea = sequenceArea;
	}

	@Override
	public void onBeforeRender(BeforeRender event) {
		Object sequenceElement = sequenceArea.getSelectedSequenceElement();
		if (sequenceElement == null) {
			return;
		}
		// todo - use a declarative transform on DetailArea.sequenceElement
		transformedSequenceElement = ((ModelTransform) sequenceArea.sequence
				.getDetailTransform()).apply(sequenceElement);
		if (transformedSequenceElement instanceof PublicCloneable) {
			// need to clone, cos may be bound
			transformedSequenceElement = ((PublicCloneable) transformedSequenceElement)
					.clone();
		}
		transformedAdditional = ((ModelTransform) sequenceArea.sequence
				.getDetailTransformAdditional()).apply(sequenceElement);
		if (transformedSequenceElement instanceof HasStringRepresentation) {
			String rep = ((HasStringRepresentation) transformedSequenceElement)
					.provideStringRepresentation();
			if (rep != null) {
				markupHighlights = new MarkupHighlights(rep, "", false,
						sequenceArea.getSelectedElementHighlights(),
						sequenceArea.getSelectedElementHighlightIndex());
				from(sequenceArea.service.getPlaceProperty())
						.signal(() -> markupHighlights.goToRange(sequenceArea
								.getSelectedElementHighlightIndex()));
			}
		}
		super.onBeforeRender(event);
	}
}
