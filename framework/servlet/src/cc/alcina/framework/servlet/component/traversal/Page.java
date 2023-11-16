package cc.alcina.framework.servlet.component.traversal;

import com.google.gwt.place.shared.PlaceChangeEvent;

import cc.alcina.framework.common.client.logic.reflection.PropertyEnum;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.traversal.TraversalEvents.SelectionSelected;
import cc.alcina.framework.servlet.component.traversal.TraversalHistories.TraversalHistory;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace;

@Directed
class Page extends Model.All
		implements TraversalEvents.SelectionSelected.Handler {
	class Header extends Model.All {
		String name = TraversalProcessView.Ui.get().getMainCaption();

		Header() {
			bindings().from(Page.this).on(Property.history)
					.typed(TraversalHistory.class).map(this::computeName)
					.accept(this::setName);
		}

		public void setName(String name) {
			set("name", this.name, name, () -> this.name = name);
		}

		String computeName(TraversalHistory history) {
			FormatBuilder format = new FormatBuilder().separator(" - ");
			format.append(TraversalProcessView.Ui.get().getMainCaption());
			format.appendIfNonNull(history, TraversalHistory::displayName);
			return format.toString();
		}
	}

	enum Property implements PropertyEnum {
		history, place
	}

	Header header;

	SelectionLayers layers;

	Properties properties;

	@Directed(className = "input")
	RenderedSelections input;

	@Directed(className = "output")
	RenderedSelections output;

	@Directed.Exclude
	TraversalHistory history;

	@Directed.Exclude
	TraversalPlace place;

	Page() {
		header = new Header();
		// FIXME - dirndl - bindings - change addListener to a ModelBinding with
		// a prebind (setleft) phase
		TraversalHistories.get().subscribe(null, this::setHistory);
		// bindings().addListener(() -> TraversalHistories.get().subscribe(null,
		// this::setHistory));
		bindings().from(this).on(Property.history).value(this)
				.map(SelectionLayers::new).accept(this::setLayers);
		bindings().from(this).on(Property.history).value(this)
				.map(Properties::new).accept(this::setProperties);
		bindings().from(this).on(Property.history)
				.map(o -> new RenderedSelections(this, true))
				.accept(this::setInput);
		bindings().from(this).on(Property.history)
				.map(o -> new RenderedSelections(this, false))
				.accept(this::setOutput);
		PlaceChangeEvent.Handler handler = evt -> {
			if (evt.getNewPlace() instanceof TraversalPlace) {
				setPlace((TraversalPlace) evt.getNewPlace());
			}
		};
		bindings().addRegistration(() -> Client.eventBus()
				.addHandler(PlaceChangeEvent.TYPE, handler));
	}

	public void setPlace(TraversalPlace place) {
		set("place", this.place, place, () -> this.place = place);
	}

	public void setInput(RenderedSelections input) {
		set("input", this.input, input, () -> this.input = input);
	}

	public void setOutput(RenderedSelections output) {
		set("output", this.output, output, () -> this.output = output);
	}

	public void setLayers(SelectionLayers layers) {
		set("layers", this.layers, layers, () -> this.layers = layers);
	}

	public void setProperties(Properties properties) {
		set("properties", this.properties, properties,
				() -> this.properties = properties);
	}

	@Override
	public void onBeforeRender(BeforeRender event) {
		super.onBeforeRender(event);
	}

	@Override
	public void onBind(Bind event) {
		super.onBind(event);
	}

	@Override
	public void onSelectionSelected(SelectionSelected event) {
		new TraversalPlace().withSelection(event.getModel()).go();
	}

	void setHistory(TraversalHistory history) {
		set(Property.history, this.history, history,
				() -> this.history = history);
	}
}
