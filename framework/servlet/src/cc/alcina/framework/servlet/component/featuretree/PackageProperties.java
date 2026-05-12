package cc.alcina.framework.servlet.component.featuretree;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeybindingsHandler;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.servlet.component.featuretree.FeaturePlace;
import cc.alcina.framework.servlet.component.featuretree.Properties;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponent;
import java.lang.String;
import java.util.Set;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    public static _FeatureTree_Ui featureTree_ui = new _FeatureTree_Ui();
    static _Properties properties = new _Properties();
    
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
