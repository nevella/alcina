package cc.alcina.framework.servlet.component.featuretree;

import java.util.Set;

import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeybindingsHandler;

public class PackageProperties {
	// auto-generated, do not modify
	//@formatter:off
    
    public static _FeatureTree_Ui featureTree_ui = new _FeatureTree_Ui();
    
    public static class _FeatureTree_Ui implements TypedProperty.Container {
      public TypedProperty<FeatureTree.Ui, Set> appCommandContexts = new TypedProperty<>(FeatureTree.Ui.class, "appCommandContexts");
      public TypedProperty<FeatureTree.Ui, CommandContext.Provider> commandContextProvider = new TypedProperty<>(FeatureTree.Ui.class, "commandContextProvider");
      public TypedProperty<FeatureTree.Ui, KeybindingsHandler> keybindingsHandler = new TypedProperty<>(FeatureTree.Ui.class, "keybindingsHandler");
      public TypedProperty<FeatureTree.Ui, String> mainCaption = new TypedProperty<>(FeatureTree.Ui.class, "mainCaption");
      public TypedProperty<FeatureTree.Ui, FeaturePlace> place = new TypedProperty<>(FeatureTree.Ui.class, "place");
    }
    
//@formatter:on
}
