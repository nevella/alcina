package cc.alcina.framework.servlet.component.console.rcs;

import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Clear;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.NotificationObservable;
import cc.alcina.framework.gwt.client.dirndl.model.SubHeading;
import cc.alcina.framework.gwt.client.module.support.login.LoginPage.HeadingArea;
import cc.alcina.framework.servlet.component.console.ServerConsoleContents;
import cc.alcina.framework.servlet.component.sequence.SequenceComponentEditor;
import cc.alcina.framework.servlet.logging.FlightEventRecorder;

@Feature.Ref(Feature_RomcomSessionConsole.class)
@Feature.Ref(Feature_RomcomSessionConsole._Dashboard.class)
@Feature.Ref(Feature_RomcomSessionConsole._Canned.class)
@Registration({ ServerConsoleContents.class, RomcomSessionPlace.class })
@TypedProperties
class RomcomSessionArea extends ServerConsoleContents<RomcomSessionPlace>
		implements ModelEvents.Clear.Handler,
		SequenceComponentEditor.DefinitionChanged.Binding {
	PackageProperties._RomcomSessionArea.InstanceProperties properties() {
		return PackageProperties.romcomSessionArea.instance(this);
	}

	HeadingArea heading = new HeadingArea("Romcom sessions", null);

	PresetsArea presets = new PresetsArea();

	SubHeading subHeadingActions = new SubHeading("Actions");

	@Directed.Wrap("actions")
	List<Link> actions = List.of(Link.of(ModelEvents.Clear.class));

	SequenceComponentEditor sequence;

	RomcomSessionArea() {
		from(properties().place()).typed(RomcomSessionPlace.class)
				.accept(this::updateDefinition);
		sequence = new SequenceComponentEditor("Inactive sessions");
		sequence.sequence.component.elementLimit = 5;
		on(SequenceComponentEditor.DefinitionChanged.class)
				.signal(this::updateRomcomSessionPlace);
	}

	void updateRomcomSessionPlace() {
		new RomcomSessionPlace(sequence.sequencePlace).go();
	}

	void updateDefinition(RomcomSessionPlace place) {
		sequence.updateDefinition(place.sequencePlace);
	}

	@Override
	public void onClear(Clear event) {
		FlightEventRecorder.get().clear();
		RomcomSessionProvider.get().clear();
		NotificationObservable.of("Sessions cleared").publish();
	}
}
