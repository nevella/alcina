package cc.alcina.framework.servlet.component.console.rcs;

import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceEvents;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceEvents.SequencePlaceChanged;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequencePlace;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceSettings.DetailDisplayMode;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Clear;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.NotificationObservable;
import cc.alcina.framework.gwt.client.dirndl.model.SubHeading;
import cc.alcina.framework.gwt.client.dirndl.model.search.SearchDefinitionEditor;
import cc.alcina.framework.gwt.client.module.support.login.LoginPage.HeadingArea;
import cc.alcina.framework.servlet.component.console.ServerConsoleContents;
import cc.alcina.framework.servlet.component.sequence.SequenceComponentServer;
import cc.alcina.framework.servlet.logging.FlightEventRecorder;

@Feature.Ref(Feature_RomcomSessionConsole.class)
@Feature.Ref(Feature_RomcomSessionConsole._Dashboard.class)
@Registration({ ServerConsoleContents.class, RomcomSessionPlace.class })
@TypedProperties
class RomcomSessionArea extends ServerConsoleContents<RomcomSessionPlace>
		implements ModelEvents.Clear.Handler {
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

	HeadingArea heading = new HeadingArea("Romcom sessions", null);

	SubHeading subHeadingActions = new SubHeading("Actions");

	@Directed.Wrap("actions")
	List<Link> actions = List.of(Link.of(ModelEvents.Clear.class));

	@TypedProperties
	@Directed.Delegating
	class SequenceComponentContainer extends Model.All
			implements SequenceEvents.SequencePlaceChanged.Binding {
		PackageProperties._RomcomSessionArea_SequenceComponentContainer.InstanceProperties
				properties() {
			return PackageProperties.romcomSessionArea_sequenceComponentContainer
					.instance(this);
		}

		SubHeading subHeading;

		SequenceComponentContainer(String title) {
			subHeading = new SubHeading(title);
			sequence = new SequenceComponentServer(header,
					properties().sequencePlace());
			sequence.component.sequenceSettings.detailDisplayMode = DetailDisplayMode.QUARTER_WIDTH;
			sequence.component.elementLimit = 5;
			on(SequenceEvents.SequencePlaceChanged.class)
					.map(SequencePlaceChanged::getModel)
					.to(properties().sequencePlace()).oneWay();
			from(properties().sequencePlace())
					.signal(RomcomSessionArea.this::updateRomcomSessionPlace);
		}

		SequenceComponentServer sequence;

		@Directed.Exclude
		SequencePlace sequencePlace;

		/*
		 * this appears in layout as a child of the SequenceComponentServer, not
		 * of this model (hence the exclude)
		 */
		@Directed.Exclude
		Header header = new Header();

		void updateDefinition(SequencePlace sequencePlace) {
			properties().sequencePlace().set(sequencePlace);
			RomcomSessionSearchDefinition def = (RomcomSessionSearchDefinition) sequencePlace.search;
			header.properties().searchDefinition().set(def);
		}
	}

	SequenceComponentContainer active;

	SequenceComponentContainer inactive;

	RomcomSessionArea() {
		from(properties().place()).typed(RomcomSessionPlace.class)
				.accept(this::updateDefinition);
		active = new SequenceComponentContainer("Active sessions");
		inactive = new SequenceComponentContainer("Inactive sessions");
	}

	void updateRomcomSessionPlace() {
		new RomcomSessionPlace(active.sequencePlace, inactive.sequencePlace)
				.go();
	}

	void updateDefinition(RomcomSessionPlace place) {
		active.updateDefinition(place.activePlace);
		inactive.updateDefinition(place.inactivePlace);
	}

	@Override
	public void onClear(Clear event) {
		FlightEventRecorder.get().clear();
		RomcomSessionProvider.get().clear();
		NotificationObservable.of("Sessions cleared").publish();
	}
}
