package cc.alcina.framework.servlet.component.traversal;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.StyleElement;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.dom.client.Text;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceChangeEvent;
import com.google.gwt.user.client.Event;

import cc.alcina.framework.common.client.logic.reflection.PropertyEnum;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeybindingsHandler;
import cc.alcina.framework.gwt.client.dirndl.cmp.status.StatusModule;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.util.KeyboardShortcuts;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentObservables;
import cc.alcina.framework.servlet.component.traversal.TraversalCommands.ClearFilter;
import cc.alcina.framework.servlet.component.traversal.TraversalEvents.FilterSelections;
import cc.alcina.framework.servlet.component.traversal.TraversalEvents.SelectionSelected;
import cc.alcina.framework.servlet.component.traversal.TraversalEvents.SelectionTypeSelected;
import cc.alcina.framework.servlet.component.traversal.TraversalProcessView.Ui;
import cc.alcina.framework.servlet.component.traversal.TraversalSettings.PropertyDisplayMode;
import cc.alcina.framework.servlet.component.traversal.TraversalViewCommands.PropertyDisplayCycle;
import cc.alcina.framework.servlet.component.traversal.TraversalViewCommands.SelectionFilterModelContainment;
import cc.alcina.framework.servlet.component.traversal.TraversalViewCommands.SelectionFilterModelDescendant;
import cc.alcina.framework.servlet.component.traversal.TraversalViewCommands.SelectionFilterModelView;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace.SelectionType;

@Directed(
	bindings = @Binding(to = "tabIndex", literal = "0", type = Type.PROPERTY))
class Page extends Model.All
		implements TraversalEvents.SelectionSelected.Handler,
		TraversalEvents.SelectionTypeSelected.Handler,
		TraversalEvents.FilterSelections.Handler,
		TraversalCommands.ClearFilter.Handler,
		TraversalViewCommands.SelectionFilterModelContainment.Handler,
		TraversalViewCommands.SelectionFilterModelDescendant.Handler,
		TraversalViewCommands.SelectionFilterModelView.Handler,
		TraversalViewCommands.PropertyDisplayCycle.Handler {
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
		// a prebind (setleft) phase....maybe? that might be a bit too
		// tree-shaped, even for me
		bindings().addBindHandler(this::bindKeyboardShortcuts);
		// one-off (to get the initial value)
		TraversalHistories.get().subscribe(null, this::setHistory).remove();
		bindings().addListener(() -> TraversalHistories.get().subscribe(null,
				this::setHistory));
		bindings().from(this).on(Property.history).value(this)
				.map(SelectionLayers::new).accept(this::setLayers);
		bindings().from(this).on(Property.place).typed(TraversalPlace.class)
				.filter(this::placeChangeCausesSelectionLayersChange)
				.value(this).map(SelectionLayers::new).accept(this::setLayers);
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
		bindings().from(TraversalProcessView.Ui.get().settings)
				.accept(this::updateStyles);
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
		// (later - not sure what this means. probably that the an init
		// PlaceChangeEvent should be fired on beforeRender/setleft, in addition
		// to regular listening)
		bindings().addRegistration(() -> Client.eventBus()
				.addHandler(PlaceChangeEvent.TYPE, handler));
	}

	private StyleElement styleElement;

	void updateStyles(TraversalSettings settings) {
		FormatBuilder builder = new FormatBuilder();
		{
			/*
			 * body > page grid-template-areas - default:
			 * "header header header header" "layers layers layers props"
			 * "input input output output";
			 */
			List<String> rows = new ArrayList<>();
			rows.add("header header header header");
			switch (settings.propertyDisplayMode) {
			case QUARTER_WIDTH:
				rows.add("layers layers layers props");
				break;
			case HALF_WIDTH:
				rows.add("layers layers props props");
				break;
			case NONE:
				rows.add("layers layers layers layers");
				builder.line("body > page > properties{display: none;}");
				break;
			default:
				throw new UnsupportedOperationException();
			}
			rows.add("input input output output");
			//
			String areas = rows.stream().map(s -> Ax.format("\"%s\"", s))
					.collect(Collectors.joining(" "));
			builder.line("body > page {grid-template-areas: %s;}", areas);
		}
		String text = builder.toString();
		if (styleElement == null) {
			styleElement = StyleInjector.createAndAttachElement(text);
		} else {
			((Text) styleElement.getChild(0)).setTextContent(text);
		}
	}

	boolean placeChangeCausesSelectionLayersChange(TraversalPlace place) {
		if (layers != null) {
			return layers.placeChangeCausesChange(place);
		} else {
			return true;
		}
	}

	KeyboardShortcuts shortcuts;

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
	}

	// needs fix
	private KeybindingsHandler keybindingsHandler = new KeybindingsHandler(
			eventType -> {
				provideNode().dispatch(eventType, null);
			}, new CommandContextProviderImpl());

	public static class CommandContextProviderImpl
			implements CommandContext.Provider {
		Class<? extends CommandContext> appContext() {
			return TraversalViewContext.class;
		}

		@Override
		public Set<Class<? extends CommandContext>> getContexts() {
			Set<Class<? extends CommandContext>> commandContexts = new LinkedHashSet<>();
			commandContexts.add(appContext());
			return commandContexts;
		}
	}

	private void bindKeyboardShortcuts(boolean bound) {
		if (bound) {
			shortcuts = new KeyboardShortcuts();
			Event.addNativePreviewHandler(shortcuts);
		}
		shortcuts.deltaHandler(keybindingsHandler, bound);
	}

	@Override
	public void onSelectionSelected(SelectionSelected event) {
		goPreserveScrollPosition(place.copy().withSelection(event.getModel()));
	}

	@Override
	public void onSelectionTypeSelected(SelectionTypeSelected event) {
		SelectionType selectionType = event.getSelectionType();
		changeSelectionType(selectionType);
	}

	void changeSelectionType(SelectionType selectionType) {
		TraversalPlace to = place.copy().withSelectionType(selectionType);
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

	@Override
	public void onFilterSelections(FilterSelections event) {
		place.copy().withTextFilter(event.getModel()).go();
	}

	@Override
	public void onClearFilter(ClearFilter event) {
		header.mid.suggestor.clear();
		new TraversalPlace().go();
	}

	@Override
	public void onSelectionFilterModelView(SelectionFilterModelView event) {
		changeSelectionType(SelectionType.VIEW);
	}

	@Override
	public void onSelectionFilterModelDescendant(
			SelectionFilterModelDescendant event) {
		changeSelectionType(SelectionType.DESCENT);
	}

	@Override
	public void onSelectionFilterModelContainment(
			SelectionFilterModelContainment event) {
		changeSelectionType(SelectionType.CONTAINMENT);
	}

	@Override
	public void onPropertyDisplayCycle(PropertyDisplayCycle event) {
		TraversalSettings settings = TraversalProcessView.Ui.get().settings;
		PropertyDisplayMode propertyDisplayMode = settings.propertyDisplayMode;
		PropertyDisplayMode next = PropertyDisplayMode
				.values()[(propertyDisplayMode.ordinal() + 1)
						% PropertyDisplayMode.values().length];
		settings.setPropertyDisplayMode(next);
		StatusModule.get().showMessageTransitional(
				Ax.format("Property display mode -> %s", next));
	}
}
