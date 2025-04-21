package cc.alcina.framework.servlet.component.traversal;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.activity.shared.PlaceUpdateable;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.StyleElement;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.dom.client.Text;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.serializer.FlatTreeSerializer;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.Selection.CopySelectionFilter;
import cc.alcina.framework.common.client.traversal.SelectionFilter;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.layer.SelectionMarkup;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.Timer;
import cc.alcina.framework.gwt.client.dirndl.activity.DirectedActivity;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestor;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext;
import cc.alcina.framework.gwt.client.dirndl.cmp.help.HelpPlace;
import cc.alcina.framework.gwt.client.dirndl.cmp.status.StatusModule;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.ApplicationHelp;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.CopyToClipboard;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.component.KeyboardShortcutsArea;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentObservables;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentObservables.ObservableEntry;
import cc.alcina.framework.servlet.component.shared.CopyToClipboardHandler;
import cc.alcina.framework.servlet.component.shared.ExecCommand;
import cc.alcina.framework.servlet.component.traversal.TraversalBrowser.Ui;
import cc.alcina.framework.servlet.component.traversal.TraversalBrowserCommand.PropertyDisplayCycle;
import cc.alcina.framework.servlet.component.traversal.TraversalBrowserCommand.SecondaryAreaDisplayCycle;
import cc.alcina.framework.servlet.component.traversal.TraversalBrowserCommand.SelectionFilterModelContainment;
import cc.alcina.framework.servlet.component.traversal.TraversalBrowserCommand.SelectionFilterModelDescendant;
import cc.alcina.framework.servlet.component.traversal.TraversalBrowserCommand.SelectionFilterModelView;
import cc.alcina.framework.servlet.component.traversal.TraversalBrowserCommand.ToggleHelp;
import cc.alcina.framework.servlet.component.traversal.TraversalCommand.ClearFilter;
import cc.alcina.framework.servlet.component.traversal.TraversalCommand.FocusSearch;
import cc.alcina.framework.servlet.component.traversal.TraversalCommand.ShowExecCommands;
import cc.alcina.framework.servlet.component.traversal.TraversalCommand.ShowKeyboardShortcuts;
import cc.alcina.framework.servlet.component.traversal.TraversalEvents.FilterSelections;
import cc.alcina.framework.servlet.component.traversal.TraversalEvents.LayerSelectionChange;
import cc.alcina.framework.servlet.component.traversal.TraversalEvents.SelectionSelected;
import cc.alcina.framework.servlet.component.traversal.TraversalEvents.SelectionTableAreaChange;
import cc.alcina.framework.servlet.component.traversal.TraversalEvents.SelectionTypeSelected;
import cc.alcina.framework.servlet.component.traversal.TraversalEvents.SetSettingSelectionAreaHeight;
import cc.alcina.framework.servlet.component.traversal.TraversalEvents.SetSettingTableRows;
import cc.alcina.framework.servlet.component.traversal.TraversalPlace.ListSource;
import cc.alcina.framework.servlet.component.traversal.TraversalPlace.SelectionPath;
import cc.alcina.framework.servlet.component.traversal.TraversalPlace.SelectionType;
import cc.alcina.framework.servlet.component.traversal.TraversalSettings.PropertyDisplayMode;
import cc.alcina.framework.servlet.component.traversal.TraversalSettings.SecondaryArea;
import cc.alcina.framework.servlet.component.traversal.TraversalSettings.SecondaryAreaDisplayMode;

