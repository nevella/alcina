package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.layer.SelectionMarkup;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeybindingsHandler;
import cc.alcina.framework.gwt.client.dirndl.model.CollectionDeltaModel;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentObservables;
import cc.alcina.framework.servlet.component.traversal.Header;
import cc.alcina.framework.servlet.component.traversal.Page;
import cc.alcina.framework.servlet.component.traversal.PropertiesArea;
import cc.alcina.framework.servlet.component.traversal.RenderedSelections;
import cc.alcina.framework.servlet.component.traversal.SelectionLayers;
import cc.alcina.framework.servlet.component.traversal.SelectionTableArea;
import cc.alcina.framework.servlet.component.traversal.TraversalPlace;
import cc.alcina.framework.servlet.component.traversal.TraversalSettings;
import java.lang.Boolean;
import java.lang.Class;
import java.lang.Integer;
import java.lang.String;
import java.util.List;
import java.util.Set;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    static _Dotburger_Menu dotburger_menu = new _Dotburger_Menu();
    static _Page page = new _Page();
    static _Page_ActivityRoute page_activityRoute = new _Page_ActivityRoute();
    static _RenderedSelections renderedSelections = new _RenderedSelections();
    static _SelectionLayers_LayersContainer selectionLayers_layersContainer = new _SelectionLayers_LayersContainer();
    public static _TraversalBrowser_Ui traversalBrowser_ui = new _TraversalBrowser_Ui();
    public static _TraversalSettings traversalSettings = new _TraversalSettings();
    
    static class _Dotburger_Menu implements TypedProperty.Container {
      TypedProperty<Dotburger.Menu, Link> keyboardShortcuts = new TypedProperty<>(Dotburger.Menu.class, "keyboardShortcuts");
      TypedProperty<Dotburger.Menu, TraversalSettings.PropertyDisplayMode> propertyDisplayMode = new TypedProperty<>(Dotburger.Menu.class, "propertyDisplayMode");
      TypedProperty<Dotburger.Menu, TraversalSettings.SecondaryAreaDisplayMode> secondaryAreaDisplayMode = new TypedProperty<>(Dotburger.Menu.class, "secondaryAreaDisplayMode");
      TypedProperty<Dotburger.Menu, Heading> section1 = new TypedProperty<>(Dotburger.Menu.class, "section1");
      TypedProperty<Dotburger.Menu, Heading> section2 = new TypedProperty<>(Dotburger.Menu.class, "section2");
      TypedProperty<Dotburger.Menu, Heading> section3 = new TypedProperty<>(Dotburger.Menu.class, "section3");
      TypedProperty<Dotburger.Menu, Heading> section4 = new TypedProperty<>(Dotburger.Menu.class, "section4");
      TypedProperty<Dotburger.Menu, TraversalPlace.SelectionType> selectionType = new TypedProperty<>(Dotburger.Menu.class, "selectionType");
    }
    
    static class _Page implements TypedProperty.Container {
      TypedProperty<Page, Header> header = new TypedProperty<>(Page.class, "header");
      TypedProperty<Page, RemoteComponentObservables.ObservableHistory> history = new TypedProperty<>(Page.class, "history");
      TypedProperty<Page, RenderedSelections> input = new TypedProperty<>(Page.class, "input");
      TypedProperty<Page, SelectionLayers> layers = new TypedProperty<>(Page.class, "layers");
      TypedProperty<Page, RenderedSelections> output = new TypedProperty<>(Page.class, "output");
      TypedProperty<Page, PropertiesArea> propertiesArea = new TypedProperty<>(Page.class, "propertiesArea");
      TypedProperty<Page, RenderedSelections> table = new TypedProperty<>(Page.class, "table");
    }
    
    static class _Page_ActivityRoute implements TypedProperty.Container {
      TypedProperty<Page.ActivityRoute, Class> channel = new TypedProperty<>(Page.ActivityRoute.class, "channel");
      TypedProperty<Page.ActivityRoute, Page> page = new TypedProperty<>(Page.ActivityRoute.class, "page");
      TypedProperty<Page.ActivityRoute, BasePlace> place = new TypedProperty<>(Page.ActivityRoute.class, "place");
    }
    
    static class _RenderedSelections implements TypedProperty.Container {
      TypedProperty<RenderedSelections, Heading> heading = new TypedProperty<>(RenderedSelections.class, "heading");
      TypedProperty<RenderedSelections, Page> page = new TypedProperty<>(RenderedSelections.class, "page");
      TypedProperty<RenderedSelections, Selection> selection = new TypedProperty<>(RenderedSelections.class, "selection");
      TypedProperty<RenderedSelections, RenderedSelections.SelectionMarkupArea> selectionMarkupArea = new TypedProperty<>(RenderedSelections.class, "selectionMarkupArea");
      TypedProperty<RenderedSelections, SelectionTableArea> selectionTable = new TypedProperty<>(RenderedSelections.class, "selectionTable");
      TypedProperty<RenderedSelections, Model> style = new TypedProperty<>(RenderedSelections.class, "style");
      TypedProperty<RenderedSelections, TraversalSettings.SecondaryArea> variant = new TypedProperty<>(RenderedSelections.class, "variant");
    }
    
    static class _SelectionLayers_LayersContainer implements TypedProperty.Container {
      TypedProperty<SelectionLayers.LayersContainer, CollectionDeltaModel> collectionRepresentation = new TypedProperty<>(SelectionLayers.LayersContainer.class, "collectionRepresentation");
      TypedProperty<SelectionLayers.LayersContainer, List> layers = new TypedProperty<>(SelectionLayers.LayersContainer.class, "layers");
    }
    
    public static class _TraversalBrowser_Ui implements TypedProperty.Container {
      public TypedProperty<TraversalBrowser.Ui, Set> appCommandContexts = new TypedProperty<>(TraversalBrowser.Ui.class, "appCommandContexts");
      public TypedProperty<TraversalBrowser.Ui, CommandContext.Provider> commandContextProvider = new TypedProperty<>(TraversalBrowser.Ui.class, "commandContextProvider");
      public TypedProperty<TraversalBrowser.Ui, RemoteComponentObservables.ObservableHistory> history = new TypedProperty<>(TraversalBrowser.Ui.class, "history");
      public TypedProperty<TraversalBrowser.Ui, KeybindingsHandler> keybindingsHandler = new TypedProperty<>(TraversalBrowser.Ui.class, "keybindingsHandler");
      public TypedProperty<TraversalBrowser.Ui, String> mainCaption = new TypedProperty<>(TraversalBrowser.Ui.class, "mainCaption");
      public TypedProperty<TraversalBrowser.Ui, Page> page = new TypedProperty<>(TraversalBrowser.Ui.class, "page");
      public TypedProperty<TraversalBrowser.Ui, TraversalPlace> place = new TypedProperty<>(TraversalBrowser.Ui.class, "place");
      public TypedProperty<TraversalBrowser.Ui, Layer> selectedLayer0 = new TypedProperty<>(TraversalBrowser.Ui.class, "selectedLayer0");
      public TypedProperty<TraversalBrowser.Ui, SelectionMarkup> selectionMarkup = new TypedProperty<>(TraversalBrowser.Ui.class, "selectionMarkup");
      public TypedProperty<TraversalBrowser.Ui, TraversalSettings> settings = new TypedProperty<>(TraversalBrowser.Ui.class, "settings");
      public TypedProperty<TraversalBrowser.Ui, SelectionTraversal> traversal = new TypedProperty<>(TraversalBrowser.Ui.class, "traversal");
      public TypedProperty<TraversalBrowser.Ui, String> traversalPath = new TypedProperty<>(TraversalBrowser.Ui.class, "traversalPath");
      public TypedProperty<TraversalBrowser.Ui, Boolean> useSelectionSegmentPath = new TypedProperty<>(TraversalBrowser.Ui.class, "useSelectionSegmentPath");
    }
    
    public static class _TraversalSettings implements TypedProperty.Container {
      public TypedProperty<TraversalSettings, Boolean> descentSelectionIncludesSecondaryRelations = new TypedProperty<>(TraversalSettings.class, "descentSelectionIncludesSecondaryRelations");
      public TypedProperty<TraversalSettings, TraversalSettings.PropertyDisplayMode> propertyDisplayMode = new TypedProperty<>(TraversalSettings.class, "propertyDisplayMode");
      public TypedProperty<TraversalSettings, TraversalSettings.SecondaryAreaDisplayMode> secondaryAreaDisplayMode = new TypedProperty<>(TraversalSettings.class, "secondaryAreaDisplayMode");
      public TypedProperty<TraversalSettings, Boolean> showContainerLayers = new TypedProperty<>(TraversalSettings.class, "showContainerLayers");
      public TypedProperty<TraversalSettings, Integer> tableRows = new TypedProperty<>(TraversalSettings.class, "tableRows");
    }
    
//@formatter:on
}
