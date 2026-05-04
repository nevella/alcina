package cc.alcina.framework.servlet.component.console.rcs;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceEvents;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceEvents.SequencePlaceChanged;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequencePlace;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceSettings.DetailDisplayMode;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.search.SearchDefinitionEditor;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.servlet.component.console.ServerConsoleContents;
import cc.alcina.framework.servlet.component.sequence.SequenceComponentServer;

@Feature.Ref(Feature_RomcomSessionConsole.class)
@Registration({ ServerConsoleContents.class, RomcomSessionPlace.class })
@TypedProperties
class RomcomSessionArea extends ServerConsoleContents<RomcomSessionPlace>
		implements SequenceEvents.SequencePlaceChanged.Binding {
	PackageProperties._RomcomSessionArea.InstanceProperties properties() {
		return PackageProperties.romcomSessionArea.instance(this);
	}

	@TypedProperties
	class Header extends Model.All {
		PackageProperties._RomcomSessionArea_Header.InstanceProperties
				properties() {
			return PackageProperties.romcomSessionArea_header.instance(this);
		}

		@Directed.Transform(SearchDefinitionEditor.class)
		SearchDefinition searchDefinition;
	}

	SequenceComponentServer model;

	@Directed.Exclude
	SequencePlace sequencePlace;

	/*
	 * this appears in layout as a child of the SequenceComponentServer, not of
	 * this model (hence the exclude)
	 */
	@Directed.Exclude
	Header header = new Header();

	RomcomSessionArea() {
		from(properties().place()).typed(RomcomSessionPlace.class)
				.accept(this::updateDefinition);
		on(SequenceEvents.SequencePlaceChanged.class)
				.map(SequencePlaceChanged::getModel)
				.to(properties().sequencePlace()).oneWay();
		from(properties().sequencePlace()).map(RomcomSessionPlace::new)
				.accept(BasePlace::go);
		model = new SequenceComponentServer(header,
				properties().sequencePlace());
		model.component.sequenceSettings.detailDisplayMode = DetailDisplayMode.QUARTER_WIDTH;
	}

	void updateDefinition(RomcomSessionPlace place) {
		SequencePlace sequencePlace = place.sequencePlace;
		properties().sequencePlace().set(sequencePlace);
		RomcomSessionSearchDefinition def = (RomcomSessionSearchDefinition) place.sequencePlace.search;
		header.properties().searchDefinition().set(def);
	}
}
