package cc.alcina.framework.gwt.client.dirndl.model;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNodeContent;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.model.CollectionDeltaModel;
import cc.alcina.framework.gwt.client.dirndl.model.DomainViewTree;
import cc.alcina.framework.gwt.client.dirndl.model.HeadingActions;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel;
import cc.alcina.framework.gwt.client.dirndl.model.Tree;
import cc.alcina.framework.gwt.client.dirndl.model.TreePath;
import com.totsp.gwittir.client.ui.table.Field;
import java.lang.Boolean;
import java.lang.Class;
import java.lang.Object;
import java.lang.String;
import java.util.Collection;
import java.util.List;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    public static _CollectionDeltaModel collectionDeltaModel = new _CollectionDeltaModel();
    static _CollectionDeltaModel_RelativeInsert collectionDeltaModel_relativeInsert = new _CollectionDeltaModel_RelativeInsert();
    public static _DomainViewTree_DomainViewNode domainViewTree_domainViewNode = new _DomainViewTree_DomainViewNode();
    public static _HeadingActions headingActions = new _HeadingActions();
    public static _StandardModels_Panel standardModels_panel = new _StandardModels_Panel();
    public static _TableModel_TableColumn tableModel_tableColumn = new _TableModel_TableColumn();
    public static _TableModel_TableColumn_ColumnFilter tableModel_tableColumn_columnFilter = new _TableModel_TableColumn_ColumnFilter();
    public static _Tree_AbstractPathNode tree_abstractPathNode = new _Tree_AbstractPathNode();
    public static _Tree_PathNode tree_pathNode = new _Tree_PathNode();
    public static _Tree_TreeNode tree_treeNode = new _Tree_TreeNode();
    public static _Tree_TreeNode_BasicNode tree_treeNode_basicNode = new _Tree_TreeNode_BasicNode();
    
    public static class _CollectionDeltaModel implements TypedProperty.Container {
      public TypedProperty<CollectionDeltaModel, Collection> collection = new TypedProperty<>(CollectionDeltaModel.class, "collection");
      public TypedProperty<CollectionDeltaModel, CollectionDeltaModel.RelativeInsert> root = new TypedProperty<>(CollectionDeltaModel.class, "root");
    }
    
    static class _CollectionDeltaModel_RelativeInsert implements TypedProperty.Container {
      TypedProperty<CollectionDeltaModel.RelativeInsert, CollectionDeltaModel.RelativeInsert> after = new TypedProperty<>(CollectionDeltaModel.RelativeInsert.class, "after");
      TypedProperty<CollectionDeltaModel.RelativeInsert, CollectionDeltaModel.RelativeInsert> before = new TypedProperty<>(CollectionDeltaModel.RelativeInsert.class, "before");
      TypedProperty<CollectionDeltaModel.RelativeInsert, Object> collectionElement = new TypedProperty<>(CollectionDeltaModel.RelativeInsert.class, "collectionElement");
      TypedProperty<CollectionDeltaModel.RelativeInsert, Object> element = new TypedProperty<>(CollectionDeltaModel.RelativeInsert.class, "element");
      TypedProperty<CollectionDeltaModel.RelativeInsert, List> flushedContents = new TypedProperty<>(CollectionDeltaModel.RelativeInsert.class, "flushedContents");
    }
    
    public static class _DomainViewTree_DomainViewNode implements TypedProperty.Container {
      public TypedProperty<DomainViewTree.DomainViewNode, List> children = new TypedProperty<>(DomainViewTree.DomainViewNode.class, "children");
      public TypedProperty<DomainViewTree.DomainViewNode, Bindable> contents = new TypedProperty<>(DomainViewTree.DomainViewNode.class, "contents");
      public TypedProperty<DomainViewTree.DomainViewNode, Boolean> keyboardSelected = new TypedProperty<>(DomainViewTree.DomainViewNode.class, "keyboardSelected");
      public TypedProperty<DomainViewTree.DomainViewNode, Tree.TreeNode.NodeLabel> label = new TypedProperty<>(DomainViewTree.DomainViewNode.class, "label");
      public TypedProperty<DomainViewTree.DomainViewNode, Boolean> leaf = new TypedProperty<>(DomainViewTree.DomainViewNode.class, "leaf");
      public TypedProperty<DomainViewTree.DomainViewNode, DomainViewNodeContent> nodeContent = new TypedProperty<>(DomainViewTree.DomainViewNode.class, "nodeContent");
      public TypedProperty<DomainViewTree.DomainViewNode, Boolean> open = new TypedProperty<>(DomainViewTree.DomainViewNode.class, "open");
      public TypedProperty<DomainViewTree.DomainViewNode, DomainViewTree.DomainViewNode> parent = new TypedProperty<>(DomainViewTree.DomainViewNode.class, "parent");
      public TypedProperty<DomainViewTree.DomainViewNode, String> pathSegment = new TypedProperty<>(DomainViewTree.DomainViewNode.class, "pathSegment");
      public TypedProperty<DomainViewTree.DomainViewNode, Boolean> selected = new TypedProperty<>(DomainViewTree.DomainViewNode.class, "selected");
      public TypedProperty<DomainViewTree.DomainViewNode, TreePath> treePath = new TypedProperty<>(DomainViewTree.DomainViewNode.class, "treePath");
    }
    
    public static class _HeadingActions implements TypedProperty.Container {
      public TypedProperty<HeadingActions, List> actions = new TypedProperty<>(HeadingActions.class, "actions");
      public TypedProperty<HeadingActions, Object> heading = new TypedProperty<>(HeadingActions.class, "heading");
    }
    
    public static class _StandardModels_Panel implements TypedProperty.Container {
      public TypedProperty<StandardModels.Panel, HeadingActions> header = new TypedProperty<>(StandardModels.Panel.class, "header");
    }
    
    public static class _TableModel_TableColumn implements TypedProperty.Container {
      public TypedProperty<TableModel.TableColumn, String> caption = new TypedProperty<>(TableModel.TableColumn.class, "caption");
      public TypedProperty<TableModel.TableColumn, TableModel.TableColumn.ColumnFilter> columnFilter = new TypedProperty<>(TableModel.TableColumn.class, "columnFilter");
      public TypedProperty<TableModel.TableColumn, Field> field = new TypedProperty<>(TableModel.TableColumn.class, "field");
      public TypedProperty<TableModel.TableColumn, TableModel.SortDirection> sortDirection = new TypedProperty<>(TableModel.TableColumn.class, "sortDirection");
      public TypedProperty<TableModel.TableColumn, Class> valueClass = new TypedProperty<>(TableModel.TableColumn.class, "valueClass");
    }
    
    public static class _TableModel_TableColumn_ColumnFilter implements TypedProperty.Container {
      public TypedProperty<TableModel.TableColumn.ColumnFilter, Boolean> filterOpen = new TypedProperty<>(TableModel.TableColumn.ColumnFilter.class, "filterOpen");
      public TypedProperty<TableModel.TableColumn.ColumnFilter, Boolean> filtered = new TypedProperty<>(TableModel.TableColumn.ColumnFilter.class, "filtered");
    }
    
    public static class _Tree_AbstractPathNode implements TypedProperty.Container {
      public TypedProperty<Tree.AbstractPathNode, List> children = new TypedProperty<>(Tree.AbstractPathNode.class, "children");
      public TypedProperty<Tree.AbstractPathNode, Bindable> contents = new TypedProperty<>(Tree.AbstractPathNode.class, "contents");
      public TypedProperty<Tree.AbstractPathNode, Boolean> keyboardSelected = new TypedProperty<>(Tree.AbstractPathNode.class, "keyboardSelected");
      public TypedProperty<Tree.AbstractPathNode, Tree.TreeNode.NodeLabel> label = new TypedProperty<>(Tree.AbstractPathNode.class, "label");
      public TypedProperty<Tree.AbstractPathNode, Boolean> leaf = new TypedProperty<>(Tree.AbstractPathNode.class, "leaf");
      public TypedProperty<Tree.AbstractPathNode, Boolean> open = new TypedProperty<>(Tree.AbstractPathNode.class, "open");
      public TypedProperty<Tree.AbstractPathNode, Tree.AbstractPathNode> parent = new TypedProperty<>(Tree.AbstractPathNode.class, "parent");
      public TypedProperty<Tree.AbstractPathNode, Boolean> selected = new TypedProperty<>(Tree.AbstractPathNode.class, "selected");
      public TypedProperty<Tree.AbstractPathNode, TreePath> treePath = new TypedProperty<>(Tree.AbstractPathNode.class, "treePath");
    }
    
    public static class _Tree_PathNode implements TypedProperty.Container {
      public TypedProperty<Tree.PathNode, List> children = new TypedProperty<>(Tree.PathNode.class, "children");
      public TypedProperty<Tree.PathNode, Bindable> contents = new TypedProperty<>(Tree.PathNode.class, "contents");
      public TypedProperty<Tree.PathNode, Boolean> keyboardSelected = new TypedProperty<>(Tree.PathNode.class, "keyboardSelected");
      public TypedProperty<Tree.PathNode, Tree.TreeNode.NodeLabel> label = new TypedProperty<>(Tree.PathNode.class, "label");
      public TypedProperty<Tree.PathNode, Boolean> leaf = new TypedProperty<>(Tree.PathNode.class, "leaf");
      public TypedProperty<Tree.PathNode, Boolean> open = new TypedProperty<>(Tree.PathNode.class, "open");
      public TypedProperty<Tree.PathNode, Tree.PathNode> parent = new TypedProperty<>(Tree.PathNode.class, "parent");
      public TypedProperty<Tree.PathNode, Boolean> selected = new TypedProperty<>(Tree.PathNode.class, "selected");
      public TypedProperty<Tree.PathNode, TreePath> treePath = new TypedProperty<>(Tree.PathNode.class, "treePath");
    }
    
    public static class _Tree_TreeNode implements TypedProperty.Container {
      public TypedProperty<Tree.TreeNode, List> children = new TypedProperty<>(Tree.TreeNode.class, "children");
      public TypedProperty<Tree.TreeNode, Bindable> contents = new TypedProperty<>(Tree.TreeNode.class, "contents");
      public TypedProperty<Tree.TreeNode, Boolean> keyboardSelected = new TypedProperty<>(Tree.TreeNode.class, "keyboardSelected");
      public TypedProperty<Tree.TreeNode, Tree.TreeNode.NodeLabel> label = new TypedProperty<>(Tree.TreeNode.class, "label");
      public TypedProperty<Tree.TreeNode, Boolean> leaf = new TypedProperty<>(Tree.TreeNode.class, "leaf");
      public TypedProperty<Tree.TreeNode, Boolean> open = new TypedProperty<>(Tree.TreeNode.class, "open");
      public TypedProperty<Tree.TreeNode, Tree.TreeNode> parent = new TypedProperty<>(Tree.TreeNode.class, "parent");
      public TypedProperty<Tree.TreeNode, Boolean> selected = new TypedProperty<>(Tree.TreeNode.class, "selected");
    }
    
    public static class _Tree_TreeNode_BasicNode implements TypedProperty.Container {
      public TypedProperty<Tree.TreeNode.BasicNode, List> children = new TypedProperty<>(Tree.TreeNode.BasicNode.class, "children");
      public TypedProperty<Tree.TreeNode.BasicNode, Bindable> contents = new TypedProperty<>(Tree.TreeNode.BasicNode.class, "contents");
      public TypedProperty<Tree.TreeNode.BasicNode, Boolean> keyboardSelected = new TypedProperty<>(Tree.TreeNode.BasicNode.class, "keyboardSelected");
      public TypedProperty<Tree.TreeNode.BasicNode, Tree.TreeNode.NodeLabel> label = new TypedProperty<>(Tree.TreeNode.BasicNode.class, "label");
      public TypedProperty<Tree.TreeNode.BasicNode, Boolean> leaf = new TypedProperty<>(Tree.TreeNode.BasicNode.class, "leaf");
      public TypedProperty<Tree.TreeNode.BasicNode, Boolean> open = new TypedProperty<>(Tree.TreeNode.BasicNode.class, "open");
      public TypedProperty<Tree.TreeNode.BasicNode, Tree.TreeNode.BasicNode> parent = new TypedProperty<>(Tree.TreeNode.BasicNode.class, "parent");
      public TypedProperty<Tree.TreeNode.BasicNode, Boolean> selected = new TypedProperty<>(Tree.TreeNode.BasicNode.class, "selected");
    }
    
//@formatter:on
}
