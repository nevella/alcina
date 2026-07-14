package cc.alcina.framework.servlet.component.sequence;

import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceEvents;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceEvents.SequencePlaceChanged;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequencePlace;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceSearchDefinition;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceSettings.DetailDisplayMode;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.SubHeading;
import cc.alcina.framework.gwt.client.dirndl.model.search.SearchDefinitionEditor;

/**
 * An definitioneditor/sequence pair. The container updates its place on
 * DefinitionChanged events and calls #updateDafinition to update this
 * component's place
 */
@TypedProperties
@Directed.Delegating
public class SequenceComponentEditor extends Model.All
		implements SequenceEvents.SequencePlaceChanged.Binding {
	@TypedProperties
	static class Header extends Model.All {
		PackageProperties._SequenceComponentEditor_Header.InstanceProperties
				properties() {
			return PackageProperties.sequenceComponentEditor_header
					.instance(this);
		}

		@Directed.Transform(
			value = SearchDefinitionEditor.class,
			transformsNull = false)
		SearchDefinition searchDefinition;
	}

	PackageProperties._SequenceComponentEditor.InstanceProperties properties() {
		return PackageProperties.sequenceComponentEditor.instance(this);
	}

	SubHeading subHeading;

	public SequenceComponentEditor(String title) {
		subHeading = new SubHeading(title);
		sequence = new SequenceComponentServer(header,
				properties().sequencePlace());
		sequence.component.sequenceSettings.detailDisplayMode = DetailDisplayMode.QUARTER_WIDTH;
		on(SequenceEvents.SequencePlaceChanged.class)
				.map(SequencePlaceChanged::getModel)
				.to(properties().sequencePlace()).oneWay();
		from(properties().sequencePlace()).emit(DefinitionChanged.class);
	}

	public SequenceComponentServer sequence;

	@Directed.Exclude
	public SequencePlace sequencePlace;

	/*
	 * this appears in layout as a child of the SequenceComponentServer, not of
	 * this model (hence the exclude)
	 */
	@Directed.Exclude
	Header header = new Header();

	public void updateDefinition(SequencePlace sequencePlace) {
		updateDefinition(sequencePlace, sequencePlace.search);
	}

	public void updateDefinition(SequencePlace sequencePlace,
			SequenceSearchDefinition search) {
		properties().sequencePlace().set(sequencePlace);
		header.properties().searchDefinition().set(search);
	}

	public static class DefinitionChanged
			extends ModelEvent<Object, DefinitionChanged.Handler> {
		@Override
		public void dispatch(DefinitionChanged.Handler handler) {
			handler.onDefinitionChanged(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onDefinitionChanged(DefinitionChanged event);
		}

		public interface Binding extends Handler {
			@Override
			default void onDefinitionChanged(DefinitionChanged event) {
				((Model) this).bindings().onNodeEvent(event);
			}
		}
	}
}