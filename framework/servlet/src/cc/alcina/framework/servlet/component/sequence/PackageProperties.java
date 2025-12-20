package cc.alcina.framework.servlet.component.sequence;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.common.client.service.InstanceOracle;
import cc.alcina.framework.common.client.util.Timer;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeybindingsHandler;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.servlet.component.sequence.DetailArea;
import cc.alcina.framework.servlet.component.sequence.Header;
import cc.alcina.framework.servlet.component.sequence.HighlightModel;
import cc.alcina.framework.servlet.component.sequence.Page;
import cc.alcina.framework.servlet.component.sequence.Sequence;
import cc.alcina.framework.servlet.component.sequence.SequenceArea;
import cc.alcina.framework.servlet.component.sequence.SequenceBrowser;
import cc.alcina.framework.servlet.component.sequence.SequenceComponent;
import cc.alcina.framework.servlet.component.sequence.SequencePlace;
import cc.alcina.framework.servlet.component.sequence.SequenceSettings;
import cc.alcina.framework.servlet.component.sequence.SequenceTable;
import com.google.gwt.dom.client.StyleElement;
import java.lang.Boolean;
import java.lang.Class;
import java.lang.Integer;
import java.lang.Runnable;
import java.lang.String;
import java.util.List;
import java.util.Set;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    public static _AppSuggestorSequence appSuggestorSequence = new _AppSuggestorSequence();
    static _Dotburger_Menu dotburger_menu = new _Dotburger_Menu();
    static _Header header = new _Header();
    static _Header_Left header_left = new _Header_Left();
    static _Page page = new _Page();
    static _Page_ActivityRoute page_activityRoute = new _Page_ActivityRoute();
    public static _SequenceArea sequenceArea = new _SequenceArea();
    static _SequenceBrowser_Ui sequenceBrowser_ui = new _SequenceBrowser_Ui();
    public static _SequenceComponent sequenceComponent = new _SequenceComponent();
    public static _SequenceSettings sequenceSettings = new _SequenceSettings();
    static _SequenceTable sequenceTable = new _SequenceTable();
    
    public static class _AppSuggestorSequence implements TypedProperty.Container {
      public TypedProperty<AppSuggestorSequence, String> acceptedFilterText = new TypedProperty<>(AppSuggestorSequence.class, "acceptedFilterText");
      public TypedProperty<AppSuggestorSequence, String> filterText = new TypedProperty<>(AppSuggestorSequence.class, "filterText");
      public TypedProperty<AppSuggestorSequence, Suggestor> suggestor = new TypedProperty<>(AppSuggestorSequence.class, "suggestor");
      public static class InstanceProperties extends InstanceProperty.Container<AppSuggestorSequence> {
        public  InstanceProperties(AppSuggestorSequence source){super(source);}
        public InstanceProperty<AppSuggestorSequence, String> acceptedFilterText(){return new InstanceProperty<>(source,PackageProperties.appSuggestorSequence.acceptedFilterText);}
        public InstanceProperty<AppSuggestorSequence, String> filterText(){return new InstanceProperty<>(source,PackageProperties.appSuggestorSequence.filterText);}
        public InstanceProperty<AppSuggestorSequence, Suggestor> suggestor(){return new InstanceProperty<>(source,PackageProperties.appSuggestorSequence.suggestor);}
      }
      
      public  InstanceProperties instance(AppSuggestorSequence instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _Dotburger_Menu implements TypedProperty.Container {
      TypedProperty<Dotburger.Menu, SequenceSettings.DetailDisplayMode> detailDisplayMode = new TypedProperty<>(Dotburger.Menu.class, "detailDisplayMode");
      TypedProperty<Dotburger.Menu, Link> keyboardShortcuts = new TypedProperty<>(Dotburger.Menu.class, "keyboardShortcuts");
      TypedProperty<Dotburger.Menu, Heading> section2 = new TypedProperty<>(Dotburger.Menu.class, "section2");
      TypedProperty<Dotburger.Menu, Heading> section4 = new TypedProperty<>(Dotburger.Menu.class, "section4");
      static class InstanceProperties extends InstanceProperty.Container<Dotburger.Menu> {
         InstanceProperties(Dotburger.Menu source){super(source);}
        InstanceProperty<Dotburger.Menu, SequenceSettings.DetailDisplayMode> detailDisplayMode(){return new InstanceProperty<>(source,PackageProperties.dotburger_menu.detailDisplayMode);}
        InstanceProperty<Dotburger.Menu, Link> keyboardShortcuts(){return new InstanceProperty<>(source,PackageProperties.dotburger_menu.keyboardShortcuts);}
        InstanceProperty<Dotburger.Menu, Heading> section2(){return new InstanceProperty<>(source,PackageProperties.dotburger_menu.section2);}
        InstanceProperty<Dotburger.Menu, Heading> section4(){return new InstanceProperty<>(source,PackageProperties.dotburger_menu.section4);}
      }
      
       InstanceProperties instance(Dotburger.Menu instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _Header implements TypedProperty.Container {
      TypedProperty<Header, Header.Left> left = new TypedProperty<>(Header.class, "left");
      TypedProperty<Header, Header.Mid> mid = new TypedProperty<>(Header.class, "mid");
      TypedProperty<Header, Header.Right> right = new TypedProperty<>(Header.class, "right");
      static class InstanceProperties extends InstanceProperty.Container<Header> {
         InstanceProperties(Header source){super(source);}
        InstanceProperty<Header, Header.Left> left(){return new InstanceProperty<>(source,PackageProperties.header.left);}
        InstanceProperty<Header, Header.Mid> mid(){return new InstanceProperty<>(source,PackageProperties.header.mid);}
        InstanceProperty<Header, Header.Right> right(){return new InstanceProperty<>(source,PackageProperties.header.right);}
      }
      
       InstanceProperties instance(Header instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _Header_Left implements TypedProperty.Container {
      TypedProperty<Header.Left, String> filter = new TypedProperty<>(Header.Left.class, "filter");
      TypedProperty<Header.Left, String> highlight = new TypedProperty<>(Header.Left.class, "highlight");
      TypedProperty<Header.Left, String> name = new TypedProperty<>(Header.Left.class, "name");
      static class InstanceProperties extends InstanceProperty.Container<Header.Left> {
         InstanceProperties(Header.Left source){super(source);}
        InstanceProperty<Header.Left, String> filter(){return new InstanceProperty<>(source,PackageProperties.header_left.filter);}
        InstanceProperty<Header.Left, String> highlight(){return new InstanceProperty<>(source,PackageProperties.header_left.highlight);}
        InstanceProperty<Header.Left, String> name(){return new InstanceProperty<>(source,PackageProperties.header_left.name);}
      }
      
       InstanceProperties instance(Header.Left instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _Page implements TypedProperty.Container {
      TypedProperty<Page, SequenceArea> sequenceArea = new TypedProperty<>(Page.class, "sequenceArea");
      TypedProperty<Page, SequenceArea.Service> sequenceAreaService = new TypedProperty<>(Page.class, "sequenceAreaService");
      TypedProperty<Page, Page.SequenceAreaServiceImpl> serviceImpl = new TypedProperty<>(Page.class, "serviceImpl");
      TypedProperty<Page, SequenceBrowser.Ui> ui = new TypedProperty<>(Page.class, "ui");
      static class InstanceProperties extends InstanceProperty.Container<Page> {
         InstanceProperties(Page source){super(source);}
        InstanceProperty<Page, SequenceArea> sequenceArea(){return new InstanceProperty<>(source,PackageProperties.page.sequenceArea);}
        InstanceProperty<Page, SequenceArea.Service> sequenceAreaService(){return new InstanceProperty<>(source,PackageProperties.page.sequenceAreaService);}
        InstanceProperty<Page, Page.SequenceAreaServiceImpl> serviceImpl(){return new InstanceProperty<>(source,PackageProperties.page.serviceImpl);}
        InstanceProperty<Page, SequenceBrowser.Ui> ui(){return new InstanceProperty<>(source,PackageProperties.page.ui);}
      }
      
       InstanceProperties instance(Page instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _Page_ActivityRoute implements TypedProperty.Container {
      TypedProperty<Page.ActivityRoute, Class> channel = new TypedProperty<>(Page.ActivityRoute.class, "channel");
      TypedProperty<Page.ActivityRoute, Page> page = new TypedProperty<>(Page.ActivityRoute.class, "page");
      TypedProperty<Page.ActivityRoute, BasePlace> place = new TypedProperty<>(Page.ActivityRoute.class, "place");
      static class InstanceProperties extends InstanceProperty.Container<Page.ActivityRoute> {
         InstanceProperties(Page.ActivityRoute source){super(source);}
        InstanceProperty<Page.ActivityRoute, Class> channel(){return new InstanceProperty<>(source,PackageProperties.page_activityRoute.channel);}
        InstanceProperty<Page.ActivityRoute, Page> page(){return new InstanceProperty<>(source,PackageProperties.page_activityRoute.page);}
        InstanceProperty<Page.ActivityRoute, BasePlace> place(){return new InstanceProperty<>(source,PackageProperties.page_activityRoute.place);}
      }
      
       InstanceProperties instance(Page.ActivityRoute instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _SequenceArea implements TypedProperty.Container {
      public TypedProperty<SequenceArea, Model> definitionHeader = new TypedProperty<>(SequenceArea.class, "definitionHeader");
      public TypedProperty<SequenceArea, DetailArea> detailArea = new TypedProperty<>(SequenceArea.class, "detailArea");
      public TypedProperty<SequenceArea, List> filteredSequenceElements = new TypedProperty<>(SequenceArea.class, "filteredSequenceElements");
      public TypedProperty<SequenceArea, HighlightModel> highlightModel = new TypedProperty<>(SequenceArea.class, "highlightModel");
      public TypedProperty<SequenceArea, SequencePlace> lastFilterTestPlace = new TypedProperty<>(SequenceArea.class, "lastFilterTestPlace");
      public TypedProperty<SequenceArea, SequencePlace> lastHighlightTestPlace = new TypedProperty<>(SequenceArea.class, "lastHighlightTestPlace");
      public TypedProperty<SequenceArea, SequencePlace> lastSelectedIndexChangePlace = new TypedProperty<>(SequenceArea.class, "lastSelectedIndexChangePlace");
      public TypedProperty<SequenceArea, Timer> observableObservedTimer = new TypedProperty<>(SequenceArea.class, "observableObservedTimer");
      public TypedProperty<SequenceArea, InstanceOracle.Query> oracleQuery = new TypedProperty<>(SequenceArea.class, "oracleQuery");
      public TypedProperty<SequenceArea, Runnable> reloadSequenceLambda = new TypedProperty<>(SequenceArea.class, "reloadSequenceLambda");
      public TypedProperty<SequenceArea, Sequence> sequence = new TypedProperty<>(SequenceArea.class, "sequence");
      public TypedProperty<SequenceArea, SequenceTable> sequenceTable = new TypedProperty<>(SequenceArea.class, "sequenceTable");
      public TypedProperty<SequenceArea, SequenceArea.Service> service = new TypedProperty<>(SequenceArea.class, "service");
      public TypedProperty<SequenceArea, StyleElement> styleElement = new TypedProperty<>(SequenceArea.class, "styleElement");
      public TypedProperty<SequenceArea, Runnable> updateStylesLambda = new TypedProperty<>(SequenceArea.class, "updateStylesLambda");
      public static class InstanceProperties extends InstanceProperty.Container<SequenceArea> {
        public  InstanceProperties(SequenceArea source){super(source);}
        public InstanceProperty<SequenceArea, Model> definitionHeader(){return new InstanceProperty<>(source,PackageProperties.sequenceArea.definitionHeader);}
        public InstanceProperty<SequenceArea, DetailArea> detailArea(){return new InstanceProperty<>(source,PackageProperties.sequenceArea.detailArea);}
        public InstanceProperty<SequenceArea, List> filteredSequenceElements(){return new InstanceProperty<>(source,PackageProperties.sequenceArea.filteredSequenceElements);}
        public InstanceProperty<SequenceArea, HighlightModel> highlightModel(){return new InstanceProperty<>(source,PackageProperties.sequenceArea.highlightModel);}
        public InstanceProperty<SequenceArea, SequencePlace> lastFilterTestPlace(){return new InstanceProperty<>(source,PackageProperties.sequenceArea.lastFilterTestPlace);}
        public InstanceProperty<SequenceArea, SequencePlace> lastHighlightTestPlace(){return new InstanceProperty<>(source,PackageProperties.sequenceArea.lastHighlightTestPlace);}
        public InstanceProperty<SequenceArea, SequencePlace> lastSelectedIndexChangePlace(){return new InstanceProperty<>(source,PackageProperties.sequenceArea.lastSelectedIndexChangePlace);}
        public InstanceProperty<SequenceArea, Timer> observableObservedTimer(){return new InstanceProperty<>(source,PackageProperties.sequenceArea.observableObservedTimer);}
        public InstanceProperty<SequenceArea, InstanceOracle.Query> oracleQuery(){return new InstanceProperty<>(source,PackageProperties.sequenceArea.oracleQuery);}
        public InstanceProperty<SequenceArea, Runnable> reloadSequenceLambda(){return new InstanceProperty<>(source,PackageProperties.sequenceArea.reloadSequenceLambda);}
        public InstanceProperty<SequenceArea, Sequence> sequence(){return new InstanceProperty<>(source,PackageProperties.sequenceArea.sequence);}
        public InstanceProperty<SequenceArea, SequenceTable> sequenceTable(){return new InstanceProperty<>(source,PackageProperties.sequenceArea.sequenceTable);}
        public InstanceProperty<SequenceArea, SequenceArea.Service> service(){return new InstanceProperty<>(source,PackageProperties.sequenceArea.service);}
        public InstanceProperty<SequenceArea, StyleElement> styleElement(){return new InstanceProperty<>(source,PackageProperties.sequenceArea.styleElement);}
        public InstanceProperty<SequenceArea, Runnable> updateStylesLambda(){return new InstanceProperty<>(source,PackageProperties.sequenceArea.updateStylesLambda);}
      }
      
      public  InstanceProperties instance(SequenceArea instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _SequenceBrowser_Ui implements TypedProperty.Container {
      TypedProperty<SequenceBrowser.Ui, Set> appCommandContexts = new TypedProperty<>(SequenceBrowser.Ui.class, "appCommandContexts");
      TypedProperty<SequenceBrowser.Ui, CommandContext.Provider> commandContextProvider = new TypedProperty<>(SequenceBrowser.Ui.class, "commandContextProvider");
      TypedProperty<SequenceBrowser.Ui, Boolean> domain = new TypedProperty<>(SequenceBrowser.Ui.class, "domain");
      TypedProperty<SequenceBrowser.Ui, Boolean> isDomain = new TypedProperty<>(SequenceBrowser.Ui.class, "isDomain");
      TypedProperty<SequenceBrowser.Ui, KeybindingsHandler> keybindingsHandler = new TypedProperty<>(SequenceBrowser.Ui.class, "keybindingsHandler");
      TypedProperty<SequenceBrowser.Ui, String> mainCaption = new TypedProperty<>(SequenceBrowser.Ui.class, "mainCaption");
      TypedProperty<SequenceBrowser.Ui, Page> page = new TypedProperty<>(SequenceBrowser.Ui.class, "page");
      TypedProperty<SequenceBrowser.Ui, SequencePlace> place = new TypedProperty<>(SequenceBrowser.Ui.class, "place");
      TypedProperty<SequenceBrowser.Ui, SequenceSettings> settings = new TypedProperty<>(SequenceBrowser.Ui.class, "settings");
      static class InstanceProperties extends InstanceProperty.Container<SequenceBrowser.Ui> {
         InstanceProperties(SequenceBrowser.Ui source){super(source);}
        InstanceProperty<SequenceBrowser.Ui, Set> appCommandContexts(){return new InstanceProperty<>(source,PackageProperties.sequenceBrowser_ui.appCommandContexts);}
        InstanceProperty<SequenceBrowser.Ui, CommandContext.Provider> commandContextProvider(){return new InstanceProperty<>(source,PackageProperties.sequenceBrowser_ui.commandContextProvider);}
        InstanceProperty<SequenceBrowser.Ui, Boolean> domain(){return new InstanceProperty<>(source,PackageProperties.sequenceBrowser_ui.domain);}
        InstanceProperty<SequenceBrowser.Ui, Boolean> isDomain(){return new InstanceProperty<>(source,PackageProperties.sequenceBrowser_ui.isDomain);}
        InstanceProperty<SequenceBrowser.Ui, KeybindingsHandler> keybindingsHandler(){return new InstanceProperty<>(source,PackageProperties.sequenceBrowser_ui.keybindingsHandler);}
        InstanceProperty<SequenceBrowser.Ui, String> mainCaption(){return new InstanceProperty<>(source,PackageProperties.sequenceBrowser_ui.mainCaption);}
        InstanceProperty<SequenceBrowser.Ui, Page> page(){return new InstanceProperty<>(source,PackageProperties.sequenceBrowser_ui.page);}
        InstanceProperty<SequenceBrowser.Ui, SequencePlace> place(){return new InstanceProperty<>(source,PackageProperties.sequenceBrowser_ui.place);}
        InstanceProperty<SequenceBrowser.Ui, SequenceSettings> settings(){return new InstanceProperty<>(source,PackageProperties.sequenceBrowser_ui.settings);}
      }
      
       InstanceProperties instance(SequenceBrowser.Ui instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _SequenceComponent implements TypedProperty.Container {
      public TypedProperty<SequenceComponent, SequencePlace> place = new TypedProperty<>(SequenceComponent.class, "place");
      public TypedProperty<SequenceComponent, SequenceArea> sequenceArea = new TypedProperty<>(SequenceComponent.class, "sequenceArea");
      public TypedProperty<SequenceComponent, SequenceArea.Service> sequenceAreaService = new TypedProperty<>(SequenceComponent.class, "sequenceAreaService");
      public TypedProperty<SequenceComponent, SequenceSettings> sequenceSettings = new TypedProperty<>(SequenceComponent.class, "sequenceSettings");
      public TypedProperty<SequenceComponent, SequenceComponent.SequenceAreaServiceImpl> serviceImpl = new TypedProperty<>(SequenceComponent.class, "serviceImpl");
      public static class InstanceProperties extends InstanceProperty.Container<SequenceComponent> {
        public  InstanceProperties(SequenceComponent source){super(source);}
        public InstanceProperty<SequenceComponent, SequencePlace> place(){return new InstanceProperty<>(source,PackageProperties.sequenceComponent.place);}
        public InstanceProperty<SequenceComponent, SequenceArea> sequenceArea(){return new InstanceProperty<>(source,PackageProperties.sequenceComponent.sequenceArea);}
        public InstanceProperty<SequenceComponent, SequenceArea.Service> sequenceAreaService(){return new InstanceProperty<>(source,PackageProperties.sequenceComponent.sequenceAreaService);}
        public InstanceProperty<SequenceComponent, SequenceSettings> sequenceSettings(){return new InstanceProperty<>(source,PackageProperties.sequenceComponent.sequenceSettings);}
        public InstanceProperty<SequenceComponent, SequenceComponent.SequenceAreaServiceImpl> serviceImpl(){return new InstanceProperty<>(source,PackageProperties.sequenceComponent.serviceImpl);}
      }
      
      public  InstanceProperties instance(SequenceComponent instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _SequenceSettings implements TypedProperty.Container {
      public TypedProperty<SequenceSettings, SequenceSettings.ColumnSet> columnSet = new TypedProperty<>(SequenceSettings.class, "columnSet");
      public TypedProperty<SequenceSettings, SequenceSettings.DetailDisplayMode> detailDisplayMode = new TypedProperty<>(SequenceSettings.class, "detailDisplayMode");
      public TypedProperty<SequenceSettings, Integer> maxElementRows = new TypedProperty<>(SequenceSettings.class, "maxElementRows");
      public TypedProperty<SequenceSettings, String> sequenceKey = new TypedProperty<>(SequenceSettings.class, "sequenceKey");
      public static class InstanceProperties extends InstanceProperty.Container<SequenceSettings> {
        public  InstanceProperties(SequenceSettings source){super(source);}
        public InstanceProperty<SequenceSettings, SequenceSettings.ColumnSet> columnSet(){return new InstanceProperty<>(source,PackageProperties.sequenceSettings.columnSet);}
        public InstanceProperty<SequenceSettings, SequenceSettings.DetailDisplayMode> detailDisplayMode(){return new InstanceProperty<>(source,PackageProperties.sequenceSettings.detailDisplayMode);}
        public InstanceProperty<SequenceSettings, Integer> maxElementRows(){return new InstanceProperty<>(source,PackageProperties.sequenceSettings.maxElementRows);}
        public InstanceProperty<SequenceSettings, String> sequenceKey(){return new InstanceProperty<>(source,PackageProperties.sequenceSettings.sequenceKey);}
      }
      
      public  InstanceProperties instance(SequenceSettings instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _SequenceTable implements TypedProperty.Container {
      TypedProperty<SequenceTable, SequenceSettings.ColumnSet> columnSet = new TypedProperty<>(SequenceTable.class, "columnSet");
      TypedProperty<SequenceTable, List> filteredElements = new TypedProperty<>(SequenceTable.class, "filteredElements");
      TypedProperty<SequenceTable, Heading> header = new TypedProperty<>(SequenceTable.class, "header");
      TypedProperty<SequenceTable, SequenceTable.RowsModelSupport> selectionSupport = new TypedProperty<>(SequenceTable.class, "selectionSupport");
      TypedProperty<SequenceTable, SequenceArea> sequenceArea = new TypedProperty<>(SequenceTable.class, "sequenceArea");
      static class InstanceProperties extends InstanceProperty.Container<SequenceTable> {
         InstanceProperties(SequenceTable source){super(source);}
        InstanceProperty<SequenceTable, SequenceSettings.ColumnSet> columnSet(){return new InstanceProperty<>(source,PackageProperties.sequenceTable.columnSet);}
        InstanceProperty<SequenceTable, List> filteredElements(){return new InstanceProperty<>(source,PackageProperties.sequenceTable.filteredElements);}
        InstanceProperty<SequenceTable, Heading> header(){return new InstanceProperty<>(source,PackageProperties.sequenceTable.header);}
        InstanceProperty<SequenceTable, SequenceTable.RowsModelSupport> selectionSupport(){return new InstanceProperty<>(source,PackageProperties.sequenceTable.selectionSupport);}
        InstanceProperty<SequenceTable, SequenceArea> sequenceArea(){return new InstanceProperty<>(source,PackageProperties.sequenceTable.sequenceArea);}
      }
      
       InstanceProperties instance(SequenceTable instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
