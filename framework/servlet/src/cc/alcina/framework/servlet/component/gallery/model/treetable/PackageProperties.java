package cc.alcina.framework.servlet.component.gallery.model.treetable;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
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
      static class InstanceProperties extends InstanceProperty.Container<TreeTableArea> {
         InstanceProperties(TreeTableArea source){super(source);}
        InstanceProperty<TreeTableArea, GalleryPlace> place(){return new InstanceProperty<>(source,PackageProperties.treeTableArea.place);}
        InstanceProperty<TreeTableArea, TreeTable> treeTable(){return new InstanceProperty<>(source,PackageProperties.treeTableArea.treeTable);}
      }
      
       InstanceProperties instance(TreeTableArea instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _TreeTableArea_Attributes implements TypedProperty.Container {
      TypedProperty<TreeTableArea.Attributes, TreeTableArea.Colour> colour = new TypedProperty<>(TreeTableArea.Attributes.class, "colour");
      TypedProperty<TreeTableArea.Attributes, String> description = new TypedProperty<>(TreeTableArea.Attributes.class, "description");
      TypedProperty<TreeTableArea.Attributes, Integer> limbs = new TypedProperty<>(TreeTableArea.Attributes.class, "limbs");
      static class InstanceProperties extends InstanceProperty.Container<TreeTableArea.Attributes> {
         InstanceProperties(TreeTableArea.Attributes source){super(source);}
        InstanceProperty<TreeTableArea.Attributes, TreeTableArea.Colour> colour(){return new InstanceProperty<>(source,PackageProperties.treeTableArea_attributes.colour);}
        InstanceProperty<TreeTableArea.Attributes, String> description(){return new InstanceProperty<>(source,PackageProperties.treeTableArea_attributes.description);}
        InstanceProperty<TreeTableArea.Attributes, Integer> limbs(){return new InstanceProperty<>(source,PackageProperties.treeTableArea_attributes.limbs);}
      }
      
       InstanceProperties instance(TreeTableArea.Attributes instance) {
        return new InstanceProperties( instance);
      }
      
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
      static class InstanceProperties extends InstanceProperty.Container<TreeTableArea.TreeOfLifeNode> {
         InstanceProperties(TreeTableArea.TreeOfLifeNode source){super(source);}
        InstanceProperty<TreeTableArea.TreeOfLifeNode, List> children(){return new InstanceProperty<>(source,PackageProperties.treeTableArea_treeOfLifeNode.children);}
        InstanceProperty<TreeTableArea.TreeOfLifeNode, TreeTableArea.Attributes> contents(){return new InstanceProperty<>(source,PackageProperties.treeTableArea_treeOfLifeNode.contents);}
        InstanceProperty<TreeTableArea.TreeOfLifeNode, Boolean> keyboardSelected(){return new InstanceProperty<>(source,PackageProperties.treeTableArea_treeOfLifeNode.keyboardSelected);}
        InstanceProperty<TreeTableArea.TreeOfLifeNode, Tree.TreeNode.NodeLabel> label(){return new InstanceProperty<>(source,PackageProperties.treeTableArea_treeOfLifeNode.label);}
        InstanceProperty<TreeTableArea.TreeOfLifeNode, Boolean> leaf(){return new InstanceProperty<>(source,PackageProperties.treeTableArea_treeOfLifeNode.leaf);}
        InstanceProperty<TreeTableArea.TreeOfLifeNode, Boolean> open(){return new InstanceProperty<>(source,PackageProperties.treeTableArea_treeOfLifeNode.open);}
        InstanceProperty<TreeTableArea.TreeOfLifeNode, TreeTableArea.TreeOfLifeNode> parent(){return new InstanceProperty<>(source,PackageProperties.treeTableArea_treeOfLifeNode.parent);}
        InstanceProperty<TreeTableArea.TreeOfLifeNode, Boolean> selected(){return new InstanceProperty<>(source,PackageProperties.treeTableArea_treeOfLifeNode.selected);}
      }
      
       InstanceProperties instance(TreeTableArea.TreeOfLifeNode instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
