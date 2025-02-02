package cc.alcina.framework.servlet.component.gallery;

import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeybindingsHandler;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
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
    
    static _Dotburger_Menu dotburger_menu = new _Dotburger_Menu();
    static _GalleryArea galleryArea = new _GalleryArea();
    static _GalleryBrowser_Ui galleryBrowser_ui = new _GalleryBrowser_Ui();
    public static _GalleryContents galleryContents = new _GalleryContents();
    static _GalleryPage galleryPage = new _GalleryPage();
    static _GalleryPage_ActivityRoute galleryPage_activityRoute = new _GalleryPage_ActivityRoute();
    
    static class _Dotburger_Menu implements TypedProperty.Container {
      TypedProperty<Dotburger.Menu, Link> keyboardShortcuts = new TypedProperty<>(Dotburger.Menu.class, "keyboardShortcuts");
      TypedProperty<Dotburger.Menu, Heading> section4 = new TypedProperty<>(Dotburger.Menu.class, "section4");
    }
    
    static class _GalleryArea implements TypedProperty.Container {
      TypedProperty<GalleryArea, GalleryContents> contents = new TypedProperty<>(GalleryArea.class, "contents");
      TypedProperty<GalleryArea, GalleryPage> page = new TypedProperty<>(GalleryArea.class, "page");
    }
    
    static class _GalleryBrowser_Ui implements TypedProperty.Container {
      TypedProperty<GalleryBrowser.Ui, Set> appCommandContexts = new TypedProperty<>(GalleryBrowser.Ui.class, "appCommandContexts");
      TypedProperty<GalleryBrowser.Ui, CommandContext.Provider> commandContextProvider = new TypedProperty<>(GalleryBrowser.Ui.class, "commandContextProvider");
      TypedProperty<GalleryBrowser.Ui, KeybindingsHandler> keybindingsHandler = new TypedProperty<>(GalleryBrowser.Ui.class, "keybindingsHandler");
      TypedProperty<GalleryBrowser.Ui, String> mainCaption = new TypedProperty<>(GalleryBrowser.Ui.class, "mainCaption");
      TypedProperty<GalleryBrowser.Ui, GalleryPage> page = new TypedProperty<>(GalleryBrowser.Ui.class, "page");
      TypedProperty<GalleryBrowser.Ui, GalleryPlace> place = new TypedProperty<>(GalleryBrowser.Ui.class, "place");
      TypedProperty<GalleryBrowser.Ui, GallerySettings> settings = new TypedProperty<>(GalleryBrowser.Ui.class, "settings");
    }
    
    public static class _GalleryContents implements TypedProperty.Container {
      public TypedProperty<GalleryContents, GalleryPlace> place = new TypedProperty<>(GalleryContents.class, "place");
    }
    
    static class _GalleryPage implements TypedProperty.Container {
      TypedProperty<GalleryPage, GalleryArea> galleryArea = new TypedProperty<>(GalleryPage.class, "galleryArea");
      TypedProperty<GalleryPage, Header> header = new TypedProperty<>(GalleryPage.class, "header");
      TypedProperty<GalleryPage, GalleryBrowser.Ui> ui = new TypedProperty<>(GalleryPage.class, "ui");
    }
    
    static class _GalleryPage_ActivityRoute implements TypedProperty.Container {
      TypedProperty<GalleryPage.ActivityRoute, Class> channel = new TypedProperty<>(GalleryPage.ActivityRoute.class, "channel");
      TypedProperty<GalleryPage.ActivityRoute, GalleryPage> page = new TypedProperty<>(GalleryPage.ActivityRoute.class, "page");
      TypedProperty<GalleryPage.ActivityRoute, BasePlace> place = new TypedProperty<>(GalleryPage.ActivityRoute.class, "place");
    }
    
//@formatter:on
}
