package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.context.LooseContextInstance;
import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.layer.SelectionMarkup;
import cc.alcina.framework.common.client.util.Timer;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeybindingsHandler;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel;
import cc.alcina.framework.gwt.client.dirndl.model.CollectionDeltaModel;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentObservables;
import cc.alcina.framework.servlet.component.traversal.Header;
import cc.alcina.framework.servlet.component.traversal.LayerFilterEditor;
import cc.alcina.framework.servlet.component.traversal.LayerSelections;
import cc.alcina.framework.servlet.component.traversal.Page;
import cc.alcina.framework.servlet.component.traversal.PropertiesArea;
import cc.alcina.framework.servlet.component.traversal.RenderedSelections;
import cc.alcina.framework.servlet.component.traversal.SelectionLayers;
import cc.alcina.framework.servlet.component.traversal.SelectionTableArea;
import cc.alcina.framework.servlet.component.traversal.TraversalPlace;
import cc.alcina.framework.servlet.component.traversal.TraversalSettings;
import com.google.gwt.dom.client.StyleElement;
import java.lang.Boolean;
import java.lang.Class;
import java.lang.Integer;
import java.lang.String;
import java.util.List;
import java.util.Set;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    public static _AppSuggestorTraversal appSuggestorTraversal = new _AppSuggestorTraversal();
    static _Dotburger_Menu dotburger_menu = new _Dotburger_Menu();
    static _Header_Left header_left = new _Header_Left();
    static _LayerSelections layerSelections = new _LayerSelections();
    static _LayerSelections_NameArea layerSelections_nameArea = new _LayerSelections_NameArea();
    static _LayerSelections_SelectionsArea layerSelections_selectionsArea = new _LayerSelections_SelectionsArea();
    static _Page page = new _Page();
    static _Page_ActivityRoute page_activityRoute = new _Page_ActivityRoute();
    static _RenderedSelections renderedSelections = new _RenderedSelections();
    static _SelectionLayers_LayersContainer selectionLayers_layersContainer = new _SelectionLayers_LayersContainer();
    public static _TraversalBrowser_Ui traversalBrowser_ui = new _TraversalBrowser_Ui();
    public static _TraversalSettings traversalSettings = new _TraversalSettings();
    
    public static class _AppSuggestorTraversal implements TypedProperty.Container {
      public TypedProperty<AppSuggestorTraversal, String> acceptedFilterText = new TypedProperty<>(AppSuggestorTraversal.class, "acceptedFilterText");
      public TypedProperty<AppSuggestorTraversal, String> filterText = new TypedProperty<>(AppSuggestorTraversal.class, "filterText");
      public TypedProperty<AppSuggestorTraversal, Suggestor> suggestor = new TypedProperty<>(AppSuggestorTraversal.class, "suggestor");
      public static class InstanceProperties extends InstanceProperty.Container<AppSuggestorTraversal> {
        public  InstanceProperties(AppSuggestorTraversal source){super(source);}
        public InstanceProperty<AppSuggestorTraversal, String> acceptedFilterText(){return new InstanceProperty<>(source,PackageProperties.appSuggestorTraversal.acceptedFilterText);}
        public InstanceProperty<AppSuggestorTraversal, String> filterText(){return new InstanceProperty<>(source,PackageProperties.appSuggestorTraversal.filterText);}
        public InstanceProperty<AppSuggestorTraversal, Suggestor> suggestor(){return new InstanceProperty<>(source,PackageProperties.appSuggestorTraversal.suggestor);}
      }
      
      public  InstanceProperties instance(AppSuggestorTraversal instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _Dotburger_Menu implements TypedProperty.Container {
      TypedProperty<Dotburger.Menu, Link> keyboardShortcuts = new TypedProperty<>(Dotburger.Menu.class, "keyboardShortcuts");
      TypedProperty<Dotburger.Menu, TraversalSettings.PropertyDisplayMode> propertyDisplayMode = new TypedProperty<>(Dotburger.Menu.class, "propertyDisplayMode");
      TypedProperty<Dotburger.Menu, String> rows = new TypedProperty<>(Dotburger.Menu.class, "rows");
      TypedProperty<Dotburger.Menu, TraversalSettings.SecondaryAreaDisplayMode> secondaryAreaDisplayMode = new TypedProperty<>(Dotburger.Menu.class, "secondaryAreaDisplayMode");
      TypedProperty<Dotburger.Menu, Heading> section1 = new TypedProperty<>(Dotburger.Menu.class, "section1");
      TypedProperty<Dotburger.Menu, Heading> section2 = new TypedProperty<>(Dotburger.Menu.class, "section2");
      TypedProperty<Dotburger.Menu, Heading> section3 = new TypedProperty<>(Dotburger.Menu.class, "section3");
      TypedProperty<Dotburger.Menu, Heading> section4 = new TypedProperty<>(Dotburger.Menu.class, "section4");
      TypedProperty<Dotburger.Menu, Heading> section5 = new TypedProperty<>(Dotburger.Menu.class, "section5");
      TypedProperty<Dotburger.Menu, String> selectionAreaHeight = new TypedProperty<>(Dotburger.Menu.class, "selectionAreaHeight");
      TypedProperty<Dotburger.Menu, TraversalPlace.SelectionType> selectionType = new TypedProperty<>(Dotburger.Menu.class, "selectionType");
      static class InstanceProperties extends InstanceProperty.Container<Dotburger.Menu> {
         InstanceProperties(Dotburger.Menu source){super(source);}
        InstanceProperty<Dotburger.Menu, Link> keyboardShortcuts(){return new InstanceProperty<>(source,PackageProperties.dotburger_menu.keyboardShortcuts);}
        InstanceProperty<Dotburger.Menu, TraversalSettings.PropertyDisplayMode> propertyDisplayMode(){return new InstanceProperty<>(source,PackageProperties.dotburger_menu.propertyDisplayMode);}
        InstanceProperty<Dotburger.Menu, String> rows(){return new InstanceProperty<>(source,PackageProperties.dotburger_menu.rows);}
        InstanceProperty<Dotburger.Menu, TraversalSettings.SecondaryAreaDisplayMode> secondaryAreaDisplayMode(){return new InstanceProperty<>(source,PackageProperties.dotburger_menu.secondaryAreaDisplayMode);}
        InstanceProperty<Dotburger.Menu, Heading> section1(){return new InstanceProperty<>(source,PackageProperties.dotburger_menu.section1);}
        InstanceProperty<Dotburger.Menu, Heading> section2(){return new InstanceProperty<>(source,PackageProperties.dotburger_menu.section2);}
        InstanceProperty<Dotburger.Menu, Heading> section3(){return new InstanceProperty<>(source,PackageProperties.dotburger_menu.section3);}
        InstanceProperty<Dotburger.Menu, Heading> section4(){return new InstanceProperty<>(source,PackageProperties.dotburger_menu.section4);}
        InstanceProperty<Dotburger.Menu, Heading> section5(){return new InstanceProperty<>(source,PackageProperties.dotburger_menu.section5);}
        InstanceProperty<Dotburger.Menu, String> selectionAreaHeight(){return new InstanceProperty<>(source,PackageProperties.dotburger_menu.selectionAreaHeight);}
        InstanceProperty<Dotburger.Menu, TraversalPlace.SelectionType> selectionType(){return new InstanceProperty<>(source,PackageProperties.dotburger_menu.selectionType);}
      }
      
       InstanceProperties instance(Dotburger.Menu instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _Header_Left implements TypedProperty.Container {
      TypedProperty<Header.Left, List> additional = new TypedProperty<>(Header.Left.class, "additional");
      TypedProperty<Header.Left, String> name = new TypedProperty<>(Header.Left.class, "name");
      static class InstanceProperties extends InstanceProperty.Container<Header.Left> {
         InstanceProperties(Header.Left source){super(source);}
        InstanceProperty<Header.Left, List> additional(){return new InstanceProperty<>(source,PackageProperties.header_left.additional);}
        InstanceProperty<Header.Left, String> name(){return new InstanceProperty<>(source,PackageProperties.header_left.name);}
      }
      
       InstanceProperties instance(Header.Left instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _LayerSelections implements TypedProperty.Container {
      TypedProperty<LayerSelections, Boolean> empty = new TypedProperty<>(LayerSelections.class, "empty");
      TypedProperty<LayerSelections, LayerSelections.NameArea> nameArea = new TypedProperty<>(LayerSelections.class, "nameArea");
      TypedProperty<LayerSelections, LayerSelections.SelectionsArea> selectionsArea = new TypedProperty<>(LayerSelections.class, "selectionsArea");
      static class InstanceProperties extends InstanceProperty.Container<LayerSelections> {
         InstanceProperties(LayerSelections source){super(source);}
        InstanceProperty<LayerSelections, Boolean> empty(){return new InstanceProperty<>(source,PackageProperties.layerSelections.empty);}
        InstanceProperty<LayerSelections, LayerSelections.NameArea> nameArea(){return new InstanceProperty<>(source,PackageProperties.layerSelections.nameArea);}
        InstanceProperty<LayerSelections, LayerSelections.SelectionsArea> selectionsArea(){return new InstanceProperty<>(source,PackageProperties.layerSelections.selectionsArea);}
      }
      
       InstanceProperties instance(LayerSelections instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _LayerSelections_NameArea implements TypedProperty.Container {
      TypedProperty<LayerSelections.NameArea, LayerFilterEditor> filter = new TypedProperty<>(LayerSelections.NameArea.class, "filter");
      TypedProperty<LayerSelections.NameArea, Boolean> filterEditorOpen = new TypedProperty<>(LayerSelections.NameArea.class, "filterEditorOpen");
      TypedProperty<LayerSelections.NameArea, Boolean> hasFilter = new TypedProperty<>(LayerSelections.NameArea.class, "hasFilter");
      TypedProperty<LayerSelections.NameArea, LeafModel.TextTitle> key = new TypedProperty<>(LayerSelections.NameArea.class, "key");
      TypedProperty<LayerSelections.NameArea, String> outputs = new TypedProperty<>(LayerSelections.NameArea.class, "outputs");
      TypedProperty<LayerSelections.NameArea, Boolean> selected = new TypedProperty<>(LayerSelections.NameArea.class, "selected");
      static class InstanceProperties extends InstanceProperty.Container<LayerSelections.NameArea> {
         InstanceProperties(LayerSelections.NameArea source){super(source);}
        InstanceProperty<LayerSelections.NameArea, LayerFilterEditor> filter(){return new InstanceProperty<>(source,PackageProperties.layerSelections_nameArea.filter);}
        InstanceProperty<LayerSelections.NameArea, Boolean> filterEditorOpen(){return new InstanceProperty<>(source,PackageProperties.layerSelections_nameArea.filterEditorOpen);}
        InstanceProperty<LayerSelections.NameArea, Boolean> hasFilter(){return new InstanceProperty<>(source,PackageProperties.layerSelections_nameArea.hasFilter);}
        InstanceProperty<LayerSelections.NameArea, LeafModel.TextTitle> key(){return new InstanceProperty<>(source,PackageProperties.layerSelections_nameArea.key);}
        InstanceProperty<LayerSelections.NameArea, String> outputs(){return new InstanceProperty<>(source,PackageProperties.layerSelections_nameArea.outputs);}
        InstanceProperty<LayerSelections.NameArea, Boolean> selected(){return new InstanceProperty<>(source,PackageProperties.layerSelections_nameArea.selected);}
      }
      
       InstanceProperties instance(LayerSelections.NameArea instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _LayerSelections_SelectionsArea implements TypedProperty.Container {
      TypedProperty<LayerSelections.SelectionsArea, List> filtered = new TypedProperty<>(LayerSelections.SelectionsArea.class, "filtered");
      TypedProperty<LayerSelections.SelectionsArea, List> filteredSelections = new TypedProperty<>(LayerSelections.SelectionsArea.class, "filteredSelections");
      TypedProperty<LayerSelections.SelectionsArea, Boolean> parallel = new TypedProperty<>(LayerSelections.SelectionsArea.class, "parallel");
      TypedProperty<LayerSelections.SelectionsArea, List> selections = new TypedProperty<>(LayerSelections.SelectionsArea.class, "selections");
      TypedProperty<LayerSelections.SelectionsArea, LooseContextInstance> snapshot = new TypedProperty<>(LayerSelections.SelectionsArea.class, "snapshot");
      TypedProperty<LayerSelections.SelectionsArea, List> tableSelections = new TypedProperty<>(LayerSelections.SelectionsArea.class, "tableSelections");
      TypedProperty<LayerSelections.SelectionsArea, LayerSelections.SelectionsArea.TestHistory> testHistory = new TypedProperty<>(LayerSelections.SelectionsArea.class, "testHistory");
      static class InstanceProperties extends InstanceProperty.Container<LayerSelections.SelectionsArea> {
         InstanceProperties(LayerSelections.SelectionsArea source){super(source);}
        InstanceProperty<LayerSelections.SelectionsArea, List> filtered(){return new InstanceProperty<>(source,PackageProperties.layerSelections_selectionsArea.filtered);}
        InstanceProperty<LayerSelections.SelectionsArea, List> filteredSelections(){return new InstanceProperty<>(source,PackageProperties.layerSelections_selectionsArea.filteredSelections);}
        InstanceProperty<LayerSelections.SelectionsArea, Boolean> parallel(){return new InstanceProperty<>(source,PackageProperties.layerSelections_selectionsArea.parallel);}
        InstanceProperty<LayerSelections.SelectionsArea, List> selections(){return new InstanceProperty<>(source,PackageProperties.layerSelections_selectionsArea.selections);}
        InstanceProperty<LayerSelections.SelectionsArea, LooseContextInstance> snapshot(){return new InstanceProperty<>(source,PackageProperties.layerSelections_selectionsArea.snapshot);}
        InstanceProperty<LayerSelections.SelectionsArea, List> tableSelections(){return new InstanceProperty<>(source,PackageProperties.layerSelections_selectionsArea.tableSelections);}
        InstanceProperty<LayerSelections.SelectionsArea, LayerSelections.SelectionsArea.TestHistory> testHistory(){return new InstanceProperty<>(source,PackageProperties.layerSelections_selectionsArea.testHistory);}
      }
      
       InstanceProperties instance(LayerSelections.SelectionsArea instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _Page implements TypedProperty.Container {
      TypedProperty<Page, Header> header = new TypedProperty<>(Page.class, "header");
      TypedProperty<Page, RemoteComponentObservables.ObservableEntry> history = new TypedProperty<>(Page.class, "history");
      TypedProperty<Page, RenderedSelections> input = new TypedProperty<>(Page.class, "input");
      TypedProperty<Page, SelectionLayers> layers = new TypedProperty<>(Page.class, "layers");
      TypedProperty<Page, Timer> observableObservedTimer = new TypedProperty<>(Page.class, "observableObservedTimer");
      TypedProperty<Page, RenderedSelections> output = new TypedProperty<>(Page.class, "output");
      TypedProperty<Page, PropertiesArea> propertiesArea = new TypedProperty<>(Page.class, "propertiesArea");
      TypedProperty<Page, RenderedSelections> table = new TypedProperty<>(Page.class, "table");
      static class InstanceProperties extends InstanceProperty.Container<Page> {
         InstanceProperties(Page source){super(source);}
        InstanceProperty<Page, Header> header(){return new InstanceProperty<>(source,PackageProperties.page.header);}
        InstanceProperty<Page, RemoteComponentObservables.ObservableEntry> history(){return new InstanceProperty<>(source,PackageProperties.page.history);}
        InstanceProperty<Page, RenderedSelections> input(){return new InstanceProperty<>(source,PackageProperties.page.input);}
        InstanceProperty<Page, SelectionLayers> layers(){return new InstanceProperty<>(source,PackageProperties.page.layers);}
        InstanceProperty<Page, Timer> observableObservedTimer(){return new InstanceProperty<>(source,PackageProperties.page.observableObservedTimer);}
        InstanceProperty<Page, RenderedSelections> output(){return new InstanceProperty<>(source,PackageProperties.page.output);}
        InstanceProperty<Page, PropertiesArea> propertiesArea(){return new InstanceProperty<>(source,PackageProperties.page.propertiesArea);}
        InstanceProperty<Page, RenderedSelections> table(){return new InstanceProperty<>(source,PackageProperties.page.table);}
      }
      
       InstanceProperties instance(Page instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _Page_ActivityRoute implements TypedProperty.Container {
      TypedProperty<Page.ActivityRoute, Class> channel = new TypedProperty<>(Page.ActivityRoute.class, "channel");
      TypedProperty<Page.ActivityRoute, Model> eventHandlerCustomisation = new TypedProperty<>(Page.ActivityRoute.class, "eventHandlerCustomisation");
      TypedProperty<Page.ActivityRoute, Page> page = new TypedProperty<>(Page.ActivityRoute.class, "page");
      TypedProperty<Page.ActivityRoute, BasePlace> place = new TypedProperty<>(Page.ActivityRoute.class, "place");
      static class InstanceProperties extends InstanceProperty.Container<Page.ActivityRoute> {
         InstanceProperties(Page.ActivityRoute source){super(source);}
        InstanceProperty<Page.ActivityRoute, Class> channel(){return new InstanceProperty<>(source,PackageProperties.page_activityRoute.channel);}
        InstanceProperty<Page.ActivityRoute, Model> eventHandlerCustomisation(){return new InstanceProperty<>(source,PackageProperties.page_activityRoute.eventHandlerCustomisation);}
        InstanceProperty<Page.ActivityRoute, Page> page(){return new InstanceProperty<>(source,PackageProperties.page_activityRoute.page);}
        InstanceProperty<Page.ActivityRoute, BasePlace> place(){return new InstanceProperty<>(source,PackageProperties.page_activityRoute.place);}
      }
      
       InstanceProperties instance(Page.ActivityRoute instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _RenderedSelections implements TypedProperty.Container {
      TypedProperty<RenderedSelections, Heading> heading = new TypedProperty<>(RenderedSelections.class, "heading");
      TypedProperty<RenderedSelections, Page> page = new TypedProperty<>(RenderedSelections.class, "page");
      TypedProperty<RenderedSelections, Selection> selection = new TypedProperty<>(RenderedSelections.class, "selection");
      TypedProperty<RenderedSelections, RenderedSelections.SelectionMarkupArea> selectionMarkupArea = new TypedProperty<>(RenderedSelections.class, "selectionMarkupArea");
      TypedProperty<RenderedSelections, SelectionTableArea> selectionTable = new TypedProperty<>(RenderedSelections.class, "selectionTable");
      TypedProperty<RenderedSelections, StyleElement> styleElement = new TypedProperty<>(RenderedSelections.class, "styleElement");
      TypedProperty<RenderedSelections, TraversalSettings.SecondaryArea> variant = new TypedProperty<>(RenderedSelections.class, "variant");
      static class InstanceProperties extends InstanceProperty.Container<RenderedSelections> {
         InstanceProperties(RenderedSelections source){super(source);}
        InstanceProperty<RenderedSelections, Heading> heading(){return new InstanceProperty<>(source,PackageProperties.renderedSelections.heading);}
        InstanceProperty<RenderedSelections, Page> page(){return new InstanceProperty<>(source,PackageProperties.renderedSelections.page);}
        InstanceProperty<RenderedSelections, Selection> selection(){return new InstanceProperty<>(source,PackageProperties.renderedSelections.selection);}
        InstanceProperty<RenderedSelections, RenderedSelections.SelectionMarkupArea> selectionMarkupArea(){return new InstanceProperty<>(source,PackageProperties.renderedSelections.selectionMarkupArea);}
        InstanceProperty<RenderedSelections, SelectionTableArea> selectionTable(){return new InstanceProperty<>(source,PackageProperties.renderedSelections.selectionTable);}
        InstanceProperty<RenderedSelections, StyleElement> styleElement(){return new InstanceProperty<>(source,PackageProperties.renderedSelections.styleElement);}
        InstanceProperty<RenderedSelections, TraversalSettings.SecondaryArea> variant(){return new InstanceProperty<>(source,PackageProperties.renderedSelections.variant);}
      }
      
       InstanceProperties instance(RenderedSelections instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _SelectionLayers_LayersContainer implements TypedProperty.Container {
      TypedProperty<SelectionLayers.LayersContainer, CollectionDeltaModel> collectionRepresentation = new TypedProperty<>(SelectionLayers.LayersContainer.class, "collectionRepresentation");
      TypedProperty<SelectionLayers.LayersContainer, List> layers = new TypedProperty<>(SelectionLayers.LayersContainer.class, "layers");
      static class InstanceProperties extends InstanceProperty.Container<SelectionLayers.LayersContainer> {
         InstanceProperties(SelectionLayers.LayersContainer source){super(source);}
        InstanceProperty<SelectionLayers.LayersContainer, CollectionDeltaModel> collectionRepresentation(){return new InstanceProperty<>(source,PackageProperties.selectionLayers_layersContainer.collectionRepresentation);}
        InstanceProperty<SelectionLayers.LayersContainer, List> layers(){return new InstanceProperty<>(source,PackageProperties.selectionLayers_layersContainer.layers);}
      }
      
       InstanceProperties instance(SelectionLayers.LayersContainer instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _TraversalBrowser_Ui implements TypedProperty.Container {
      public TypedProperty<TraversalBrowser.Ui, Set> appCommandContexts = new TypedProperty<>(TraversalBrowser.Ui.class, "appCommandContexts");
      public TypedProperty<TraversalBrowser.Ui, Boolean> appendTableSelections = new TypedProperty<>(TraversalBrowser.Ui.class, "appendTableSelections");
      public TypedProperty<TraversalBrowser.Ui, Boolean> clearPostSelectionLayers = new TypedProperty<>(TraversalBrowser.Ui.class, "clearPostSelectionLayers");
      public TypedProperty<TraversalBrowser.Ui, CommandContext.Provider> commandContextProvider = new TypedProperty<>(TraversalBrowser.Ui.class, "commandContextProvider");
      public TypedProperty<TraversalBrowser.Ui, Model> eventHandlerCustomisation = new TypedProperty<>(TraversalBrowser.Ui.class, "eventHandlerCustomisation");
      public TypedProperty<TraversalBrowser.Ui, RemoteComponentObservables.ObservableEntry> history = new TypedProperty<>(TraversalBrowser.Ui.class, "history");
      public TypedProperty<TraversalBrowser.Ui, KeybindingsHandler> keybindingsHandler = new TypedProperty<>(TraversalBrowser.Ui.class, "keybindingsHandler");
      public TypedProperty<TraversalBrowser.Ui, Layer> listSourceLayer0 = new TypedProperty<>(TraversalBrowser.Ui.class, "listSourceLayer0");
      public TypedProperty<TraversalBrowser.Ui, String> mainCaption = new TypedProperty<>(TraversalBrowser.Ui.class, "mainCaption");
      public TypedProperty<TraversalBrowser.Ui, Page> page = new TypedProperty<>(TraversalBrowser.Ui.class, "page");
      public TypedProperty<TraversalBrowser.Ui, TraversalPlace> place = new TypedProperty<>(TraversalBrowser.Ui.class, "place");
      public TypedProperty<TraversalBrowser.Ui, Layer> selectedLayer0 = new TypedProperty<>(TraversalBrowser.Ui.class, "selectedLayer0");
      public TypedProperty<TraversalBrowser.Ui, SelectionMarkup> selectionMarkup = new TypedProperty<>(TraversalBrowser.Ui.class, "selectionMarkup");
      public TypedProperty<TraversalBrowser.Ui, TraversalSettings> settings = new TypedProperty<>(TraversalBrowser.Ui.class, "settings");
      public TypedProperty<TraversalBrowser.Ui, SelectionTraversal> traversal = new TypedProperty<>(TraversalBrowser.Ui.class, "traversal");
      public TypedProperty<TraversalBrowser.Ui, String> traversalPath = new TypedProperty<>(TraversalBrowser.Ui.class, "traversalPath");
      public TypedProperty<TraversalBrowser.Ui, Boolean> useSelectionSegmentPath = new TypedProperty<>(TraversalBrowser.Ui.class, "useSelectionSegmentPath");
      public static class InstanceProperties extends InstanceProperty.Container<TraversalBrowser.Ui> {
        public  InstanceProperties(TraversalBrowser.Ui source){super(source);}
        public InstanceProperty<TraversalBrowser.Ui, Set> appCommandContexts(){return new InstanceProperty<>(source,PackageProperties.traversalBrowser_ui.appCommandContexts);}
        public InstanceProperty<TraversalBrowser.Ui, Boolean> appendTableSelections(){return new InstanceProperty<>(source,PackageProperties.traversalBrowser_ui.appendTableSelections);}
        public InstanceProperty<TraversalBrowser.Ui, Boolean> clearPostSelectionLayers(){return new InstanceProperty<>(source,PackageProperties.traversalBrowser_ui.clearPostSelectionLayers);}
        public InstanceProperty<TraversalBrowser.Ui, CommandContext.Provider> commandContextProvider(){return new InstanceProperty<>(source,PackageProperties.traversalBrowser_ui.commandContextProvider);}
        public InstanceProperty<TraversalBrowser.Ui, Model> eventHandlerCustomisation(){return new InstanceProperty<>(source,PackageProperties.traversalBrowser_ui.eventHandlerCustomisation);}
        public InstanceProperty<TraversalBrowser.Ui, RemoteComponentObservables.ObservableEntry> history(){return new InstanceProperty<>(source,PackageProperties.traversalBrowser_ui.history);}
        public InstanceProperty<TraversalBrowser.Ui, KeybindingsHandler> keybindingsHandler(){return new InstanceProperty<>(source,PackageProperties.traversalBrowser_ui.keybindingsHandler);}
        public InstanceProperty<TraversalBrowser.Ui, Layer> listSourceLayer0(){return new InstanceProperty<>(source,PackageProperties.traversalBrowser_ui.listSourceLayer0);}
        public InstanceProperty<TraversalBrowser.Ui, String> mainCaption(){return new InstanceProperty<>(source,PackageProperties.traversalBrowser_ui.mainCaption);}
        public InstanceProperty<TraversalBrowser.Ui, Page> page(){return new InstanceProperty<>(source,PackageProperties.traversalBrowser_ui.page);}
        public InstanceProperty<TraversalBrowser.Ui, TraversalPlace> place(){return new InstanceProperty<>(source,PackageProperties.traversalBrowser_ui.place);}
        public InstanceProperty<TraversalBrowser.Ui, Layer> selectedLayer0(){return new InstanceProperty<>(source,PackageProperties.traversalBrowser_ui.selectedLayer0);}
        public InstanceProperty<TraversalBrowser.Ui, SelectionMarkup> selectionMarkup(){return new InstanceProperty<>(source,PackageProperties.traversalBrowser_ui.selectionMarkup);}
        public InstanceProperty<TraversalBrowser.Ui, TraversalSettings> settings(){return new InstanceProperty<>(source,PackageProperties.traversalBrowser_ui.settings);}
        public InstanceProperty<TraversalBrowser.Ui, SelectionTraversal> traversal(){return new InstanceProperty<>(source,PackageProperties.traversalBrowser_ui.traversal);}
        public InstanceProperty<TraversalBrowser.Ui, String> traversalPath(){return new InstanceProperty<>(source,PackageProperties.traversalBrowser_ui.traversalPath);}
        public InstanceProperty<TraversalBrowser.Ui, Boolean> useSelectionSegmentPath(){return new InstanceProperty<>(source,PackageProperties.traversalBrowser_ui.useSelectionSegmentPath);}
      }
      
      public  InstanceProperties instance(TraversalBrowser.Ui instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _TraversalSettings implements TypedProperty.Container {
      public TypedProperty<TraversalSettings, Boolean> descentSelectionIncludesSecondaryRelations = new TypedProperty<>(TraversalSettings.class, "descentSelectionIncludesSecondaryRelations");
      public TypedProperty<TraversalSettings, TraversalSettings.PropertyDisplayMode> propertyDisplayMode = new TypedProperty<>(TraversalSettings.class, "propertyDisplayMode");
      public TypedProperty<TraversalSettings, TraversalSettings.SecondaryAreaDisplayMode> secondaryAreaDisplayMode = new TypedProperty<>(TraversalSettings.class, "secondaryAreaDisplayMode");
      public TypedProperty<TraversalSettings, Integer> selectionAreaHeight = new TypedProperty<>(TraversalSettings.class, "selectionAreaHeight");
      public TypedProperty<TraversalSettings, Boolean> showContainerLayers = new TypedProperty<>(TraversalSettings.class, "showContainerLayers");
      public TypedProperty<TraversalSettings, Integer> tableRows = new TypedProperty<>(TraversalSettings.class, "tableRows");
      public static class InstanceProperties extends InstanceProperty.Container<TraversalSettings> {
        public  InstanceProperties(TraversalSettings source){super(source);}
        public InstanceProperty<TraversalSettings, Boolean> descentSelectionIncludesSecondaryRelations(){return new InstanceProperty<>(source,PackageProperties.traversalSettings.descentSelectionIncludesSecondaryRelations);}
        public InstanceProperty<TraversalSettings, TraversalSettings.PropertyDisplayMode> propertyDisplayMode(){return new InstanceProperty<>(source,PackageProperties.traversalSettings.propertyDisplayMode);}
        public InstanceProperty<TraversalSettings, TraversalSettings.SecondaryAreaDisplayMode> secondaryAreaDisplayMode(){return new InstanceProperty<>(source,PackageProperties.traversalSettings.secondaryAreaDisplayMode);}
        public InstanceProperty<TraversalSettings, Integer> selectionAreaHeight(){return new InstanceProperty<>(source,PackageProperties.traversalSettings.selectionAreaHeight);}
        public InstanceProperty<TraversalSettings, Boolean> showContainerLayers(){return new InstanceProperty<>(source,PackageProperties.traversalSettings.showContainerLayers);}
        public InstanceProperty<TraversalSettings, Integer> tableRows(){return new InstanceProperty<>(source,PackageProperties.traversalSettings.tableRows);}
      }
      
      public  InstanceProperties instance(TraversalSettings instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
