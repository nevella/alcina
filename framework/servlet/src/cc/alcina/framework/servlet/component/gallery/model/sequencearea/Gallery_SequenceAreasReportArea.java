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
@Registration({ GalleryContents.class, GallerySequenceAreasReportPlace.class })
@TypedProperties
class Gallery_SequenceAreasReportArea
		extends GalleryContents<GallerySequenceAreasReportPlace> {
	PackageProperties._Gallery_SequenceAreasReportArea.InstanceProperties
			properties() {
		return PackageProperties.userSessionsReportArea.instance(this);
	}

	@TypedProperties
	class Header extends Model.All {
		PackageProperties._Gallery_SequenceAreasReportArea_Header.InstanceProperties
				properties() {
			return PackageProperties.userSessionsReportArea_header
					.instance(this);
		}

		@Directed(bindToModel = false)
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

	Gallery_SequenceAreasReportArea() {
		bindings().from(properties().place())
				.typed(GallerySequenceAreasReportPlace.class)
				.accept(this::updateDefinition);
		from(properties().sequencePlace())
				.map(GallerySequenceAreasReportPlace::new)
				.accept(BasePlace::go);
		model = new SequenceComponentServer(header,
				properties().sequencePlace());
	}

	void updateDefinition(GallerySequenceAreasReportPlace place) {
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
