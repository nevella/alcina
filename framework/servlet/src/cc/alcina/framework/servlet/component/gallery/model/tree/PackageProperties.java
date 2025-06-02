package cc.alcina.framework.servlet.component.gallery.model.tree;

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
    }
    
//@formatter:on
}
