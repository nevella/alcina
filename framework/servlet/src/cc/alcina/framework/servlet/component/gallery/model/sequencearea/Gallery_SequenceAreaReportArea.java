package cc.alcina.framework.servlet.component.gallery.model.sequencearea;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceEvents;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceEvents.SequencePlaceChanged;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequencePlace;
import cc.alcina.framework.gwt.client.dirndl.model.Feature_Dirndl_TableModel;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.search.SearchDefinitionEditor;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.servlet.component.gallery.GalleryContents;
import cc.alcina.framework.servlet.component.sequence.SequenceComponentServer;
import cc.alcina.framework.servlet.component.sequence.adapter.FlightEventSearchDefinition;

/*
 */
@Feature.Ref(Feature_Dirndl_TableModel._OrderService.class)
@Registration({ GalleryContents.class, GallerySequenceAreaReportPlace.class })
@TypedProperties
@Directed(tag = "gallery-sequence-area")
class Gallery_SequenceAreaReportArea
		extends GalleryContents<GallerySequenceAreaReportPlace>
		implements SequenceEvents.SequencePlaceChanged.Handler {
	PackageProperties._Gallery_SequenceAreaReportArea.InstanceProperties
			subtypeProperties() {
		return PackageProperties.gallery_sequenceAreaReportArea.instance(this);
	}

	@Override
	public void onSequencePlaceChanged(SequencePlaceChanged event) {
		GallerySequenceAreaReportPlace to = place.copy();
		SequencePlace model = event.getModel();
		to.sequencePlace = model;
		Client.refreshOrGoTo(to);
	}

	@TypedProperties
	class Header extends Model.All {
		PackageProperties._Gallery_SequenceAreaReportArea_Header.InstanceProperties
				properties() {
			return PackageProperties.gallery_sequenceAreaReportArea_header
					.instance(this);
		}

		@Directed.Transform(SearchDefinitionEditor.class)
		SearchDefinition searchDefinition;
	}

	/*
	 * either the generated report, or a spacer
	 * 
	 * todo - straight to sequencearea -
	 */
	Object model;

	@Directed.Exclude
	SequencePlace sequencePlace;

	@Directed.Exclude
	Header header = new Header();

	Gallery_SequenceAreaReportArea() {
		bindings().from(subtypeProperties().place())
				.typed(GallerySequenceAreaReportPlace.class)
				.accept(this::updateDefinition);
		from(subtypeProperties().sequencePlace())
				.map(GallerySequenceAreaReportPlace::new).accept(BasePlace::go);
		model = new SequenceComponentServer(header,
				subtypeProperties().sequencePlace());
	}

	void updateDefinition(GallerySequenceAreaReportPlace place) {
		SequencePlace sequencePlace = place.sequencePlace;
		subtypeProperties().sequencePlace().set(sequencePlace);
		FlightEventSearchDefinition def = (FlightEventSearchDefinition) sequencePlace.search;
		if (def == null) {
			def = new FlightEventSearchDefinition();
		}
		header.properties().searchDefinition().set(def);
	}
}
