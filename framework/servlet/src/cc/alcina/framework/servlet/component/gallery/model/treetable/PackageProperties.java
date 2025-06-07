package cc.alcina.framework.servlet.component.gallery.model.treetable;

import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.model.Tree;
import cc.alcina.framework.gwt.client.dirndl.model.TreeTable;
import cc.alcina.framework.servlet.component.gallery.GalleryPlace;
import cc.alcina.framework.servlet.component.gallery.model.treetable.TreeTableArea;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.String;
import java.util.List;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    static _TreeTableArea treeTableArea = new _TreeTableArea();
    static _TreeTableArea_Attributes treeTableArea_attributes = new _TreeTableArea_Attributes();
    static _TreeTableArea_TreeOfLifeNode treeTableArea_treeOfLifeNode = new _TreeTableArea_TreeOfLifeNode();
    
    static class _TreeTableArea implements TypedProperty.Container {
      TypedProperty<TreeTableArea, GalleryPlace> place = new TypedProperty<>(TreeTableArea.class, "place");
      TypedProperty<TreeTableArea, TreeTable> treeTable = new TypedProperty<>(TreeTableArea.class, "treeTable");
    }
    
    static class _TreeTableArea_Attributes implements TypedProperty.Container {
      TypedProperty<TreeTableArea.Attributes, TreeTableArea.Colour> colour = new TypedProperty<>(TreeTableArea.Attributes.class, "colour");
      TypedProperty<TreeTableArea.Attributes, String> description = new TypedProperty<>(TreeTableArea.Attributes.class, "description");
      TypedProperty<TreeTableArea.Attributes, Integer> limbs = new TypedProperty<>(TreeTableArea.Attributes.class, "limbs");
    }
    
    static class _TreeTableArea_TreeOfLifeNode implements TypedProperty.Container {
      TypedProperty<TreeTableArea.TreeOfLifeNode, List> children = new TypedProperty<>(TreeTableArea.TreeOfLifeNode.class, "children");
      TypedProperty<TreeTableArea.TreeOfLifeNode, TreeTableArea.Attributes> contents = new TypedProperty<>(TreeTableArea.TreeOfLifeNode.class, "contents");
      TypedProperty<TreeTableArea.TreeOfLifeNode, Boolean> keyboardSelected = new TypedProperty<>(TreeTableArea.TreeOfLifeNode.class, "keyboardSelected");
      TypedProperty<TreeTableArea.TreeOfLifeNode, Tree.TreeNode.NodeLabel> label = new TypedProperty<>(TreeTableArea.TreeOfLifeNode.class, "label");
      TypedProperty<TreeTableArea.TreeOfLifeNode, Boolean> leaf = new TypedProperty<>(TreeTableArea.TreeOfLifeNode.class, "leaf");
      TypedProperty<TreeTableArea.TreeOfLifeNode, Boolean> open = new TypedProperty<>(TreeTableArea.TreeOfLifeNode.class, "open");
      TypedProperty<TreeTableArea.TreeOfLifeNode, TreeTableArea.TreeOfLifeNode> parent = new TypedProperty<>(TreeTableArea.TreeOfLifeNode.class, "parent");
      TypedProperty<TreeTableArea.TreeOfLifeNode, Boolean> selected = new TypedProperty<>(TreeTableArea.TreeOfLifeNode.class, "selected");
    }
    
//@formatter:on
}
