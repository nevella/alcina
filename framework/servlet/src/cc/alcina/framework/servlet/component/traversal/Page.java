package cc.alcina.framework.servlet.component.traversal;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceChangeEvent;

import cc.alcina.framework.common.client.logic.reflection.PropertyEnum;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Click;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.KeyDown;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Change;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent.Context;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.edit.StringInput;
import cc.alcina.framework.servlet.component.traversal.TraversalEvents.SelectionSelected;
import cc.alcina.framework.servlet.component.traversal.TraversalHistories.TraversalHistory;
import cc.alcina.framework.servlet.component.traversal.TraversalProcessView.Ui;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace;

@Directed
class Page extends Model.All implements
		TraversalEvents.SelectionSelected.Handler, DomEvents.KeyDown.Handler {
	class Header extends Model.All
			implements DomEvents.Click.Handler, ModelEvents.Change.Handler {
		// FIXME - cmp - scheduler
		// @StringInput.FocusOnBind
		StringInput filter = new StringInput();

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

		@Override
		public void onClick(Click event) {
			new TraversalPlace().go();
		}

		@Override
		public void onChange(Change event) {
			new TraversalPlace().withTextFilter((String) event.getModel()).go();
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

	// FIXME - traversal - resolver/descendant event
	public static TraversalPlace traversalPlace() {
		return Ui.get().page.place;
	}

	Page() {
		header = new Header();
		/*
		 * FIXME - bindings - all these below - should be the children that bind
		 * to a PageContext event
		 */
		// FIXME - dirndl - bindings - change addListener to a ModelBinding with
		// a prebind (setleft) phase
		TraversalHistories.get().subscribe(null, this::setHistory);
		// bindings().addListener(() -> TraversalHistories.get().subscribe(null,
		// this::setHistory));
		bindings().from(this).on(Property.history).value(this)
				.map(SelectionLayers::new).accept(this::setLayers);
		bindings().from(this).on(Property.place).value(this)
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
		Place place = Client.currentPlace();
		if (place instanceof TraversalPlace) {
			this.place = (TraversalPlace) place;
		}
		// FIXME - dirndl - bindings - should set startup
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
		place.<TraversalPlace> copy().withSelection(event.getModel()).go();
	}

	void setHistory(TraversalHistory history) {
		set(Property.history, this.history, history,
				() -> this.history = history);
	}

	// FIXME - not hooked up?
	@Override
	public void onKeyDown(KeyDown event) {
		Context context = event.getContext();
		KeyDownEvent domEvent = (KeyDownEvent) context.getGwtEvent();
		switch (domEvent.getNativeKeyCode()) {
		case KeyCodes.KEY_ESCAPE:
			new TraversalPlace().go();
			break;
		}
	}
}
