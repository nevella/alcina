package cc.alcina.framework.servlet.component.featuretree;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
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
      public static class InstanceProperties extends InstanceProperty.Container<FeatureTree.Ui> {
        public  InstanceProperties(FeatureTree.Ui source){super(source);}
        public InstanceProperty<FeatureTree.Ui, Set> appCommandContexts(){return new InstanceProperty<>(source,PackageProperties.featureTree_ui.appCommandContexts);}
        public InstanceProperty<FeatureTree.Ui, CommandContext.Provider> commandContextProvider(){return new InstanceProperty<>(source,PackageProperties.featureTree_ui.commandContextProvider);}
        public InstanceProperty<FeatureTree.Ui, KeybindingsHandler> keybindingsHandler(){return new InstanceProperty<>(source,PackageProperties.featureTree_ui.keybindingsHandler);}
        public InstanceProperty<FeatureTree.Ui, String> mainCaption(){return new InstanceProperty<>(source,PackageProperties.featureTree_ui.mainCaption);}
        public InstanceProperty<FeatureTree.Ui, FeaturePlace> place(){return new InstanceProperty<>(source,PackageProperties.featureTree_ui.place);}
      }
      
      public  InstanceProperties instance(FeatureTree.Ui instance) {
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
    
//@formatter:on
}
