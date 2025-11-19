package cc.alcina.framework.gwt.client.dirndl.model;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNodeContent;
import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.model.CollectionDeltaModel;
import cc.alcina.framework.gwt.client.dirndl.model.DomainViewTree;
import cc.alcina.framework.gwt.client.dirndl.model.FilteredChoices;
import cc.alcina.framework.gwt.client.dirndl.model.HeadingActions;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel;
import cc.alcina.framework.gwt.client.dirndl.model.Tree;
import cc.alcina.framework.gwt.client.dirndl.model.TreePath;
import com.totsp.gwittir.client.ui.table.Field;
import java.lang.Boolean;
import java.lang.Class;
import java.lang.Integer;
import java.lang.Object;
import java.lang.String;
import java.util.Collection;
import java.util.List;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    static _Choices_Category choices_category = new _Choices_Category();
    public static _Choices_Multiple choices_multiple = new _Choices_Multiple();
    public static _Choices_MultipleSelect choices_multipleSelect = new _Choices_MultipleSelect();
    public static _CollectionDeltaModel collectionDeltaModel = new _CollectionDeltaModel();
    static _CollectionDeltaModel_RelativeInsert collectionDeltaModel_relativeInsert = new _CollectionDeltaModel_RelativeInsert();
    public static _DomainViewTree_DomainViewNode domainViewTree_domainViewNode = new _DomainViewTree_DomainViewNode();
    public static _FilteredChoices filteredChoices = new _FilteredChoices();
    public static _HeadingActions headingActions = new _HeadingActions();
    public static _StandardModels_Panel standardModels_panel = new _StandardModels_Panel();
    public static _TableModel_TableColumn tableModel_tableColumn = new _TableModel_TableColumn();
    public static _TableModel_TableColumn_ColumnFilter tableModel_tableColumn_columnFilter = new _TableModel_TableColumn_ColumnFilter();
    public static _Toggle toggle = new _Toggle();
    public static _Tree_AbstractPathNode tree_abstractPathNode = new _Tree_AbstractPathNode();
    public static _Tree_PathNode tree_pathNode = new _Tree_PathNode();
    public static _Tree_TreeNode tree_treeNode = new _Tree_TreeNode();
    public static _Tree_TreeNode_BasicNode tree_treeNode_basicNode = new _Tree_TreeNode_BasicNode();
    
    static class _Choices_Category implements TypedProperty.Container {
      TypedProperty<Choices.Category, String> category = new TypedProperty<>(Choices.Category.class, "category");
      TypedProperty<Choices.Category, List> choices = new TypedProperty<>(Choices.Category.class, "choices");
      TypedProperty<Choices.Category, Boolean> filtered = new TypedProperty<>(Choices.Category.class, "filtered");
      static class InstanceProperties extends InstanceProperty.Container<Choices.Category> {
         InstanceProperties(Choices.Category source){super(source);}
        InstanceProperty<Choices.Category, String> category(){return new InstanceProperty<>(source,PackageProperties.choices_category.category);}
        InstanceProperty<Choices.Category, List> choices(){return new InstanceProperty<>(source,PackageProperties.choices_category.choices);}
        InstanceProperty<Choices.Category, Boolean> filtered(){return new InstanceProperty<>(source,PackageProperties.choices_category.filtered);}
      }
      
       InstanceProperties instance(Choices.Category instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _Choices_Multiple implements TypedProperty.Container {
      public TypedProperty<Choices.Multiple, List> choices = new TypedProperty<>(Choices.Multiple.class, "choices");
      public TypedProperty<Choices.Multiple, List> elements = new TypedProperty<>(Choices.Multiple.class, "elements");
      public TypedProperty<Choices.Multiple, Boolean> repeatableChoices = new TypedProperty<>(Choices.Multiple.class, "repeatableChoices");
      public TypedProperty<Choices.Multiple, List> selectedValues = new TypedProperty<>(Choices.Multiple.class, "selectedValues");
      public TypedProperty<Choices.Multiple, List> values = new TypedProperty<>(Choices.Multiple.class, "values");
      public static class InstanceProperties extends InstanceProperty.Container<Choices.Multiple> {
        public  InstanceProperties(Choices.Multiple source){super(source);}
        public InstanceProperty<Choices.Multiple, List> choices(){return new InstanceProperty<>(source,PackageProperties.choices_multiple.choices);}
        public InstanceProperty<Choices.Multiple, List> elements(){return new InstanceProperty<>(source,PackageProperties.choices_multiple.elements);}
        public InstanceProperty<Choices.Multiple, Boolean> repeatableChoices(){return new InstanceProperty<>(source,PackageProperties.choices_multiple.repeatableChoices);}
        public InstanceProperty<Choices.Multiple, List> selectedValues(){return new InstanceProperty<>(source,PackageProperties.choices_multiple.selectedValues);}
        public InstanceProperty<Choices.Multiple, List> values(){return new InstanceProperty<>(source,PackageProperties.choices_multiple.values);}
      }
      
      public  InstanceProperties instance(Choices.Multiple instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _Choices_MultipleSelect implements TypedProperty.Container {
      public TypedProperty<Choices.MultipleSelect, List> choices = new TypedProperty<>(Choices.MultipleSelect.class, "choices");
      public TypedProperty<Choices.MultipleSelect, List> elements = new TypedProperty<>(Choices.MultipleSelect.class, "elements");
      public TypedProperty<Choices.MultipleSelect, Boolean> multiple = new TypedProperty<>(Choices.MultipleSelect.class, "multiple");
      public TypedProperty<Choices.MultipleSelect, Boolean> repeatableChoices = new TypedProperty<>(Choices.MultipleSelect.class, "repeatableChoices");
      public TypedProperty<Choices.MultipleSelect, List> selectedValues = new TypedProperty<>(Choices.MultipleSelect.class, "selectedValues");
      public TypedProperty<Choices.MultipleSelect, List> unboundSelectedValues = new TypedProperty<>(Choices.MultipleSelect.class, "unboundSelectedValues");
      public TypedProperty<Choices.MultipleSelect, Class> valueTransformer = new TypedProperty<>(Choices.MultipleSelect.class, "valueTransformer");
      public TypedProperty<Choices.MultipleSelect, List> values = new TypedProperty<>(Choices.MultipleSelect.class, "values");
      public static class InstanceProperties extends InstanceProperty.Container<Choices.MultipleSelect> {
        public  InstanceProperties(Choices.MultipleSelect source){super(source);}
        public InstanceProperty<Choices.MultipleSelect, List> choices(){return new InstanceProperty<>(source,PackageProperties.choices_multipleSelect.choices);}
        public InstanceProperty<Choices.MultipleSelect, List> elements(){return new InstanceProperty<>(source,PackageProperties.choices_multipleSelect.elements);}
        public InstanceProperty<Choices.MultipleSelect, Boolean> multiple(){return new InstanceProperty<>(source,PackageProperties.choices_multipleSelect.multiple);}
        public InstanceProperty<Choices.MultipleSelect, Boolean> repeatableChoices(){return new InstanceProperty<>(source,PackageProperties.choices_multipleSelect.repeatableChoices);}
        public InstanceProperty<Choices.MultipleSelect, List> selectedValues(){return new InstanceProperty<>(source,PackageProperties.choices_multipleSelect.selectedValues);}
        public InstanceProperty<Choices.MultipleSelect, List> unboundSelectedValues(){return new InstanceProperty<>(source,PackageProperties.choices_multipleSelect.unboundSelectedValues);}
        public InstanceProperty<Choices.MultipleSelect, Class> valueTransformer(){return new InstanceProperty<>(source,PackageProperties.choices_multipleSelect.valueTransformer);}
        public InstanceProperty<Choices.MultipleSelect, List> values(){return new InstanceProperty<>(source,PackageProperties.choices_multipleSelect.values);}
      }
      
      public  InstanceProperties instance(Choices.MultipleSelect instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _CollectionDeltaModel implements TypedProperty.Container {
      public TypedProperty<CollectionDeltaModel, Collection> collection = new TypedProperty<>(CollectionDeltaModel.class, "collection");
      public TypedProperty<CollectionDeltaModel, CollectionDeltaModel.RelativeInsert> root = new TypedProperty<>(CollectionDeltaModel.class, "root");
      public static class InstanceProperties extends InstanceProperty.Container<CollectionDeltaModel> {
        public  InstanceProperties(CollectionDeltaModel source){super(source);}
        public InstanceProperty<CollectionDeltaModel, Collection> collection(){return new InstanceProperty<>(source,PackageProperties.collectionDeltaModel.collection);}
        public InstanceProperty<CollectionDeltaModel, CollectionDeltaModel.RelativeInsert> root(){return new InstanceProperty<>(source,PackageProperties.collectionDeltaModel.root);}
      }
      
      public  InstanceProperties instance(CollectionDeltaModel instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _CollectionDeltaModel_RelativeInsert implements TypedProperty.Container {
      TypedProperty<CollectionDeltaModel.RelativeInsert, CollectionDeltaModel.RelativeInsert> after = new TypedProperty<>(CollectionDeltaModel.RelativeInsert.class, "after");
      TypedProperty<CollectionDeltaModel.RelativeInsert, CollectionDeltaModel.RelativeInsert> before = new TypedProperty<>(CollectionDeltaModel.RelativeInsert.class, "before");
      TypedProperty<CollectionDeltaModel.RelativeInsert, Object> collectionElement = new TypedProperty<>(CollectionDeltaModel.RelativeInsert.class, "collectionElement");
      TypedProperty<CollectionDeltaModel.RelativeInsert, Object> element = new TypedProperty<>(CollectionDeltaModel.RelativeInsert.class, "element");
      TypedProperty<CollectionDeltaModel.RelativeInsert, List> flushedContents = new TypedProperty<>(CollectionDeltaModel.RelativeInsert.class, "flushedContents");
      static class InstanceProperties extends InstanceProperty.Container<CollectionDeltaModel.RelativeInsert> {
         InstanceProperties(CollectionDeltaModel.RelativeInsert source){super(source);}
        InstanceProperty<CollectionDeltaModel.RelativeInsert, CollectionDeltaModel.RelativeInsert> after(){return new InstanceProperty<>(source,PackageProperties.collectionDeltaModel_relativeInsert.after);}
        InstanceProperty<CollectionDeltaModel.RelativeInsert, CollectionDeltaModel.RelativeInsert> before(){return new InstanceProperty<>(source,PackageProperties.collectionDeltaModel_relativeInsert.before);}
        InstanceProperty<CollectionDeltaModel.RelativeInsert, Object> collectionElement(){return new InstanceProperty<>(source,PackageProperties.collectionDeltaModel_relativeInsert.collectionElement);}
        InstanceProperty<CollectionDeltaModel.RelativeInsert, Object> element(){return new InstanceProperty<>(source,PackageProperties.collectionDeltaModel_relativeInsert.element);}
        InstanceProperty<CollectionDeltaModel.RelativeInsert, List> flushedContents(){return new InstanceProperty<>(source,PackageProperties.collectionDeltaModel_relativeInsert.flushedContents);}
      }
      
       InstanceProperties instance(CollectionDeltaModel.RelativeInsert instance) {
        return new InstanceProperties( instance);
      }
      
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
      public static class InstanceProperties extends InstanceProperty.Container<DomainViewTree.DomainViewNode> {
        public  InstanceProperties(DomainViewTree.DomainViewNode source){super(source);}
        public InstanceProperty<DomainViewTree.DomainViewNode, List> children(){return new InstanceProperty<>(source,PackageProperties.domainViewTree_domainViewNode.children);}
        public InstanceProperty<DomainViewTree.DomainViewNode, Bindable> contents(){return new InstanceProperty<>(source,PackageProperties.domainViewTree_domainViewNode.contents);}
        public InstanceProperty<DomainViewTree.DomainViewNode, Boolean> keyboardSelected(){return new InstanceProperty<>(source,PackageProperties.domainViewTree_domainViewNode.keyboardSelected);}
        public InstanceProperty<DomainViewTree.DomainViewNode, Tree.TreeNode.NodeLabel> label(){return new InstanceProperty<>(source,PackageProperties.domainViewTree_domainViewNode.label);}
        public InstanceProperty<DomainViewTree.DomainViewNode, Boolean> leaf(){return new InstanceProperty<>(source,PackageProperties.domainViewTree_domainViewNode.leaf);}
        public InstanceProperty<DomainViewTree.DomainViewNode, DomainViewNodeContent> nodeContent(){return new InstanceProperty<>(source,PackageProperties.domainViewTree_domainViewNode.nodeContent);}
        public InstanceProperty<DomainViewTree.DomainViewNode, Boolean> open(){return new InstanceProperty<>(source,PackageProperties.domainViewTree_domainViewNode.open);}
        public InstanceProperty<DomainViewTree.DomainViewNode, DomainViewTree.DomainViewNode> parent(){return new InstanceProperty<>(source,PackageProperties.domainViewTree_domainViewNode.parent);}
        public InstanceProperty<DomainViewTree.DomainViewNode, String> pathSegment(){return new InstanceProperty<>(source,PackageProperties.domainViewTree_domainViewNode.pathSegment);}
        public InstanceProperty<DomainViewTree.DomainViewNode, Boolean> selected(){return new InstanceProperty<>(source,PackageProperties.domainViewTree_domainViewNode.selected);}
        public InstanceProperty<DomainViewTree.DomainViewNode, TreePath> treePath(){return new InstanceProperty<>(source,PackageProperties.domainViewTree_domainViewNode.treePath);}
      }
      
      public  InstanceProperties instance(DomainViewTree.DomainViewNode instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _FilteredChoices implements TypedProperty.Container {
      public TypedProperty<FilteredChoices, FilteredChoices.Filter> filter = new TypedProperty<>(FilteredChoices.class, "filter");
      public TypedProperty<FilteredChoices, List> value = new TypedProperty<>(FilteredChoices.class, "value");
      public static class InstanceProperties extends InstanceProperty.Container<FilteredChoices> {
        public  InstanceProperties(FilteredChoices source){super(source);}
        public InstanceProperty<FilteredChoices, FilteredChoices.Filter> filter(){return new InstanceProperty<>(source,PackageProperties.filteredChoices.filter);}
        public InstanceProperty<FilteredChoices, List> value(){return new InstanceProperty<>(source,PackageProperties.filteredChoices.value);}
      }
      
      public  InstanceProperties instance(FilteredChoices instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _HeadingActions implements TypedProperty.Container {
      public TypedProperty<HeadingActions, List> actions = new TypedProperty<>(HeadingActions.class, "actions");
      public TypedProperty<HeadingActions, Object> heading = new TypedProperty<>(HeadingActions.class, "heading");
      public static class InstanceProperties extends InstanceProperty.Container<HeadingActions> {
        public  InstanceProperties(HeadingActions source){super(source);}
        public InstanceProperty<HeadingActions, List> actions(){return new InstanceProperty<>(source,PackageProperties.headingActions.actions);}
        public InstanceProperty<HeadingActions, Object> heading(){return new InstanceProperty<>(source,PackageProperties.headingActions.heading);}
      }
      
      public  InstanceProperties instance(HeadingActions instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _StandardModels_Panel implements TypedProperty.Container {
      public TypedProperty<StandardModels.Panel, HeadingActions> header = new TypedProperty<>(StandardModels.Panel.class, "header");
      public static class InstanceProperties extends InstanceProperty.Container<StandardModels.Panel> {
        public  InstanceProperties(StandardModels.Panel source){super(source);}
        public InstanceProperty<StandardModels.Panel, HeadingActions> header(){return new InstanceProperty<>(source,PackageProperties.standardModels_panel.header);}
      }
      
      public  InstanceProperties instance(StandardModels.Panel instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _TableModel_TableColumn implements TypedProperty.Container {
      public TypedProperty<TableModel.TableColumn, String> caption = new TypedProperty<>(TableModel.TableColumn.class, "caption");
      public TypedProperty<TableModel.TableColumn, TableModel.TableColumn.ColumnFilter> columnFilter = new TypedProperty<>(TableModel.TableColumn.class, "columnFilter");
      public TypedProperty<TableModel.TableColumn, Field> field = new TypedProperty<>(TableModel.TableColumn.class, "field");
      public TypedProperty<TableModel.TableColumn, TableModel.SortDirection> sortDirection = new TypedProperty<>(TableModel.TableColumn.class, "sortDirection");
      public TypedProperty<TableModel.TableColumn, String> title = new TypedProperty<>(TableModel.TableColumn.class, "title");
      public TypedProperty<TableModel.TableColumn, Class> valueClass = new TypedProperty<>(TableModel.TableColumn.class, "valueClass");
      public static class InstanceProperties extends InstanceProperty.Container<TableModel.TableColumn> {
        public  InstanceProperties(TableModel.TableColumn source){super(source);}
        public InstanceProperty<TableModel.TableColumn, String> caption(){return new InstanceProperty<>(source,PackageProperties.tableModel_tableColumn.caption);}
        public InstanceProperty<TableModel.TableColumn, TableModel.TableColumn.ColumnFilter> columnFilter(){return new InstanceProperty<>(source,PackageProperties.tableModel_tableColumn.columnFilter);}
        public InstanceProperty<TableModel.TableColumn, Field> field(){return new InstanceProperty<>(source,PackageProperties.tableModel_tableColumn.field);}
        public InstanceProperty<TableModel.TableColumn, TableModel.SortDirection> sortDirection(){return new InstanceProperty<>(source,PackageProperties.tableModel_tableColumn.sortDirection);}
        public InstanceProperty<TableModel.TableColumn, String> title(){return new InstanceProperty<>(source,PackageProperties.tableModel_tableColumn.title);}
        public InstanceProperty<TableModel.TableColumn, Class> valueClass(){return new InstanceProperty<>(source,PackageProperties.tableModel_tableColumn.valueClass);}
      }
      
      public  InstanceProperties instance(TableModel.TableColumn instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _TableModel_TableColumn_ColumnFilter implements TypedProperty.Container {
      public TypedProperty<TableModel.TableColumn.ColumnFilter, Boolean> filterOpen = new TypedProperty<>(TableModel.TableColumn.ColumnFilter.class, "filterOpen");
      public TypedProperty<TableModel.TableColumn.ColumnFilter, Boolean> filtered = new TypedProperty<>(TableModel.TableColumn.ColumnFilter.class, "filtered");
      public static class InstanceProperties extends InstanceProperty.Container<TableModel.TableColumn.ColumnFilter> {
        public  InstanceProperties(TableModel.TableColumn.ColumnFilter source){super(source);}
        public InstanceProperty<TableModel.TableColumn.ColumnFilter, Boolean> filterOpen(){return new InstanceProperty<>(source,PackageProperties.tableModel_tableColumn_columnFilter.filterOpen);}
        public InstanceProperty<TableModel.TableColumn.ColumnFilter, Boolean> filtered(){return new InstanceProperty<>(source,PackageProperties.tableModel_tableColumn_columnFilter.filtered);}
      }
      
      public  InstanceProperties instance(TableModel.TableColumn.ColumnFilter instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _Toggle implements TypedProperty.Container {
      public TypedProperty<Toggle, Object> active = new TypedProperty<>(Toggle.class, "active");
      public TypedProperty<Toggle, Object> displayed = new TypedProperty<>(Toggle.class, "displayed");
      public TypedProperty<Toggle, Integer> displayedIndex = new TypedProperty<>(Toggle.class, "displayedIndex");
      public TypedProperty<Toggle, List> values = new TypedProperty<>(Toggle.class, "values");
      public static class InstanceProperties extends InstanceProperty.Container<Toggle> {
        public  InstanceProperties(Toggle source){super(source);}
        public InstanceProperty<Toggle, Object> active(){return new InstanceProperty<>(source,PackageProperties.toggle.active);}
        public InstanceProperty<Toggle, Object> displayed(){return new InstanceProperty<>(source,PackageProperties.toggle.displayed);}
        public InstanceProperty<Toggle, Integer> displayedIndex(){return new InstanceProperty<>(source,PackageProperties.toggle.displayedIndex);}
        public InstanceProperty<Toggle, List> values(){return new InstanceProperty<>(source,PackageProperties.toggle.values);}
      }
      
      public  InstanceProperties instance(Toggle instance) {
        return new InstanceProperties( instance);
      }
      
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
      public static class InstanceProperties extends InstanceProperty.Container<Tree.AbstractPathNode> {
        public  InstanceProperties(Tree.AbstractPathNode source){super(source);}
        public InstanceProperty<Tree.AbstractPathNode, List> children(){return new InstanceProperty<>(source,PackageProperties.tree_abstractPathNode.children);}
        public InstanceProperty<Tree.AbstractPathNode, Bindable> contents(){return new InstanceProperty<>(source,PackageProperties.tree_abstractPathNode.contents);}
        public InstanceProperty<Tree.AbstractPathNode, Boolean> keyboardSelected(){return new InstanceProperty<>(source,PackageProperties.tree_abstractPathNode.keyboardSelected);}
        public InstanceProperty<Tree.AbstractPathNode, Tree.TreeNode.NodeLabel> label(){return new InstanceProperty<>(source,PackageProperties.tree_abstractPathNode.label);}
        public InstanceProperty<Tree.AbstractPathNode, Boolean> leaf(){return new InstanceProperty<>(source,PackageProperties.tree_abstractPathNode.leaf);}
        public InstanceProperty<Tree.AbstractPathNode, Boolean> open(){return new InstanceProperty<>(source,PackageProperties.tree_abstractPathNode.open);}
        public InstanceProperty<Tree.AbstractPathNode, Tree.AbstractPathNode> parent(){return new InstanceProperty<>(source,PackageProperties.tree_abstractPathNode.parent);}
        public InstanceProperty<Tree.AbstractPathNode, Boolean> selected(){return new InstanceProperty<>(source,PackageProperties.tree_abstractPathNode.selected);}
        public InstanceProperty<Tree.AbstractPathNode, TreePath> treePath(){return new InstanceProperty<>(source,PackageProperties.tree_abstractPathNode.treePath);}
      }
      
      public  InstanceProperties instance(Tree.AbstractPathNode instance) {
        return new InstanceProperties( instance);
      }
      
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
      public static class InstanceProperties extends InstanceProperty.Container<Tree.PathNode> {
        public  InstanceProperties(Tree.PathNode source){super(source);}
        public InstanceProperty<Tree.PathNode, List> children(){return new InstanceProperty<>(source,PackageProperties.tree_pathNode.children);}
        public InstanceProperty<Tree.PathNode, Bindable> contents(){return new InstanceProperty<>(source,PackageProperties.tree_pathNode.contents);}
        public InstanceProperty<Tree.PathNode, Boolean> keyboardSelected(){return new InstanceProperty<>(source,PackageProperties.tree_pathNode.keyboardSelected);}
        public InstanceProperty<Tree.PathNode, Tree.TreeNode.NodeLabel> label(){return new InstanceProperty<>(source,PackageProperties.tree_pathNode.label);}
        public InstanceProperty<Tree.PathNode, Boolean> leaf(){return new InstanceProperty<>(source,PackageProperties.tree_pathNode.leaf);}
        public InstanceProperty<Tree.PathNode, Boolean> open(){return new InstanceProperty<>(source,PackageProperties.tree_pathNode.open);}
        public InstanceProperty<Tree.PathNode, Tree.PathNode> parent(){return new InstanceProperty<>(source,PackageProperties.tree_pathNode.parent);}
        public InstanceProperty<Tree.PathNode, Boolean> selected(){return new InstanceProperty<>(source,PackageProperties.tree_pathNode.selected);}
        public InstanceProperty<Tree.PathNode, TreePath> treePath(){return new InstanceProperty<>(source,PackageProperties.tree_pathNode.treePath);}
      }
      
      public  InstanceProperties instance(Tree.PathNode instance) {
        return new InstanceProperties( instance);
      }
      
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
      public static class InstanceProperties extends InstanceProperty.Container<Tree.TreeNode> {
        public  InstanceProperties(Tree.TreeNode source){super(source);}
        public InstanceProperty<Tree.TreeNode, List> children(){return new InstanceProperty<>(source,PackageProperties.tree_treeNode.children);}
        public InstanceProperty<Tree.TreeNode, Bindable> contents(){return new InstanceProperty<>(source,PackageProperties.tree_treeNode.contents);}
        public InstanceProperty<Tree.TreeNode, Boolean> keyboardSelected(){return new InstanceProperty<>(source,PackageProperties.tree_treeNode.keyboardSelected);}
        public InstanceProperty<Tree.TreeNode, Tree.TreeNode.NodeLabel> label(){return new InstanceProperty<>(source,PackageProperties.tree_treeNode.label);}
        public InstanceProperty<Tree.TreeNode, Boolean> leaf(){return new InstanceProperty<>(source,PackageProperties.tree_treeNode.leaf);}
        public InstanceProperty<Tree.TreeNode, Boolean> open(){return new InstanceProperty<>(source,PackageProperties.tree_treeNode.open);}
        public InstanceProperty<Tree.TreeNode, Tree.TreeNode> parent(){return new InstanceProperty<>(source,PackageProperties.tree_treeNode.parent);}
        public InstanceProperty<Tree.TreeNode, Boolean> selected(){return new InstanceProperty<>(source,PackageProperties.tree_treeNode.selected);}
      }
      
      public  InstanceProperties instance(Tree.TreeNode instance) {
        return new InstanceProperties( instance);
      }
      
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
      public static class InstanceProperties extends InstanceProperty.Container<Tree.TreeNode.BasicNode> {
        public  InstanceProperties(Tree.TreeNode.BasicNode source){super(source);}
        public InstanceProperty<Tree.TreeNode.BasicNode, List> children(){return new InstanceProperty<>(source,PackageProperties.tree_treeNode_basicNode.children);}
        public InstanceProperty<Tree.TreeNode.BasicNode, Bindable> contents(){return new InstanceProperty<>(source,PackageProperties.tree_treeNode_basicNode.contents);}
        public InstanceProperty<Tree.TreeNode.BasicNode, Boolean> keyboardSelected(){return new InstanceProperty<>(source,PackageProperties.tree_treeNode_basicNode.keyboardSelected);}
        public InstanceProperty<Tree.TreeNode.BasicNode, Tree.TreeNode.NodeLabel> label(){return new InstanceProperty<>(source,PackageProperties.tree_treeNode_basicNode.label);}
        public InstanceProperty<Tree.TreeNode.BasicNode, Boolean> leaf(){return new InstanceProperty<>(source,PackageProperties.tree_treeNode_basicNode.leaf);}
        public InstanceProperty<Tree.TreeNode.BasicNode, Boolean> open(){return new InstanceProperty<>(source,PackageProperties.tree_treeNode_basicNode.open);}
        public InstanceProperty<Tree.TreeNode.BasicNode, Tree.TreeNode.BasicNode> parent(){return new InstanceProperty<>(source,PackageProperties.tree_treeNode_basicNode.parent);}
        public InstanceProperty<Tree.TreeNode.BasicNode, Boolean> selected(){return new InstanceProperty<>(source,PackageProperties.tree_treeNode_basicNode.selected);}
      }
      
      public  InstanceProperties instance(Tree.TreeNode.BasicNode instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
