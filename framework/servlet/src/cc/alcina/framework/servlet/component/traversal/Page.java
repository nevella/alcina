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

import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.TopicListener;
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
import cc.alcina.framework.servlet.component.traversal.TraversalCommands.FocusSearch;
import cc.alcina.framework.servlet.component.traversal.TraversalEvents.FilterSelections;
import cc.alcina.framework.servlet.component.traversal.TraversalEvents.SelectionSelected;
import cc.alcina.framework.servlet.component.traversal.TraversalEvents.SelectionTypeSelected;
import cc.alcina.framework.servlet.component.traversal.TraversalProcessView.Ui;
import cc.alcina.framework.servlet.component.traversal.TraversalSettings.InputOutputDisplayMode;
import cc.alcina.framework.servlet.component.traversal.TraversalSettings.PropertyDisplayMode;
import cc.alcina.framework.servlet.component.traversal.TraversalViewCommands.InputOutputCycle;
import cc.alcina.framework.servlet.component.traversal.TraversalViewCommands.PropertyDisplayCycle;
import cc.alcina.framework.servlet.component.traversal.TraversalViewCommands.SelectionFilterModelContainment;
import cc.alcina.framework.servlet.component.traversal.TraversalViewCommands.SelectionFilterModelDescendant;
import cc.alcina.framework.servlet.component.traversal.TraversalViewCommands.SelectionFilterModelView;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace.SelectionType;

@Directed(
	bindings = @Binding(to = "tabIndex", literal = "0", type = Type.PROPERTY))
@TypedProperties
class Page extends Model.All
		implements TraversalEvents.SelectionSelected.Handler,
		TraversalEvents.SelectionTypeSelected.Handler,
		TraversalEvents.FilterSelections.Handler,
		TraversalCommands.ClearFilter.Handler,
		TraversalViewCommands.SelectionFilterModelContainment.Handler,
		TraversalViewCommands.SelectionFilterModelDescendant.Handler,
		TraversalViewCommands.SelectionFilterModelView.Handler,
		TraversalViewCommands.PropertyDisplayCycle.Handler,
		TraversalViewCommands.InputOutputCycle.Handler,
		TraversalCommands.FocusSearch.Handler {
	// FIXME - traversal - resolver/descendant event
	public static TraversalPlace traversalPlace() {
		return Ui.get().page.place;
	}

	static PackageProperties._Page properties = PackageProperties.page;

	Header header;

	SelectionLayers layers;

	PropertiesArea propertiesArea;

	@Directed(className = "input")
	RenderedSelections input;

	@Directed(className = "output")
	RenderedSelections output;

	@Directed.Exclude
	RemoteComponentObservables<SelectionTraversal>.ObservableHistory history;

	@Directed.Exclude
	TraversalPlace place;

	@cc.alcina.framework.common.client.reflection.Property.Not
	TopicListener<RemoteComponentObservables<SelectionTraversal>.ObservableHistory> historySubsciption = this::setHistory;

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
		/*
		 * The traversl path can be specified by say the url
		 * /traversal?path=/traversal/0.1
		 */
		String traversalPath = Ui.get().getTraversalPath();
		// one-off (to get the initial value)
		TraversalHistories.get().subscribe(traversalPath, this::setHistory)
				.remove();
		bindings().addListener(() -> TraversalHistories.get()
				.subscribe(traversalPath, this::setHistory));
		// place selections will be invalid if history changes
		bindings().from(this).on(properties.history)
				.signal(this::clearPlaceSelections);
		bindings().from(this).on(properties.history).value(this)
				.map(SelectionLayers::new).accept(this::setLayers);
		bindings().from(this).on(properties.history).value(this)
				.map(PropertiesArea::new).accept(this::setPropertiesArea);
		bindings().from(this).on(properties.history)
				.map(o -> new RenderedSelections(this, true))
				.accept(this::setInput);
		bindings().from(this).on(properties.history)
				.map(o -> new RenderedSelections(this, false))
				.accept(this::setOutput);
		bindings().from(this).on(properties.place).typed(TraversalPlace.class)
				.filter(this::filterRedundantPlaceChange).value(this)
				.map(SelectionLayers::new).accept(this::setLayers);
		bindings().from(this).on(properties.place).typed(TraversalPlace.class)
				.map(TraversalPlace::getTextFilter).to(header.mid.suggestor)
				.on("filterText").oneWay();
		bindings().from(TraversalProcessView.Ui.get().settings)
				.accept(this::updateStyles);
		PlaceChangeEvent.Handler handler = evt -> {
			if (evt.getNewPlace() instanceof TraversalPlace) {
				setPlace((TraversalPlace) evt.getNewPlace());
				Ui.get().setPlace(this.place);
			}
		};
		this.place = Ui.place();
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
			builder.line("body > page {grid-template-rows: 50px 1fr 260px;}");
			switch (settings.inputOutputDisplayMode) {
			case INPUT_OUTPUT:
				rows.add("input input output output");
				break;
			case INPUT:
				rows.add("input input input input");
				builder.line("body > page > selections.output{display: none;}");
				break;
			case OUTPUT:
				rows.add("output output output output");
				builder.line("body > page > selections.input{display: none;}");
				break;
			case NONE:
				builder.line("body > page > selections{display: none;}");
				builder.line("body > page {grid-template-rows: 50px 1fr;}");
				break;
			default:
				throw new UnsupportedOperationException();
			}
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

	void clearPlaceSelections() {
		if (this.place != null) {
			this.place.clearSelections();
		}
	}

	boolean filterRedundantPlaceChange(TraversalPlace place) {
		if (layers != null) {
			if (layers.traversal != Ui.traversal()) {
				// layers will be changed anyway by traversal change, redundant
				return false;
			} else {
				return layers.placeChangeCausesChange(place);
			}
		} else {
			return true;
		}
	}

	@Property.Not
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
		if (place.lastSelectionType() == selectionType) {
			return;
		}
		TraversalPlace to = place.copy().withSelectionType(selectionType);
		goPreserveScrollPosition(to);
	}

	void setHistory(
			RemoteComponentObservables<SelectionTraversal>.ObservableHistory history) {
		set(properties.history, this.history, history,
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

	public void setPropertiesArea(PropertiesArea properties) {
		set("properties", this.propertiesArea, properties,
				() -> this.propertiesArea = properties);
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
		PropertyDisplayMode next = settings.nextPropertyDisplayMode();
		StatusModule.get().showMessageTransitional(
				Ax.format("Property display mode -> %s", next));
	}

	@Override
	public void onFocusSearch(FocusSearch event) {
		header.mid.suggestor.focus();
	}

	@Override
	public void onInputOutputCycle(InputOutputCycle event) {
		TraversalSettings settings = TraversalProcessView.Ui.get().settings;
		InputOutputDisplayMode next = settings.nextInputOutputDisplayMode();
		StatusModule.get().showMessageTransitional(
				Ax.format("Input/Output display mode -> %s", next));
	}
}
