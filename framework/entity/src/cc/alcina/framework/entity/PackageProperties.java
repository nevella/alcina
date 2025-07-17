package cc.alcina.framework.entity;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.gwt.client.dirndl.model.Tree;
import java.lang.Boolean;
import java.util.List;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    public static _Configuration_PropertyTree_PropertyNode configuration_propertyTree_propertyNode = new _Configuration_PropertyTree_PropertyNode();
    
    public static class _Configuration_PropertyTree_PropertyNode implements TypedProperty.Container {
      public TypedProperty<Configuration.PropertyTree.PropertyNode, List> children = new TypedProperty<>(Configuration.PropertyTree.PropertyNode.class, "children");
      public TypedProperty<Configuration.PropertyTree.PropertyNode, Bindable> contents = new TypedProperty<>(Configuration.PropertyTree.PropertyNode.class, "contents");
      public TypedProperty<Configuration.PropertyTree.PropertyNode, Boolean> keyboardSelected = new TypedProperty<>(Configuration.PropertyTree.PropertyNode.class, "keyboardSelected");
      public TypedProperty<Configuration.PropertyTree.PropertyNode, Tree.TreeNode.NodeLabel> label = new TypedProperty<>(Configuration.PropertyTree.PropertyNode.class, "label");
      public TypedProperty<Configuration.PropertyTree.PropertyNode, Boolean> leaf = new TypedProperty<>(Configuration.PropertyTree.PropertyNode.class, "leaf");
      public TypedProperty<Configuration.PropertyTree.PropertyNode, Boolean> open = new TypedProperty<>(Configuration.PropertyTree.PropertyNode.class, "open");
      public TypedProperty<Configuration.PropertyTree.PropertyNode, Configuration.PropertyTree.PropertyNode> parent = new TypedProperty<>(Configuration.PropertyTree.PropertyNode.class, "parent");
      public TypedProperty<Configuration.PropertyTree.PropertyNode, Boolean> selected = new TypedProperty<>(Configuration.PropertyTree.PropertyNode.class, "selected");
      public static class InstanceProperties extends InstanceProperty.Container<Configuration.PropertyTree.PropertyNode> {
        public  InstanceProperties(Configuration.PropertyTree.PropertyNode source){super(source);}
        public InstanceProperty<Configuration.PropertyTree.PropertyNode, List> children(){return new InstanceProperty<>(source,PackageProperties.configuration_propertyTree_propertyNode.children);}
        public InstanceProperty<Configuration.PropertyTree.PropertyNode, Bindable> contents(){return new InstanceProperty<>(source,PackageProperties.configuration_propertyTree_propertyNode.contents);}
        public InstanceProperty<Configuration.PropertyTree.PropertyNode, Boolean> keyboardSelected(){return new InstanceProperty<>(source,PackageProperties.configuration_propertyTree_propertyNode.keyboardSelected);}
        public InstanceProperty<Configuration.PropertyTree.PropertyNode, Tree.TreeNode.NodeLabel> label(){return new InstanceProperty<>(source,PackageProperties.configuration_propertyTree_propertyNode.label);}
        public InstanceProperty<Configuration.PropertyTree.PropertyNode, Boolean> leaf(){return new InstanceProperty<>(source,PackageProperties.configuration_propertyTree_propertyNode.leaf);}
        public InstanceProperty<Configuration.PropertyTree.PropertyNode, Boolean> open(){return new InstanceProperty<>(source,PackageProperties.configuration_propertyTree_propertyNode.open);}
        public InstanceProperty<Configuration.PropertyTree.PropertyNode, Configuration.PropertyTree.PropertyNode> parent(){return new InstanceProperty<>(source,PackageProperties.configuration_propertyTree_propertyNode.parent);}
        public InstanceProperty<Configuration.PropertyTree.PropertyNode, Boolean> selected(){return new InstanceProperty<>(source,PackageProperties.configuration_propertyTree_propertyNode.selected);}
      }
      
      public  InstanceProperties instance(Configuration.PropertyTree.PropertyNode instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
