package cc.alcina.framework.servlet.component.sequence;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.common.client.service.InstanceOracle;
import cc.alcina.framework.common.client.util.Timer;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeybindingsHandler;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.servlet.component.sequence.DetailArea;
import cc.alcina.framework.servlet.component.sequence.Header;
import cc.alcina.framework.servlet.component.sequence.HighlightModel;
import cc.alcina.framework.servlet.component.sequence.Page;
import cc.alcina.framework.servlet.component.sequence.Sequence;
import cc.alcina.framework.servlet.component.sequence.SequenceArea;
import cc.alcina.framework.servlet.component.sequence.SequenceBrowser;
import cc.alcina.framework.servlet.component.sequence.SequencePlace;
import cc.alcina.framework.servlet.component.sequence.SequenceSettings;
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
    
    public static _AppSuggestorSequence appSuggestorSequence = new _AppSuggestorSequence();
    static _Dotburger_Menu dotburger_menu = new _Dotburger_Menu();
    static _Header header = new _Header();
    static _Header_Left header_left = new _Header_Left();
    static _Page page = new _Page();
    static _Page_ActivityRoute page_activityRoute = new _Page_ActivityRoute();
    static _SequenceArea sequenceArea = new _SequenceArea();
    static _SequenceBrowser_Ui sequenceBrowser_ui = new _SequenceBrowser_Ui();
    public static _SequenceSettings sequenceSettings = new _SequenceSettings();
    
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
      TypedProperty<Page, DetailArea> detailArea = new TypedProperty<>(Page.class, "detailArea");
      TypedProperty<Page, List> filteredSequenceElements = new TypedProperty<>(Page.class, "filteredSequenceElements");
      TypedProperty<Page, Header> header = new TypedProperty<>(Page.class, "header");
      TypedProperty<Page, HighlightModel> highlightModel = new TypedProperty<>(Page.class, "highlightModel");
      TypedProperty<Page, SequencePlace> lastFilterTestPlace = new TypedProperty<>(Page.class, "lastFilterTestPlace");
      TypedProperty<Page, SequencePlace> lastHighlightTestPlace = new TypedProperty<>(Page.class, "lastHighlightTestPlace");
      TypedProperty<Page, SequencePlace> lastSelectedIndexChangePlace = new TypedProperty<>(Page.class, "lastSelectedIndexChangePlace");
      TypedProperty<Page, Timer> observableObservedTimer = new TypedProperty<>(Page.class, "observableObservedTimer");
      TypedProperty<Page, InstanceOracle.Query> oracleQuery = new TypedProperty<>(Page.class, "oracleQuery");
      TypedProperty<Page, Sequence> sequence = new TypedProperty<>(Page.class, "sequence");
      TypedProperty<Page, SequenceArea> sequenceArea = new TypedProperty<>(Page.class, "sequenceArea");
      TypedProperty<Page, StyleElement> styleElement = new TypedProperty<>(Page.class, "styleElement");
      TypedProperty<Page, SequenceBrowser.Ui> ui = new TypedProperty<>(Page.class, "ui");
      static class InstanceProperties extends InstanceProperty.Container<Page> {
         InstanceProperties(Page source){super(source);}
        InstanceProperty<Page, DetailArea> detailArea(){return new InstanceProperty<>(source,PackageProperties.page.detailArea);}
        InstanceProperty<Page, List> filteredSequenceElements(){return new InstanceProperty<>(source,PackageProperties.page.filteredSequenceElements);}
        InstanceProperty<Page, Header> header(){return new InstanceProperty<>(source,PackageProperties.page.header);}
        InstanceProperty<Page, HighlightModel> highlightModel(){return new InstanceProperty<>(source,PackageProperties.page.highlightModel);}
        InstanceProperty<Page, SequencePlace> lastFilterTestPlace(){return new InstanceProperty<>(source,PackageProperties.page.lastFilterTestPlace);}
        InstanceProperty<Page, SequencePlace> lastHighlightTestPlace(){return new InstanceProperty<>(source,PackageProperties.page.lastHighlightTestPlace);}
        InstanceProperty<Page, SequencePlace> lastSelectedIndexChangePlace(){return new InstanceProperty<>(source,PackageProperties.page.lastSelectedIndexChangePlace);}
        InstanceProperty<Page, Timer> observableObservedTimer(){return new InstanceProperty<>(source,PackageProperties.page.observableObservedTimer);}
        InstanceProperty<Page, InstanceOracle.Query> oracleQuery(){return new InstanceProperty<>(source,PackageProperties.page.oracleQuery);}
        InstanceProperty<Page, Sequence> sequence(){return new InstanceProperty<>(source,PackageProperties.page.sequence);}
        InstanceProperty<Page, SequenceArea> sequenceArea(){return new InstanceProperty<>(source,PackageProperties.page.sequenceArea);}
        InstanceProperty<Page, StyleElement> styleElement(){return new InstanceProperty<>(source,PackageProperties.page.styleElement);}
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
    
    static class _SequenceArea implements TypedProperty.Container {
      TypedProperty<SequenceArea, SequenceSettings.ColumnSet> columnSet = new TypedProperty<>(SequenceArea.class, "columnSet");
      TypedProperty<SequenceArea, List> filteredElements = new TypedProperty<>(SequenceArea.class, "filteredElements");
      TypedProperty<SequenceArea, Heading> header = new TypedProperty<>(SequenceArea.class, "header");
      TypedProperty<SequenceArea, Page> page = new TypedProperty<>(SequenceArea.class, "page");
      TypedProperty<SequenceArea, SequenceArea.RowsModelSupport> selectionSupport = new TypedProperty<>(SequenceArea.class, "selectionSupport");
      static class InstanceProperties extends InstanceProperty.Container<SequenceArea> {
         InstanceProperties(SequenceArea source){super(source);}
        InstanceProperty<SequenceArea, SequenceSettings.ColumnSet> columnSet(){return new InstanceProperty<>(source,PackageProperties.sequenceArea.columnSet);}
        InstanceProperty<SequenceArea, List> filteredElements(){return new InstanceProperty<>(source,PackageProperties.sequenceArea.filteredElements);}
        InstanceProperty<SequenceArea, Heading> header(){return new InstanceProperty<>(source,PackageProperties.sequenceArea.header);}
        InstanceProperty<SequenceArea, Page> page(){return new InstanceProperty<>(source,PackageProperties.sequenceArea.page);}
        InstanceProperty<SequenceArea, SequenceArea.RowsModelSupport> selectionSupport(){return new InstanceProperty<>(source,PackageProperties.sequenceArea.selectionSupport);}
      }
      
       InstanceProperties instance(SequenceArea instance) {
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
    
//@formatter:on
}
