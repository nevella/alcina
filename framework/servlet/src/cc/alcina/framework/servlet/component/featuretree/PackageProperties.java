package cc.alcina.framework.servlet.component.featuretree;

import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeybindingsHandler;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.servlet.component.featuretree.FeaturePlace;
import cc.alcina.framework.servlet.component.featuretree.Page;
import java.lang.Class;
import java.lang.String;
import java.util.Set;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    public static _FeatureTree_Ui featureTree_ui = new _FeatureTree_Ui();
    static _Page_ActivityRoute page_activityRoute = new _Page_ActivityRoute();
    
    public static class _FeatureTree_Ui implements TypedProperty.Container {
      public TypedProperty<FeatureTree.Ui, Set> appCommandContexts = new TypedProperty<>(FeatureTree.Ui.class, "appCommandContexts");
      public TypedProperty<FeatureTree.Ui, CommandContext.Provider> commandContextProvider = new TypedProperty<>(FeatureTree.Ui.class, "commandContextProvider");
      public TypedProperty<FeatureTree.Ui, KeybindingsHandler> keybindingsHandler = new TypedProperty<>(FeatureTree.Ui.class, "keybindingsHandler");
      public TypedProperty<FeatureTree.Ui, String> mainCaption = new TypedProperty<>(FeatureTree.Ui.class, "mainCaption");
      public TypedProperty<FeatureTree.Ui, FeaturePlace> place = new TypedProperty<>(FeatureTree.Ui.class, "place");
    }
    
    static class _Page_ActivityRoute implements TypedProperty.Container {
      TypedProperty<Page.ActivityRoute, Class> channel = new TypedProperty<>(Page.ActivityRoute.class, "channel");
      TypedProperty<Page.ActivityRoute, Page> page = new TypedProperty<>(Page.ActivityRoute.class, "page");
      TypedProperty<Page.ActivityRoute, BasePlace> place = new TypedProperty<>(Page.ActivityRoute.class, "place");
    }
    
//@formatter:on
}
