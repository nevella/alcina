package cc.alcina.framework.servlet.component.gallery;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeybindingsHandler;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.servlet.component.gallery.GalleryArea;
import cc.alcina.framework.servlet.component.gallery.GalleryBrowser;
import cc.alcina.framework.servlet.component.gallery.GalleryContents;
import cc.alcina.framework.servlet.component.gallery.GalleryPage;
import cc.alcina.framework.servlet.component.gallery.GalleryPlace;
import cc.alcina.framework.servlet.component.gallery.GallerySettings;
import cc.alcina.framework.servlet.component.gallery.Header;
import java.lang.Class;
import java.lang.String;
import java.util.Set;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    public static _AppSuggestorGallery appSuggestorGallery = new _AppSuggestorGallery();
    static _Dotburger_Menu dotburger_menu = new _Dotburger_Menu();
    static _GalleryArea galleryArea = new _GalleryArea();
    static _GalleryBrowser_Ui galleryBrowser_ui = new _GalleryBrowser_Ui();
    public static _GalleryContents galleryContents = new _GalleryContents();
    static _GalleryPage galleryPage = new _GalleryPage();
    static _GalleryPage_ActivityRoute galleryPage_activityRoute = new _GalleryPage_ActivityRoute();
    
    public static class _AppSuggestorGallery implements TypedProperty.Container {
      public TypedProperty<AppSuggestorGallery, String> acceptedFilterText = new TypedProperty<>(AppSuggestorGallery.class, "acceptedFilterText");
      public TypedProperty<AppSuggestorGallery, String> filterText = new TypedProperty<>(AppSuggestorGallery.class, "filterText");
      public TypedProperty<AppSuggestorGallery, Suggestor> suggestor = new TypedProperty<>(AppSuggestorGallery.class, "suggestor");
      public static class InstanceProperties extends InstanceProperty.Container<AppSuggestorGallery> {
        public  InstanceProperties(AppSuggestorGallery source){super(source);}
        public InstanceProperty<AppSuggestorGallery, String> acceptedFilterText(){return new InstanceProperty<>(source,PackageProperties.appSuggestorGallery.acceptedFilterText);}
        public InstanceProperty<AppSuggestorGallery, String> filterText(){return new InstanceProperty<>(source,PackageProperties.appSuggestorGallery.filterText);}
        public InstanceProperty<AppSuggestorGallery, Suggestor> suggestor(){return new InstanceProperty<>(source,PackageProperties.appSuggestorGallery.suggestor);}
      }
      
      public  InstanceProperties instance(AppSuggestorGallery instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _Dotburger_Menu implements TypedProperty.Container {
      TypedProperty<Dotburger.Menu, Link> keyboardShortcuts = new TypedProperty<>(Dotburger.Menu.class, "keyboardShortcuts");
      TypedProperty<Dotburger.Menu, Heading> section4 = new TypedProperty<>(Dotburger.Menu.class, "section4");
      static class InstanceProperties extends InstanceProperty.Container<Dotburger.Menu> {
         InstanceProperties(Dotburger.Menu source){super(source);}
        InstanceProperty<Dotburger.Menu, Link> keyboardShortcuts(){return new InstanceProperty<>(source,PackageProperties.dotburger_menu.keyboardShortcuts);}
        InstanceProperty<Dotburger.Menu, Heading> section4(){return new InstanceProperty<>(source,PackageProperties.dotburger_menu.section4);}
      }
      
       InstanceProperties instance(Dotburger.Menu instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _GalleryArea implements TypedProperty.Container {
      TypedProperty<GalleryArea, GalleryContents> contents = new TypedProperty<>(GalleryArea.class, "contents");
      TypedProperty<GalleryArea, GalleryPage> page = new TypedProperty<>(GalleryArea.class, "page");
      static class InstanceProperties extends InstanceProperty.Container<GalleryArea> {
         InstanceProperties(GalleryArea source){super(source);}
        InstanceProperty<GalleryArea, GalleryContents> contents(){return new InstanceProperty<>(source,PackageProperties.galleryArea.contents);}
        InstanceProperty<GalleryArea, GalleryPage> page(){return new InstanceProperty<>(source,PackageProperties.galleryArea.page);}
      }
      
       InstanceProperties instance(GalleryArea instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _GalleryBrowser_Ui implements TypedProperty.Container {
      TypedProperty<GalleryBrowser.Ui, Set> appCommandContexts = new TypedProperty<>(GalleryBrowser.Ui.class, "appCommandContexts");
      TypedProperty<GalleryBrowser.Ui, CommandContext.Provider> commandContextProvider = new TypedProperty<>(GalleryBrowser.Ui.class, "commandContextProvider");
      TypedProperty<GalleryBrowser.Ui, KeybindingsHandler> keybindingsHandler = new TypedProperty<>(GalleryBrowser.Ui.class, "keybindingsHandler");
      TypedProperty<GalleryBrowser.Ui, String> mainCaption = new TypedProperty<>(GalleryBrowser.Ui.class, "mainCaption");
      TypedProperty<GalleryBrowser.Ui, GalleryPage> page = new TypedProperty<>(GalleryBrowser.Ui.class, "page");
      TypedProperty<GalleryBrowser.Ui, GalleryPlace> place = new TypedProperty<>(GalleryBrowser.Ui.class, "place");
      TypedProperty<GalleryBrowser.Ui, GallerySettings> settings = new TypedProperty<>(GalleryBrowser.Ui.class, "settings");
      static class InstanceProperties extends InstanceProperty.Container<GalleryBrowser.Ui> {
         InstanceProperties(GalleryBrowser.Ui source){super(source);}
        InstanceProperty<GalleryBrowser.Ui, Set> appCommandContexts(){return new InstanceProperty<>(source,PackageProperties.galleryBrowser_ui.appCommandContexts);}
        InstanceProperty<GalleryBrowser.Ui, CommandContext.Provider> commandContextProvider(){return new InstanceProperty<>(source,PackageProperties.galleryBrowser_ui.commandContextProvider);}
        InstanceProperty<GalleryBrowser.Ui, KeybindingsHandler> keybindingsHandler(){return new InstanceProperty<>(source,PackageProperties.galleryBrowser_ui.keybindingsHandler);}
        InstanceProperty<GalleryBrowser.Ui, String> mainCaption(){return new InstanceProperty<>(source,PackageProperties.galleryBrowser_ui.mainCaption);}
        InstanceProperty<GalleryBrowser.Ui, GalleryPage> page(){return new InstanceProperty<>(source,PackageProperties.galleryBrowser_ui.page);}
        InstanceProperty<GalleryBrowser.Ui, GalleryPlace> place(){return new InstanceProperty<>(source,PackageProperties.galleryBrowser_ui.place);}
        InstanceProperty<GalleryBrowser.Ui, GallerySettings> settings(){return new InstanceProperty<>(source,PackageProperties.galleryBrowser_ui.settings);}
      }
      
       InstanceProperties instance(GalleryBrowser.Ui instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _GalleryContents implements TypedProperty.Container {
      public TypedProperty<GalleryContents, GalleryPlace> place = new TypedProperty<>(GalleryContents.class, "place");
      public static class InstanceProperties extends InstanceProperty.Container<GalleryContents> {
        public  InstanceProperties(GalleryContents source){super(source);}
        public InstanceProperty<GalleryContents, GalleryPlace> place(){return new InstanceProperty<>(source,PackageProperties.galleryContents.place);}
      }
      
      public  InstanceProperties instance(GalleryContents instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _GalleryPage implements TypedProperty.Container {
      TypedProperty<GalleryPage, GalleryArea> galleryArea = new TypedProperty<>(GalleryPage.class, "galleryArea");
      TypedProperty<GalleryPage, Header> header = new TypedProperty<>(GalleryPage.class, "header");
      TypedProperty<GalleryPage, GalleryBrowser.Ui> ui = new TypedProperty<>(GalleryPage.class, "ui");
      static class InstanceProperties extends InstanceProperty.Container<GalleryPage> {
         InstanceProperties(GalleryPage source){super(source);}
        InstanceProperty<GalleryPage, GalleryArea> galleryArea(){return new InstanceProperty<>(source,PackageProperties.galleryPage.galleryArea);}
        InstanceProperty<GalleryPage, Header> header(){return new InstanceProperty<>(source,PackageProperties.galleryPage.header);}
        InstanceProperty<GalleryPage, GalleryBrowser.Ui> ui(){return new InstanceProperty<>(source,PackageProperties.galleryPage.ui);}
      }
      
       InstanceProperties instance(GalleryPage instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _GalleryPage_ActivityRoute implements TypedProperty.Container {
      TypedProperty<GalleryPage.ActivityRoute, Class> channel = new TypedProperty<>(GalleryPage.ActivityRoute.class, "channel");
      TypedProperty<GalleryPage.ActivityRoute, GalleryPage> page = new TypedProperty<>(GalleryPage.ActivityRoute.class, "page");
      TypedProperty<GalleryPage.ActivityRoute, BasePlace> place = new TypedProperty<>(GalleryPage.ActivityRoute.class, "place");
      static class InstanceProperties extends InstanceProperty.Container<GalleryPage.ActivityRoute> {
         InstanceProperties(GalleryPage.ActivityRoute source){super(source);}
        InstanceProperty<GalleryPage.ActivityRoute, Class> channel(){return new InstanceProperty<>(source,PackageProperties.galleryPage_activityRoute.channel);}
        InstanceProperty<GalleryPage.ActivityRoute, GalleryPage> page(){return new InstanceProperty<>(source,PackageProperties.galleryPage_activityRoute.page);}
        InstanceProperty<GalleryPage.ActivityRoute, BasePlace> place(){return new InstanceProperty<>(source,PackageProperties.galleryPage_activityRoute.place);}
      }
      
       InstanceProperties instance(GalleryPage.ActivityRoute instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