@TypedProperties
class Page extends Model.All
		implements HasPage, TraversalEvents.SelectionSelected.Handler,
		TraversalEvents.SelectionTypeSelected.Handler,
		TraversalEvents.FilterSelections.Handler,
		TraversalCommand.ClearFilter.Handler,
		TraversalBrowserCommand.SelectionFilterModelContainment.Handler,
		TraversalBrowserCommand.SelectionFilterModelDescendant.Handler,
		TraversalBrowserCommand.SelectionFilterModelView.Handler,
		TraversalBrowserCommand.PropertyDisplayCycle.Handler,
		TraversalBrowserCommand.SecondaryAreaDisplayCycle.Handler,
		TraversalCommand.FocusSearch.Handler,
		TraversalEvents.SetSettingTableRows.Handler,
		TraversalEvents.SetSettingSelectionAreaHeight.Handler,
		FlightEventCommandHandlers,
		TraversalCommand.ShowKeyboardShortcuts.Handler,
		TraversalCommand.ShowExecCommands.Handler,
		TraversalEvents.LayerSelectionChange.Handler,
		TraversalBrowserCommand.ToggleHelp.Handler,
		ModelEvents.ApplicationHelp.Handler,
		Selection.CopySelectionFilter.Handler, CopyToClipboardHandler,
		ExecCommand.PerformCommand.Handler,
		TraversalEvents.SelectionTableAreaChange.Handler, Binding.TabIndexZero {
	public static class CommandContextProviderImpl
			implements CommandContext.Provider {
		@Override
		public Set<Class<? extends CommandContext>> getContexts() {
			Set<Class<? extends CommandContext>> commandContexts = new LinkedHashSet<>();
			commandContexts.add(appContext());
			commandContexts.add(FlightEventCommand.CommandContext.class);
			return commandContexts;
		}

		Class<? extends CommandContext> appContext() {
			return TraversalBrowser.CommandContext.class;
		}
	}

	/**
	 * This activity hooks the Page up to the RootArea (the general routing
	 * contract)
	 */
	@Directed.Delegating
	@Bean(PropertySource.FIELDS)
	@Registration({ DirectedActivity.class, TraversalPlace.class })
	static class ActivityRoute extends DirectedActivity
			// register in spite of non-public access
			implements Registration.AllSubtypes, PlaceUpdateable,
			ModelEvent.DelegatesDispatch,
			ModelEvents.TopLevelMissedEvent.Emitter {
		@Directed
		Page page;

		@Directed
		Model eventHandlerCustomisation;

		@Override
		public void onBeforeRender(BeforeRender event) {
			page = new Page();
			eventHandlerCustomisation = Ui.get().getEventHandlerCustomisation();
			super.onBeforeRender(event);
		}

		@Override
		public boolean canUpdate(PlaceUpdateable otherActivity) {
			/*
			 * All place updates are handled by the Page
			 */
			return true;
		}

		@Override
		public Model provideDispatchDelegate() {
			return page;
		}
	}

	static PackageProperties._Page properties = PackageProperties.page;

	Header header;

	SelectionLayers layers;

	PropertiesArea propertiesArea;

	@Directed(className = "input")
	RenderedSelections input;

	@Directed(className = "output")
	RenderedSelections output;

	@Directed(className = "table")
	RenderedSelections table;

	@Directed.Exclude
	RemoteComponentObservables<SelectionTraversal>.ObservableEntry history;

	@Property.Not
	Ui ui;

	private StyleElement styleElement;

	@Property.Not
	Logger logger = LoggerFactory.getLogger(getClass());

	Timer observableObservedTimer;

	@Property.Not
	List<?> currentTableElements;

	Page() {
		this.ui = Ui.get();
		this.ui.page = this;
		header = new Header(this);
		// FIXME - dirndl - bindings - change addListener to a ModelBinding with
		// a prebind (setleft) phase....maybe? that might be a bit too
		// tree-shaped, even for me
		bindings().addBindHandler(ui::bindKeyboardShortcuts);
		/*
		 * The traversl path can be specified by say the url
		 * /traversal?path=/traversal/0.1
		 */
		String traversalPath = Ui.get().getTraversalPath();
		// one-off (to get the initial value)
		TraversalObserver.get().subscribe(traversalPath, this::setHistory)
				.remove();
		/*
		 * logging
		 */
		bindings().from(this).on(properties.history).accept(
				history -> logger.info("history change :: {}", history));
		bindings().from(ui).on(Ui.properties.place).typed(TraversalPlace.class)
				.accept(place -> logger.info("place change :: {}", place));
		bindings().addListener(() -> TraversalObserver.get()
				.subscribe(traversalPath, this::setHistory));
		bindings().from(this).on(properties.history)
				.map(ObservableEntry::getObservable)
				.typed(SelectionTraversal.class).to(ui)
				.on(Ui.properties.traversal).oneWay();
		// place selections will be invalid if history changes
		bindings().from(this).on(properties.history)
				.signal(this::clearPlaceSelections);
		bindings().from(this).on(properties.history).value(this)
				.map(PropertiesArea::new).to(this).on(properties.propertiesArea)
				.oneWay();
		bindings().from(TraversalSettings.get())
				.on(TraversalSettings.properties.secondaryAreaDisplayMode)
				.value(() -> renderedSelectionsIfVisible(SecondaryArea.INPUT))
				.to(this).on(properties.input).oneWay();
		bindings().from(TraversalSettings.get())
				.on(TraversalSettings.properties.secondaryAreaDisplayMode)
				.value(() -> renderedSelectionsIfVisible(SecondaryArea.OUTPUT))
				.to(this).on(properties.output).oneWay();
		bindings().from(TraversalSettings.get())
				.on(TraversalSettings.properties.secondaryAreaDisplayMode)
				.value(() -> renderedSelectionsIfVisible(SecondaryArea.TABLE))
				.to(this).on(properties.table).oneWay();
		bindings().from(ui).on(Ui.properties.place).typed(TraversalPlace.class)
				.map(TraversalPlace::getTextFilter).to(header.mid.suggestor)
				.on(AppSuggestor.properties.acceptedFilterText).withFireOnce()
				.oneWay();
		bindings().from(ui).on(Ui.properties.place).typed(TraversalPlace.class)
				.map(TraversalPlace::getTextFilter).to(header.mid.suggestor)
				.on(AppSuggestor.properties.filterText).oneWay();
		bindings().from(ui).on(Ui.properties.place)
				.value(() -> new SelectionLayers(this)).to(this)
				.on(properties.layers).oneWay();
		bindings().from(TraversalBrowser.Ui.get().settings)
				.accept(this::updateStyles);
	}

	public Page providePage() {
		return this;
	}

	@Override
	public void onSelectionSelected(SelectionSelected event) {
		SelectionPath selectionPath = event.getModel();
		TraversalPlace to = place().copy().withSelection(selectionPath);
		to.clearLayerSelection();
		if (Ui.get().isClearPostSelectionLayers()) {
			int index = Ui.traversal().layers()
					.get(selectionPath.selection).index;
			to.clearLayersPost(index);
		}
		to.computeListSource(null, selectionPath.selection);
		goPreserveScrollPosition(to);
	}

	@Override
	public void onSelectionTypeSelected(SelectionTypeSelected event) {
		SelectionType selectionType = event.getSelectionType();
		changeSelectionType(selectionType);
	}

	@Override
	public void onBind(Bind event) {
		super.onBind(event);
		if (event.isBound()) {
			observableObservedTimer = Timer.Provider.get()
					.getTimer(this::observableAccessed);
		} else {
			observableObservedTimer.cancel();
		}
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
	public void onSecondaryAreaDisplayCycle(SecondaryAreaDisplayCycle event) {
		TraversalSettings settings = TraversalBrowser.Ui.get().settings;
		SecondaryAreaDisplayMode next = settings.nextSecondaryAreaDisplayMode();
		StatusModule.get().showMessageTransitional(
				Ax.format("Secondary display mode -> %s", next));
	}

	@Override
	public void onSetSettingTableRows(SetSettingTableRows event) {
		String model = event.getModel();
		TraversalBrowser.Ui.get().settings.putTableRows(model);
	}

	@Override
	public void onShowKeyboardShortcuts(ShowKeyboardShortcuts event) {
		KeyboardShortcutsArea
				.show(TraversalBrowser.Ui.get().getKeybindingsHandler());
	}

	@Override
	public void onLayerSelectionChange(LayerSelectionChange event) {
		TraversalPlace to = place().copy();
		int currentSelected = to.provideSelectedLayerIndex();
		to.clearLayerSelection();
		Layer selectedLayer = event.getModel();
		if (currentSelected != selectedLayer.index) {
			to.selectLayer(event.getModel());
			to.computeListSource(selectedLayer, null);
		}
		to.go();
	}

	@Override
	public void onToggleHelp(ToggleHelp event) {
		event.reemitAs(this, ApplicationHelp.class);
	}

	@Override
	public void onApplicationHelp(ApplicationHelp event) {
		HelpPlace.toggleRoot(Ui.place().copy()).go();
	}

	public List<? extends Selection> getFilteredSelections(Layer layer) {
		return layers.getFilteredSelections(layer);
	}

	@Override
	public void onCopySelectionFilter(CopySelectionFilter event) {
		Selection selection = event.getModel();
		SelectionFilter filter = SelectionFilter
				.ofSelections(List.of(selection));
		String serialized = FlatTreeSerializer.serializeSingleLine(filter);
		event.reemitAs(this, CopyToClipboard.class, serialized);
	}

	@Override
	public void onSetSettingSelectionAreaHeight(
			SetSettingSelectionAreaHeight event) {
		String model = event.getModel();
		TraversalBrowser.Ui.get().settings.putSelectionAreaHeight(model);
	}

	@Override
	public void onPerformCommand(ExecCommand.PerformCommand event) {
		TraversalExecCommand.Support.execCommand(event, currentTableElements,
				event.getModel());
	}

	@Override
	public void onSelectionTableAreaChange(SelectionTableAreaChange event) {
		currentTableElements = event.getModel();
	}

	RenderedSelections renderedSelectionsIfVisible(SecondaryArea area) {
		if (TraversalSettings.get().secondaryAreaDisplayMode.isVisible(area)) {
			return new RenderedSelections(this, area);
		} else {
			return null;
		}
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
			case FULL_WIDTH:
				rows.add("props props props props");
				builder.line("body > page > layers{display: none;}");
				break;
			case NONE:
				rows.add("layers layers layers layers");
				builder.line("body > page > properties{display: none;}");
				break;
			default:
				throw new UnsupportedOperationException();
			}
			builder.line("body > page {grid-template-rows: 50px 1fr %spx;}",
					settings.selectionAreaHeight);
			switch (settings.secondaryAreaDisplayMode) {
			case INPUT_OUTPUT:
				rows.add("input input output output");
				builder.line("body > page > selections.table{display: none;}");
				break;
			case INPUT:
				rows.add("input input input input");
				builder.line("body > page > selections.output{display: none;}");
				builder.line("body > page > selections.table{display: none;}");
				break;
			case OUTPUT:
				rows.add("output output output output");
				builder.line("body > page > selections.input{display: none;}");
				builder.line("body > page > selections.table{display: none;}");
				break;
			case TABLE:
				rows.add("table table table table");
				builder.line("body > page > selections.input{display: none;}");
				builder.line("body > page > selections.output{display: none;}");
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

	@Property.Not
	SelectionMarkup getSelectionMarkup() {
		return ui.getSelectionMarkup();
	}

	void goPreserveScrollPosition(TraversalPlace place) {
		Element scrollableLayers = layers.layersContainer.provideElement();
		int top = scrollableLayers.getScrollTop();
		place.go();
		// layers.provideElement().getChildElement(1).setScrollTop(top);
	}

	void changeSelectionType(SelectionType selectionType) {
		if (Ui.place().firstSelectionType() == selectionType) {
			return;
		}
		TraversalPlace to = place().copy().withSelectionType(selectionType);
		goPreserveScrollPosition(to);
	}

	void setHistory(
			RemoteComponentObservables<SelectionTraversal>.ObservableEntry history) {
		set(properties.history, this.history, history,
				() -> this.history = history);
	}

	void observableAccessed() {
		TraversalObserver.get().observableObserved(Ui.traversal());
	}

	@Override
	public void onShowExecCommands(ShowExecCommands event) {
		TraversalExecCommand.Support.showAvailableCommands();
	}
}
