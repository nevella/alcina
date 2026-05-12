package cc.alcina.framework.servlet.component.featuretree;

import java.util.Set;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeybindingsHandler;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponent;

public class PackageProperties {
	// auto-generated, do not modify
	//@formatter:off
    
    static _FeatureTable featureTable = new _FeatureTable();
    public static _FeatureTree_Ui featureTree_ui = new _FeatureTree_Ui();
    static _Page_ActivityRoute page_activityRoute = new _Page_ActivityRoute();
    static _Properties properties = new _Properties();
    
    static class _FeatureTable implements TypedProperty.Container {
      TypedProperty<FeatureTable, FeatureTable.Features> features = new TypedProperty<>(FeatureTable.class, "features");
      TypedProperty<FeatureTable, FeaturePlace> lastTablePlace = new TypedProperty<>(FeatureTable.class, "lastTablePlace");
      TypedProperty<FeatureTable, Table> table = new TypedProperty<>(FeatureTable.class, "table");
      static class InstanceProperties extends 	InstanceProperty.Container<FeatureTable> {
         InstanceProperties(FeatureTable source){super(source);}
        InstanceProperty<FeatureTable, FeatureTable.Features> features(){return new InstanceProperty<>(source,PackageProperties.featureTable.features);}
        InstanceProperty<FeatureTable, FeaturePlace> lastTablePlace(){return new InstanceProperty<>(source,PackageProperties.featureTable.lastTablePlace);}
        InstanceProperty<FeatureTable, Table> table(){return new InstanceProperty<>(source,PackageProperties.featureTable.table);}
      }
      
       InstanceProperties instance(FeatureTable instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _FeatureTree_Ui implements TypedProperty.Container {
      public TypedProperty<FeatureTree.Ui, Set> appCommandContexts = new TypedProperty<>(FeatureTree.Ui.class, "appCommandContexts");
      public TypedProperty<FeatureTree.Ui, CommandContext.Provider> commandContextProvider = new TypedProperty<>(FeatureTree.Ui.class, "commandContextProvider");
      public TypedProperty<FeatureTree.Ui, KeybindingsHandler> keybindingsHandler = new TypedProperty<>(FeatureTree.Ui.class, "keybindingsHandler");
      public TypedProperty<FeatureTree.Ui, String> mainCaption = new TypedProperty<>(FeatureTree.Ui.class, "mainCaption");
      public TypedProperty<FeatureTree.Ui, FeaturePlace> place = new TypedProperty<>(FeatureTree.Ui.class, "place");
      public TypedProperty<FeatureTree.Ui, RemoteComponent> remoteComponent = new TypedProperty<>(FeatureTree.Ui.class, "remoteComponent");
      public static class InstanceProperties extends 	InstanceProperty.Container<FeatureTree.Ui> {
        public  InstanceProperties(FeatureTree.Ui source){super(source);}
        public InstanceProperty<FeatureTree.Ui, Set> appCommandContexts(){return new InstanceProperty<>(source,PackageProperties.featureTree_ui.appCommandContexts);}
        public InstanceProperty<FeatureTree.Ui, CommandContext.Provider> commandContextProvider(){return new InstanceProperty<>(source,PackageProperties.featureTree_ui.commandContextProvider);}
        public InstanceProperty<FeatureTree.Ui, KeybindingsHandler> keybindingsHandler(){return new InstanceProperty<>(source,PackageProperties.featureTree_ui.keybindingsHandler);}
        public InstanceProperty<FeatureTree.Ui, String> mainCaption(){return new InstanceProperty<>(source,PackageProperties.featureTree_ui.mainCaption);}
        public InstanceProperty<FeatureTree.Ui, FeaturePlace> place(){return new InstanceProperty<>(source,PackageProperties.featureTree_ui.place);}
        public InstanceProperty<FeatureTree.Ui, RemoteComponent> remoteComponent(){return new InstanceProperty<>(source,PackageProperties.featureTree_ui.remoteComponent);}
      }
      
      public  InstanceProperties instance(FeatureTree.Ui instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _Page_ActivityRoute implements TypedProperty.Container {
      TypedProperty<Page.ActivityRoute, Class> channel = new TypedProperty<>(Page.ActivityRoute.class, "channel");
      TypedProperty<Page.ActivityRoute, Page> page = new TypedProperty<>(Page.ActivityRoute.class, "page");
      TypedProperty<Page.ActivityRoute, BasePlace> place = new TypedProperty<>(Page.ActivityRoute.class, "place");
      static class InstanceProperties extends 	InstanceProperty.Container<Page.ActivityRoute> {
         InstanceProperties(Page.ActivityRoute source){super(source);}
        InstanceProperty<Page.ActivityRoute, Class> channel(){return new InstanceProperty<>(source,PackageProperties.page_activityRoute.channel);}
        InstanceProperty<Page.ActivityRoute, Page> page(){return new InstanceProperty<>(source,PackageProperties.page_activityRoute.page);}
        InstanceProperty<Page.ActivityRoute, BasePlace> place(){return new InstanceProperty<>(source,PackageProperties.page_activityRoute.place);}
      }
      
       InstanceProperties instance(Page.ActivityRoute instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _Properties implements TypedProperty.Container {
      TypedProperty<Properties, Heading> header = new TypedProperty<>(Properties.class, "header");
      TypedProperty<Properties, Properties.FeatureProperties> properties = new TypedProperty<>(Properties.class, "properties");
      static class InstanceProperties extends 	InstanceProperty.Container<Properties> {
         InstanceProperties(Properties source){super(source);}
        InstanceProperty<Properties, Heading> header(){return new InstanceProperty<>(source,PackageProperties.properties.header);}
        InstanceProperty<Properties, Properties.FeatureProperties> properties(){return new InstanceProperty<>(source,PackageProperties.properties.properties);}
      }
      
       InstanceProperties instance(Properties instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
