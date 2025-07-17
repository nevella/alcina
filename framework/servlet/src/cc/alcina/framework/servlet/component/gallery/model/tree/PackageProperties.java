package cc.alcina.framework.servlet.component.gallery.model.tree;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.model.Tree;
import cc.alcina.framework.servlet.component.gallery.GalleryPlace;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    static _TreeArea treeArea = new _TreeArea();
    
    static class _TreeArea implements TypedProperty.Container {
      TypedProperty<TreeArea, GalleryPlace> place = new TypedProperty<>(TreeArea.class, "place");
      TypedProperty<TreeArea, Tree> tree = new TypedProperty<>(TreeArea.class, "tree");
      static class InstanceProperties extends InstanceProperty.Container<TreeArea> {
         InstanceProperties(TreeArea source){super(source);}
        InstanceProperty<TreeArea, GalleryPlace> place(){return new InstanceProperty<>(source,PackageProperties.treeArea.place);}
        InstanceProperty<TreeArea, Tree> tree(){return new InstanceProperty<>(source,PackageProperties.treeArea.tree);}
      }
      
       InstanceProperties instance(TreeArea instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
