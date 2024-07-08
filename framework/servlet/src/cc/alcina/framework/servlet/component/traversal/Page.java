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
import com.google.gwt.user.client.Event;

import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.TopicListener;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeybindingsHandler;
import cc.alcina.framework.gwt.client.dirndl.cmp.status.StatusModule;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.util.KeyboardShortcuts;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentObservables;
import cc.alcina.framework.servlet.component.traversal.TraversalCommand.ClearFilter;
import cc.alcina.framework.servlet.component.traversal.TraversalCommand.FocusSearch;
import cc.alcina.framework.servlet.component.traversal.TraversalEvents.FilterSelections;
import cc.alcina.framework.servlet.component.traversal.TraversalEvents.SelectionSelected;
import cc.alcina.framework.servlet.component.traversal.TraversalEvents.SelectionTypeSelected;
import cc.alcina.framework.servlet.component.traversal.TraversalBrowser.Ui;
import cc.alcina.framework.servlet.component.traversal.TraversalSettings.InputOutputDisplayMode;
import cc.alcina.framework.servlet.component.traversal.TraversalSettings.PropertyDisplayMode;
import cc.alcina.framework.servlet.component.traversal.TraversalBrowserCommand.InputOutputCycle;
import cc.alcina.framework.servlet.component.traversal.TraversalBrowserCommand.PropertyDisplayCycle;
import cc.alcina.framework.servlet.component.traversal.TraversalBrowserCommand.SelectionFilterModelContainment;
import cc.alcina.framework.servlet.component.traversal.TraversalBrowserCommand.SelectionFilterModelDescendant;
import cc.alcina.framework.servlet.component.traversal.TraversalBrowserCommand.SelectionFilterModelView;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace.SelectionType;
import cc.alcina.framework.servlet.dom.AbstractUi;

@Directed(
	bindings = @Binding(to = "tabIndex", literal = "0", type = Type.PROPERTY))
@TypedProperties
class Page extends Model.All
		implements TraversalEvents.SelectionSelected.Handler,
		TraversalEvents.SelectionTypeSelected.Handler,
		TraversalEvents.FilterSelections.Handler,
		TraversalCommand.ClearFilter.Handler,
		TraversalBrowserCommand.SelectionFilterModelContainment.Handler,
		TraversalBrowserCommand.SelectionFilterModelDescendant.Handler,
		TraversalBrowserCommand.SelectionFilterModelView.Handler,
		TraversalBrowserCommand.PropertyDisplayCycle.Handler,
		TraversalBrowserCommand.InputOutputCycle.Handler,
		TraversalCommand.FocusSearch.Handler {
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
	Ui ui;

	private StyleElement styleElement;

	private KeyboardShortcuts shortcuts;

	private KeybindingsHandler keybindingsHandler = new KeybindingsHandler(
			eventType -> {
				provideNode().dispatch(eventType, null);
			}, new CommandContextProviderImpl());

	Page() {
		header = new Header(this);
		this.ui = Ui.get();
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
				.map(SelectionLayers::new).to(this).on(properties.layers)
				.oneWay();
		bindings().from(this).on(properties.history).value(this)
				.map(PropertiesArea::new).to(this).on(properties.propertiesArea)
				.oneWay();
		bindings().from(this).on(properties.history)
				.map(o -> new RenderedSelections(this, true)).to(this)
				.on(properties.input).oneWay();
		bindings().from(this).on(properties.history)
				.map(o -> new RenderedSelections(this, false)).to(this)
				.on(properties.output).oneWay();
		bindings().from(ui).on(Ui.properties.place).typed(TraversalPlace.class)
				.filter(this::filterRedundantPlaceChange).value(this)
				.map(SelectionLayers::new).to(this).on(properties.layers)
				.oneWay();
		bindings().from(ui).on(Ui.properties.place).typed(TraversalPlace.class)
				.map(TraversalPlace::getTextFilter).to(header.mid.suggestor)
				.on("filterText").oneWay();
		bindings().from(TraversalBrowser.Ui.get().settings)
				.accept(this::updateStyles);
	}

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

	TraversalPlace place() {
		return Ui.place();
	}

	void clearPlaceSelections() {
		place().clearSelections();
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

	void goPreserveScrollPosition(TraversalPlace place) {
		Element scrollableLayers = layers.provideElement().getChildElement(1);
		int top = scrollableLayers.getScrollTop();
		place.go();
		layers.provideElement().getChildElement(1).setScrollTop(top);
	}

	public static class CommandContextProviderImpl
			implements CommandContext.Provider {
		Class<? extends CommandContext> appContext() {
			return TraversalBrowser.CommandContext.class;
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
		goPreserveScrollPosition(
				place().copy().withSelection(event.getModel()));
	}

	@Override
	public void onSelectionTypeSelected(SelectionTypeSelected event) {
		SelectionType selectionType = event.getSelectionType();
		changeSelectionType(selectionType);
	}

	void changeSelectionType(SelectionType selectionType) {
		if (Ui.place().lastSelectionType() == selectionType) {
			return;
		}
		TraversalPlace to = place().copy().withSelectionType(selectionType);
		goPreserveScrollPosition(to);
	}

	void setHistory(
			RemoteComponentObservables<SelectionTraversal>.ObservableHistory history) {
		set(properties.history, this.history, history,
				() -> this.history = history);
	}

	@Override
	public void onFilterSelections(FilterSelections event) {
		place().copy().withTextFilter(event.getModel()).go();
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
		TraversalSettings settings = TraversalBrowser.Ui.get().settings;
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
		TraversalSettings settings = TraversalBrowser.Ui.get().settings;
		InputOutputDisplayMode next = settings.nextInputOutputDisplayMode();
		StatusModule.get().showMessageTransitional(
				Ax.format("Input/Output display mode -> %s", next));
	}
}
