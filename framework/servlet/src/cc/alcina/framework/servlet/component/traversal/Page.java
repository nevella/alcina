package cc.alcina.framework.servlet.component.traversal;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceChangeEvent;
import com.google.gwt.user.client.Event;

import cc.alcina.framework.common.client.logic.reflection.PropertyEnum;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.KeyDown;
import cc.alcina.framework.gwt.client.dirndl.event.KeybindingsHandler;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent.Context;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.util.GlobalKeyboardShortcuts;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentObservables;
import cc.alcina.framework.servlet.component.traversal.TraversalEvents.SelectionSelected;
import cc.alcina.framework.servlet.component.traversal.TraversalEvents.SelectionTypeSelected;
import cc.alcina.framework.servlet.component.traversal.TraversalProcessView.Ui;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace.SelectionType;

@Directed(
	bindings = @Binding(to = "tabIndex", literal = "0", type = Type.PROPERTY))
class Page extends Model.All
		implements TraversalEvents.SelectionSelected.Handler,
		TraversalEvents.SelectionTypeSelected.Handler,
		DomEvents.KeyDown.Handler {
	// FIXME - traversal - resolver/descendant event
	public static TraversalPlace traversalPlace() {
		return Ui.get().page.place;
	}

	Header header;

	SelectionLayers layers;

	Properties properties;

	@Directed(className = "input")
	RenderedSelections input;

	@Directed(className = "output")
	RenderedSelections output;

	@Directed.Exclude
	RemoteComponentObservables<SelectionTraversal>.ObservableHistory history;

	@Directed.Exclude
	TraversalPlace place;

	Page() {
		header = new Header(this);
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
		bindings().from(this).on(Property.place).typed(TraversalPlace.class)
				.map(TraversalPlace::getTextFilter).to(header.mid.suggestor)
				.on("filterText").oneWay();
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

	GlobalKeyboardShortcuts shortcuts;

	void goPreserveScrollPosition(TraversalPlace place) {
		Element scrollableLayers = layers.provideElement().getChildElement(1);
		int top = scrollableLayers.getScrollTop();
		place.go();
		layers.provideElement().getChildElement(1).setScrollTop(top);
	}

	@Override
	public void onBeforeRender(BeforeRender event) {
		super.onBeforeRender(event);
	}

	@Override
	public void onBind(Bind event) {
		super.onBind(event);
		// FIXME - to a registration
		bindKeyboardShortcuts(event.isBound());
	}

	private KeybindingsHandler keybindingsHandler = new KeybindingsHandler(
			eventType -> provideNode().dispatch(eventType, null));

	private void bindKeyboardShortcuts(boolean bound) {
		if (bound) {
			shortcuts = new GlobalKeyboardShortcuts();
			Event.addNativePreviewHandler(shortcuts);
		}
		shortcuts.deltaHandler(keybindingsHandler, bound);
	}

	// FIXME - not hooked up?
	@Override
	public void onKeyDown(KeyDown event) {
		Context context = event.getContext();
		KeyDownEvent domEvent = (KeyDownEvent) context.getGwtEvent();
		if (GlobalKeyboardShortcuts.eventFiredFromInputish(
				domEvent.getNativeEvent().getEventTarget())) {
			return;
		}
		TraversalPlace to = null;
		switch (domEvent.getNativeKeyCode()) {
		case KeyCodes.KEY_ESCAPE:
			to = new TraversalPlace();
			break;
		case KeyCodes.KEY_ONE:
			to = place.copy().withSelectionType(SelectionType.VIEW);
			break;
		case KeyCodes.KEY_TWO:
			to = place.copy().withSelectionType(SelectionType.CONTAINMENT);
			break;
		case KeyCodes.KEY_THREE:
			to = place.withSelectionType(SelectionType.DESCENT);
			break;
		}
		if (to != null) {
			goPreserveScrollPosition(to);
		}
	}

	@Override
	public void onSelectionSelected(SelectionSelected event) {
		goPreserveScrollPosition(place.copy().withSelection(event.getModel()));
	}

	@Override
	public void onSelectionTypeSelected(SelectionTypeSelected event) {
		TraversalPlace to = place.copy()
				.withSelectionType(event.getSelectionType());
		goPreserveScrollPosition(to);
	}

	void setHistory(
			RemoteComponentObservables<SelectionTraversal>.ObservableHistory history) {
		set(Property.history, this.history, history,
				() -> this.history = history);
	}

	public void setInput(RenderedSelections input) {
		set("input", this.input, input, () -> this.input = input);
	}

	public void setLayers(SelectionLayers layers) {
		set("layers", this.layers, layers, () -> this.layers = layers);
	}

	public void setOutput(RenderedSelections output) {
		set("output", this.output, output, () -> this.output = output);
	}

	public void setPlace(TraversalPlace place) {
		set("place", this.place, place, () -> this.place = place);
	}

	public void setProperties(Properties properties) {
		set("properties", this.properties, properties,
				() -> this.properties = properties);
	}

	enum Property implements PropertyEnum {
		history, place
	}
}
