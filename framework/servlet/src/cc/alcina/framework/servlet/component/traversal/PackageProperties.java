package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentObservables;
import cc.alcina.framework.servlet.component.traversal.Header;
import cc.alcina.framework.servlet.component.traversal.Page;
import cc.alcina.framework.servlet.component.traversal.PropertiesArea;
import cc.alcina.framework.servlet.component.traversal.RenderedSelections;
import cc.alcina.framework.servlet.component.traversal.SelectionLayers;
import cc.alcina.framework.servlet.component.traversal.TraversalBrowser;
import cc.alcina.framework.servlet.component.traversal.TraversalSettings;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.String;
import java.util.Set;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    static _Dotburger_Menu dotburger_menu = new _Dotburger_Menu();
    static _Page page = new _Page();
    public static _TraversalBrowser_Ui traversalBrowser_ui = new _TraversalBrowser_Ui();
    public static _TraversalSettings traversalSettings = new _TraversalSettings();
    
    static class _Dotburger_Menu implements TypedProperty.Container {
      TypedProperty<Dotburger.Menu, TraversalSettings.InputOutputDisplayMode> ioDisplayMode = new TypedProperty<>(Dotburger.Menu.class, "ioDisplayMode");
      TypedProperty<Dotburger.Menu, TraversalSettings.PropertyDisplayMode> propertyDisplayMode = new TypedProperty<>(Dotburger.Menu.class, "propertyDisplayMode");
      TypedProperty<Dotburger.Menu, Heading> section1 = new TypedProperty<>(Dotburger.Menu.class, "section1");
      TypedProperty<Dotburger.Menu, Heading> section2 = new TypedProperty<>(Dotburger.Menu.class, "section2");
      TypedProperty<Dotburger.Menu, Heading> section3 = new TypedProperty<>(Dotburger.Menu.class, "section3");
      TypedProperty<Dotburger.Menu, TraversalPlace.SelectionType> selectionType = new TypedProperty<>(Dotburger.Menu.class, "selectionType");
    }
    
    static class _Page implements TypedProperty.Container {
      TypedProperty<Page, Header> header = new TypedProperty<>(Page.class, "header");
      TypedProperty<Page, RemoteComponentObservables.ObservableHistory> history = new TypedProperty<>(Page.class, "history");
      TypedProperty<Page, RenderedSelections> input = new TypedProperty<>(Page.class, "input");
      TypedProperty<Page, SelectionLayers> layers = new TypedProperty<>(Page.class, "layers");
      TypedProperty<Page, RenderedSelections> output = new TypedProperty<>(Page.class, "output");
      TypedProperty<Page, PropertiesArea> propertiesArea = new TypedProperty<>(Page.class, "propertiesArea");
      TypedProperty<Page, TraversalBrowser.Ui> ui = new TypedProperty<>(Page.class, "ui");
    }
    
    public static class _TraversalBrowser_Ui implements TypedProperty.Container {
      public TypedProperty<TraversalBrowser.Ui, Set> appCommandContexts = new TypedProperty<>(TraversalBrowser.Ui.class, "appCommandContexts");
      public TypedProperty<TraversalBrowser.Ui, CommandContext.Provider> commandContextProvider = new TypedProperty<>(TraversalBrowser.Ui.class, "commandContextProvider");
      public TypedProperty<TraversalBrowser.Ui, RemoteComponentObservables.ObservableHistory> history = new TypedProperty<>(TraversalBrowser.Ui.class, "history");
      public TypedProperty<TraversalBrowser.Ui, String> mainCaption = new TypedProperty<>(TraversalBrowser.Ui.class, "mainCaption");
      public TypedProperty<TraversalBrowser.Ui, Page> page = new TypedProperty<>(TraversalBrowser.Ui.class, "page");
      public TypedProperty<TraversalBrowser.Ui, TraversalPlace> place = new TypedProperty<>(TraversalBrowser.Ui.class, "place");
      public TypedProperty<TraversalBrowser.Ui, TraversalSettings> settings = new TypedProperty<>(TraversalBrowser.Ui.class, "settings");
      public TypedProperty<TraversalBrowser.Ui, String> traversalPath = new TypedProperty<>(TraversalBrowser.Ui.class, "traversalPath");
      public TypedProperty<TraversalBrowser.Ui, Boolean> useSelectionSegmentPath = new TypedProperty<>(TraversalBrowser.Ui.class, "useSelectionSegmentPath");
    }
    
    public static class _TraversalSettings implements TypedProperty.Container {
      public TypedProperty<TraversalSettings, Boolean> descentSelectionIncludesSecondaryRelations = new TypedProperty<>(TraversalSettings.class, "descentSelectionIncludesSecondaryRelations");
      public TypedProperty<TraversalSettings, TraversalSettings.InputOutputDisplayMode> inputOutputDisplayMode = new TypedProperty<>(TraversalSettings.class, "inputOutputDisplayMode");
      public TypedProperty<TraversalSettings, TraversalSettings.PropertyDisplayMode> propertyDisplayMode = new TypedProperty<>(TraversalSettings.class, "propertyDisplayMode");
      public TypedProperty<TraversalSettings, Boolean> showContainerLayers = new TypedProperty<>(TraversalSettings.class, "showContainerLayers");
      public TypedProperty<TraversalSettings, Integer> tableRows = new TypedProperty<>(TraversalSettings.class, "tableRows");
    }
    
//@formatter:on
}
