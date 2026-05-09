package cc.alcina.framework.servlet.component.gallery.model.sequencearea;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.search.TextCriterion;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.Feature_Dirndl_TableModel;
import cc.alcina.framework.servlet.component.gallery.GalleryContents;
import cc.alcina.framework.servlet.component.sequence.SequenceComponentEditor;
import cc.alcina.framework.servlet.component.sequence.adapter.FlightEventSearchDefinition;

/*
 */
@Feature.Ref(Feature_Dirndl_TableModel._OrderService.class)
@Registration({ GalleryContents.class, GallerySequenceAreaReportPlace.class })
@TypedProperties
@Directed(tag = "gallery-sequence-area")
class Gallery_SequenceAreaReportArea
		extends GalleryContents<GallerySequenceAreaReportPlace>
		implements SequenceComponentEditor.DefinitionChanged.Binding {
	PackageProperties._Gallery_SequenceAreaReportArea.InstanceProperties
			subtypeProperties() {
		return PackageProperties.gallery_sequenceAreaReportArea.instance(this);
	}

	void updatePlace() {
		GallerySequenceAreaReportPlace to = place.copy();
		to.sequencePlace = sequenceEditor.sequencePlace;
		Client.refreshOrGoTo(to);
	}

	SequenceComponentEditor sequenceEditor;

	Gallery_SequenceAreaReportArea() {
		bindings().from(subtypeProperties().place())
				.typed(GallerySequenceAreaReportPlace.class)
				.accept(this::updateDefinition);
		sequenceEditor = new SequenceComponentEditor("");
		on(SequenceComponentEditor.DefinitionChanged.class)
				.signal(this::updatePlace);
	}

	void updateDefinition(GallerySequenceAreaReportPlace place) {
		if (place.sequencePlace.search == null) {
			FlightEventSearchDefinition def = new FlightEventSearchDefinition();
			def.withCriterion(new TextCriterion(" "));
			place.sequencePlace.search = def;
		}
		sequenceEditor.updateDefinition(place.sequencePlace);
	}
}
