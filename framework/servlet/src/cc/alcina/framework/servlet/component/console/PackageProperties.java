package cc.alcina.framework.servlet.component.console;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeybindingsHandler;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.servlet.component.console.Header;
import cc.alcina.framework.servlet.component.console.ServerConsoleArea;
import cc.alcina.framework.servlet.component.console.ServerConsoleBrowser;
import cc.alcina.framework.servlet.component.console.ServerConsoleContents;
import cc.alcina.framework.servlet.component.console.ServerConsolePage;
import cc.alcina.framework.servlet.component.console.ServerConsolePlace;
import cc.alcina.framework.servlet.component.console.ServerConsoleSettings;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponent;
import java.lang.Boolean;
import java.lang.Class;
import java.lang.Object;
import java.lang.String;
import java.util.Set;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    public static _AppSuggestorServerConsole appSuggestorServerConsole = new _AppSuggestorServerConsole();
    static _Dotburger_Menu dotburger_menu = new _Dotburger_Menu();
    static _Header header = new _Header();
    static _Header_Left header_left = new _Header_Left();
    static _ServerConsoleArea serverConsoleArea = new _ServerConsoleArea();
    static _ServerConsoleBrowser_Ui serverConsoleBrowser_ui = new _ServerConsoleBrowser_Ui();
    public static _ServerConsoleContents serverConsoleContents = new _ServerConsoleContents();
    static _ServerConsolePage serverConsolePage = new _ServerConsolePage();
    static _ServerConsolePage_ActivityRoute serverConsolePage_activityRoute = new _ServerConsolePage_ActivityRoute();
    public static _ServerConsoleSettings serverConsoleSettings = new _ServerConsoleSettings();
    
    public static class _AppSuggestorServerConsole implements TypedProperty.Container {
      public TypedProperty<AppSuggestorServerConsole, String> acceptedFilterText = new TypedProperty<>(AppSuggestorServerConsole.class, "acceptedFilterText");
      public TypedProperty<AppSuggestorServerConsole, String> filterText = new TypedProperty<>(AppSuggestorServerConsole.class, "filterText");
      public TypedProperty<AppSuggestorServerConsole, Suggestor> suggestor = new TypedProperty<>(AppSuggestorServerConsole.class, "suggestor");
      public static class InstanceProperties extends 	InstanceProperty.Container<AppSuggestorServerConsole> {
        public  InstanceProperties(AppSuggestorServerConsole source){super(source);}
        public InstanceProperty<AppSuggestorServerConsole, String> acceptedFilterText(){return new InstanceProperty<>(source,PackageProperties.appSuggestorServerConsole.acceptedFilterText);}
        public InstanceProperty<AppSuggestorServerConsole, String> filterText(){return new InstanceProperty<>(source,PackageProperties.appSuggestorServerConsole.filterText);}
        public InstanceProperty<AppSuggestorServerConsole, Suggestor> suggestor(){return new InstanceProperty<>(source,PackageProperties.appSuggestorServerConsole.suggestor);}
      }
      
      public  InstanceProperties instance(AppSuggestorServerConsole instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _Dotburger_Menu implements TypedProperty.Container {
      TypedProperty<Dotburger.Menu, Boolean> compactServerConsoleUi = new TypedProperty<>(Dotburger.Menu.class, "compactServerConsoleUi");
      TypedProperty<Dotburger.Menu, Link> keyboardShortcuts = new TypedProperty<>(Dotburger.Menu.class, "keyboardShortcuts");
      TypedProperty<Dotburger.Menu, Heading> section1 = new TypedProperty<>(Dotburger.Menu.class, "section1");
      TypedProperty<Dotburger.Menu, Heading> section4 = new TypedProperty<>(Dotburger.Menu.class, "section4");
      static class InstanceProperties extends 	InstanceProperty.Container<Dotburger.Menu> {
         InstanceProperties(Dotburger.Menu source){super(source);}
        InstanceProperty<Dotburger.Menu, Boolean> compactServerConsoleUi(){return new InstanceProperty<>(source,PackageProperties.dotburger_menu.compactServerConsoleUi);}
        InstanceProperty<Dotburger.Menu, Link> keyboardShortcuts(){return new InstanceProperty<>(source,PackageProperties.dotburger_menu.keyboardShortcuts);}
        InstanceProperty<Dotburger.Menu, Heading> section1(){return new InstanceProperty<>(source,PackageProperties.dotburger_menu.section1);}
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
      static class InstanceProperties extends 	InstanceProperty.Container<Header> {
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
      TypedProperty<Header.Left, Object> logo = new TypedProperty<>(Header.Left.class, "logo");
      static class InstanceProperties extends 	InstanceProperty.Container<Header.Left> {
         InstanceProperties(Header.Left source){super(source);}
        InstanceProperty<Header.Left, Object> logo(){return new InstanceProperty<>(source,PackageProperties.header_left.logo);}
      }
      
       InstanceProperties instance(Header.Left instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _ServerConsoleArea implements TypedProperty.Container {
      TypedProperty<ServerConsoleArea, Boolean> compactServerConsoleUi = new TypedProperty<>(ServerConsoleArea.class, "compactServerConsoleUi");
      TypedProperty<ServerConsoleArea, ServerConsoleContents> contents = new TypedProperty<>(ServerConsoleArea.class, "contents");
      TypedProperty<ServerConsoleArea, ServerConsolePage> page = new TypedProperty<>(ServerConsoleArea.class, "page");
      static class InstanceProperties extends 	InstanceProperty.Container<ServerConsoleArea> {
         InstanceProperties(ServerConsoleArea source){super(source);}
        InstanceProperty<ServerConsoleArea, Boolean> compactServerConsoleUi(){return new InstanceProperty<>(source,PackageProperties.serverConsoleArea.compactServerConsoleUi);}
        InstanceProperty<ServerConsoleArea, ServerConsoleContents> contents(){return new InstanceProperty<>(source,PackageProperties.serverConsoleArea.contents);}
        InstanceProperty<ServerConsoleArea, ServerConsolePage> page(){return new InstanceProperty<>(source,PackageProperties.serverConsoleArea.page);}
      }
      
       InstanceProperties instance(ServerConsoleArea instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _ServerConsoleBrowser_Ui implements TypedProperty.Container {
      TypedProperty<ServerConsoleBrowser.Ui, Set> appCommandContexts = new TypedProperty<>(ServerConsoleBrowser.Ui.class, "appCommandContexts");
      TypedProperty<ServerConsoleBrowser.Ui, Class> cacheableStringProviderClass = new TypedProperty<>(ServerConsoleBrowser.Ui.class, "cacheableStringProviderClass");
      TypedProperty<ServerConsoleBrowser.Ui, CommandContext.Provider> commandContextProvider = new TypedProperty<>(ServerConsoleBrowser.Ui.class, "commandContextProvider");
      TypedProperty<ServerConsoleBrowser.Ui, Boolean> domain = new TypedProperty<>(ServerConsoleBrowser.Ui.class, "domain");
      TypedProperty<ServerConsoleBrowser.Ui, KeybindingsHandler> keybindingsHandler = new TypedProperty<>(ServerConsoleBrowser.Ui.class, "keybindingsHandler");
      TypedProperty<ServerConsoleBrowser.Ui, String> mainCaption = new TypedProperty<>(ServerConsoleBrowser.Ui.class, "mainCaption");
      TypedProperty<ServerConsoleBrowser.Ui, ServerConsolePage> page = new TypedProperty<>(ServerConsoleBrowser.Ui.class, "page");
      TypedProperty<ServerConsoleBrowser.Ui, ServerConsolePlace> place = new TypedProperty<>(ServerConsoleBrowser.Ui.class, "place");
      TypedProperty<ServerConsoleBrowser.Ui, RemoteComponent> remoteComponent = new TypedProperty<>(ServerConsoleBrowser.Ui.class, "remoteComponent");
      TypedProperty<ServerConsoleBrowser.Ui, ServerConsoleSettings> settings = new TypedProperty<>(ServerConsoleBrowser.Ui.class, "settings");
      static class InstanceProperties extends 	InstanceProperty.Container<ServerConsoleBrowser.Ui> {
         InstanceProperties(ServerConsoleBrowser.Ui source){super(source);}
        InstanceProperty<ServerConsoleBrowser.Ui, Set> appCommandContexts(){return new InstanceProperty<>(source,PackageProperties.serverConsoleBrowser_ui.appCommandContexts);}
        InstanceProperty<ServerConsoleBrowser.Ui, Class> cacheableStringProviderClass(){return new InstanceProperty<>(source,PackageProperties.serverConsoleBrowser_ui.cacheableStringProviderClass);}
        InstanceProperty<ServerConsoleBrowser.Ui, CommandContext.Provider> commandContextProvider(){return new InstanceProperty<>(source,PackageProperties.serverConsoleBrowser_ui.commandContextProvider);}
        InstanceProperty<ServerConsoleBrowser.Ui, Boolean> domain(){return new InstanceProperty<>(source,PackageProperties.serverConsoleBrowser_ui.domain);}
        InstanceProperty<ServerConsoleBrowser.Ui, KeybindingsHandler> keybindingsHandler(){return new InstanceProperty<>(source,PackageProperties.serverConsoleBrowser_ui.keybindingsHandler);}
        InstanceProperty<ServerConsoleBrowser.Ui, String> mainCaption(){return new InstanceProperty<>(source,PackageProperties.serverConsoleBrowser_ui.mainCaption);}
        InstanceProperty<ServerConsoleBrowser.Ui, ServerConsolePage> page(){return new InstanceProperty<>(source,PackageProperties.serverConsoleBrowser_ui.page);}
        InstanceProperty<ServerConsoleBrowser.Ui, ServerConsolePlace> place(){return new InstanceProperty<>(source,PackageProperties.serverConsoleBrowser_ui.place);}
        InstanceProperty<ServerConsoleBrowser.Ui, RemoteComponent> remoteComponent(){return new InstanceProperty<>(source,PackageProperties.serverConsoleBrowser_ui.remoteComponent);}
        InstanceProperty<ServerConsoleBrowser.Ui, ServerConsoleSettings> settings(){return new InstanceProperty<>(source,PackageProperties.serverConsoleBrowser_ui.settings);}
      }
      
       InstanceProperties instance(ServerConsoleBrowser.Ui instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _ServerConsoleContents implements TypedProperty.Container {
      public TypedProperty<ServerConsoleContents, ServerConsolePlace> place = new TypedProperty<>(ServerConsoleContents.class, "place");
      public static class InstanceProperties extends 	InstanceProperty.Container<ServerConsoleContents> {
        public  InstanceProperties(ServerConsoleContents source){super(source);}
        public InstanceProperty<ServerConsoleContents, ServerConsolePlace> place(){return new InstanceProperty<>(source,PackageProperties.serverConsoleContents.place);}
      }
      
      public  InstanceProperties instance(ServerConsoleContents instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _ServerConsolePage implements TypedProperty.Container {
      TypedProperty<ServerConsolePage, Header> header = new TypedProperty<>(ServerConsolePage.class, "header");
      TypedProperty<ServerConsolePage, ServerConsoleArea> reportArea = new TypedProperty<>(ServerConsolePage.class, "reportArea");
      TypedProperty<ServerConsolePage, ServerConsoleBrowser.Ui> ui = new TypedProperty<>(ServerConsolePage.class, "ui");
      static class InstanceProperties extends 	InstanceProperty.Container<ServerConsolePage> {
         InstanceProperties(ServerConsolePage source){super(source);}
        InstanceProperty<ServerConsolePage, Header> header(){return new InstanceProperty<>(source,PackageProperties.serverConsolePage.header);}
        InstanceProperty<ServerConsolePage, ServerConsoleArea> reportArea(){return new InstanceProperty<>(source,PackageProperties.serverConsolePage.reportArea);}
        InstanceProperty<ServerConsolePage, ServerConsoleBrowser.Ui> ui(){return new InstanceProperty<>(source,PackageProperties.serverConsolePage.ui);}
      }
      
       InstanceProperties instance(ServerConsolePage instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _ServerConsolePage_ActivityRoute implements TypedProperty.Container {
      TypedProperty<ServerConsolePage.ActivityRoute, Class> channel = new TypedProperty<>(ServerConsolePage.ActivityRoute.class, "channel");
      TypedProperty<ServerConsolePage.ActivityRoute, ServerConsolePage> page = new TypedProperty<>(ServerConsolePage.ActivityRoute.class, "page");
      TypedProperty<ServerConsolePage.ActivityRoute, BasePlace> place = new TypedProperty<>(ServerConsolePage.ActivityRoute.class, "place");
      static class InstanceProperties extends 	InstanceProperty.Container<ServerConsolePage.ActivityRoute> {
         InstanceProperties(ServerConsolePage.ActivityRoute source){super(source);}
        InstanceProperty<ServerConsolePage.ActivityRoute, Class> channel(){return new InstanceProperty<>(source,PackageProperties.serverConsolePage_activityRoute.channel);}
        InstanceProperty<ServerConsolePage.ActivityRoute, ServerConsolePage> page(){return new InstanceProperty<>(source,PackageProperties.serverConsolePage_activityRoute.page);}
        InstanceProperty<ServerConsolePage.ActivityRoute, BasePlace> place(){return new InstanceProperty<>(source,PackageProperties.serverConsolePage_activityRoute.place);}
      }
      
       InstanceProperties instance(ServerConsolePage.ActivityRoute instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _ServerConsoleSettings implements TypedProperty.Container {
      public TypedProperty<ServerConsoleSettings, Boolean> compactServerConsoleUi = new TypedProperty<>(ServerConsoleSettings.class, "compactServerConsoleUi");
      public static class InstanceProperties extends 	InstanceProperty.Container<ServerConsoleSettings> {
        public  InstanceProperties(ServerConsoleSettings source){super(source);}
        public InstanceProperty<ServerConsoleSettings, Boolean> compactServerConsoleUi(){return new InstanceProperty<>(source,PackageProperties.serverConsoleSettings.compactServerConsoleUi);}
      }
      
      public  InstanceProperties instance(ServerConsoleSettings instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
