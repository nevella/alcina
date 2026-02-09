package cc.alcina.framework.servlet.component.gallery.model.sequencearea;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
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
		extends GalleryContents<GallerySequenceAreaReportPlace> {
	PackageProperties._Gallery_SequenceAreaReportArea.InstanceProperties
			properties() {
		return PackageProperties.gallery_sequenceAreaReportArea.instance(this);
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
		bindings().from(properties().place())
				.typed(GallerySequenceAreaReportPlace.class)
				.accept(this::updateDefinition);
		from(properties().sequencePlace())
				.map(GallerySequenceAreaReportPlace::new).accept(BasePlace::go);
		model = new SequenceComponentServer(header,
				properties().sequencePlace());
	}

	void updateDefinition(GallerySequenceAreaReportPlace place) {
		SequencePlace sequencePlace = place.sequencePlace;
		properties().sequencePlace().set(sequencePlace);
		FlightEventSearchDefinition.Parameter typedParameter = sequencePlace.instanceQuery
				.typedParameter(FlightEventSearchDefinition.Parameter.class);
		if (typedParameter == null) {
			FlightEventSearchDefinition def = new FlightEventSearchDefinition();
			typedParameter = new FlightEventSearchDefinition.Parameter();
			typedParameter.withValue(def);
			sequencePlace.instanceQuery.addParameters(typedParameter);
		}
		FlightEventSearchDefinition def = typedParameter.getValue();
		header.properties().searchDefinition().set(def);
	}
}
