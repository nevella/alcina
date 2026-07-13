package cc.alcina.framework.gwt.client.dirndl.model.search;

import cc.alcina.framework.common.client.domain.search.criterion.PropertyCriterion;
import cc.alcina.framework.common.client.domain.search.criterion.PropertyOrderCriterion;
import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.search.Searchable;
import cc.alcina.framework.gwt.client.dirndl.model.search.ValueEditor;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.String;
import java.util.List;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    public static _FilterEditor filterEditor = new _FilterEditor();
    public static _OperatorSelector operatorSelector = new _OperatorSelector();
    public static _OrderEditor orderEditor = new _OrderEditor();
    public static _SearchDefinitionEditor searchDefinitionEditor = new _SearchDefinitionEditor();
    public static _Searchable searchable = new _Searchable();
    static _Searchable_RenderedOperator searchable_renderedOperator = new _Searchable_RenderedOperator();
    
    public static class _FilterEditor implements TypedProperty.Container {
      public TypedProperty<FilterEditor, String> filterValue = new TypedProperty<>(FilterEditor.class, "filterValue");
      public TypedProperty<FilterEditor, String> propertyName = new TypedProperty<>(FilterEditor.class, "propertyName");
      public TypedProperty<FilterEditor, PropertyCriterion.Filter> value = new TypedProperty<>(FilterEditor.class, "value");
      public static class InstanceProperties extends 	InstanceProperty.Container<FilterEditor> {
        public  InstanceProperties(FilterEditor source){super(source);}
        public InstanceProperty<FilterEditor, String> filterValue(){return new InstanceProperty<>(source,PackageProperties.filterEditor.filterValue);}
        public InstanceProperty<FilterEditor, String> propertyName(){return new InstanceProperty<>(source,PackageProperties.filterEditor.propertyName);}
        public InstanceProperty<FilterEditor, PropertyCriterion.Filter> value(){return new InstanceProperty<>(source,PackageProperties.filterEditor.value);}
      }
      
      public  InstanceProperties instance(FilterEditor instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _OperatorSelector implements TypedProperty.Container {
      public TypedProperty<OperatorSelector, StandardSearchOperator> operator = new TypedProperty<>(OperatorSelector.class, "operator");
      public static class InstanceProperties extends 	InstanceProperty.Container<OperatorSelector> {
        public  InstanceProperties(OperatorSelector source){super(source);}
        public InstanceProperty<OperatorSelector, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.operatorSelector.operator);}
      }
      
      public  InstanceProperties instance(OperatorSelector instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _OrderEditor implements TypedProperty.Container {
      public TypedProperty<OrderEditor, SearchCriterion.Direction> direction = new TypedProperty<>(OrderEditor.class, "direction");
      public TypedProperty<OrderEditor, String> propertyName = new TypedProperty<>(OrderEditor.class, "propertyName");
      public TypedProperty<OrderEditor, PropertyOrderCriterion.Order> value = new TypedProperty<>(OrderEditor.class, "value");
      public static class InstanceProperties extends 	InstanceProperty.Container<OrderEditor> {
        public  InstanceProperties(OrderEditor source){super(source);}
        public InstanceProperty<OrderEditor, SearchCriterion.Direction> direction(){return new InstanceProperty<>(source,PackageProperties.orderEditor.direction);}
        public InstanceProperty<OrderEditor, String> propertyName(){return new InstanceProperty<>(source,PackageProperties.orderEditor.propertyName);}
        public InstanceProperty<OrderEditor, PropertyOrderCriterion.Order> value(){return new InstanceProperty<>(source,PackageProperties.orderEditor.value);}
      }
      
      public  InstanceProperties instance(OrderEditor instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _SearchDefinitionEditor implements TypedProperty.Container {
      public TypedProperty<SearchDefinitionEditor, Boolean> modified = new TypedProperty<>(SearchDefinitionEditor.class, "modified");
      public TypedProperty<SearchDefinitionEditor, Boolean> popupsOpen = new TypedProperty<>(SearchDefinitionEditor.class, "popupsOpen");
      public TypedProperty<SearchDefinitionEditor, List> searchables = new TypedProperty<>(SearchDefinitionEditor.class, "searchables");
      public static class InstanceProperties extends 	InstanceProperty.Container<SearchDefinitionEditor> {
        public  InstanceProperties(SearchDefinitionEditor source){super(source);}
        public InstanceProperty<SearchDefinitionEditor, Boolean> modified(){return new InstanceProperty<>(source,PackageProperties.searchDefinitionEditor.modified);}
        public InstanceProperty<SearchDefinitionEditor, Boolean> popupsOpen(){return new InstanceProperty<>(source,PackageProperties.searchDefinitionEditor.popupsOpen);}
        public InstanceProperty<SearchDefinitionEditor, List> searchables(){return new InstanceProperty<>(source,PackageProperties.searchDefinitionEditor.searchables);}
      }
      
      public  InstanceProperties instance(SearchDefinitionEditor instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _Searchable implements TypedProperty.Container {
      public TypedProperty<Searchable, String> criterionClass = new TypedProperty<>(Searchable.class, "criterionClass");
      public TypedProperty<Searchable, Link> delete = new TypedProperty<>(Searchable.class, "delete");
      public TypedProperty<Searchable, Boolean> emptyValue = new TypedProperty<>(Searchable.class, "emptyValue");
      public TypedProperty<Searchable, String> name = new TypedProperty<>(Searchable.class, "name");
      public TypedProperty<Searchable, Object> operator = new TypedProperty<>(Searchable.class, "operator");
      public TypedProperty<Searchable, Searchable.RenderedOperator> renderedOperator = new TypedProperty<>(Searchable.class, "renderedOperator");
      public TypedProperty<Searchable, SearchCriterion> searchCriterion = new TypedProperty<>(Searchable.class, "searchCriterion");
      public TypedProperty<Searchable, ValueEditor> valueEditor = new TypedProperty<>(Searchable.class, "valueEditor");
      public static class InstanceProperties extends 	InstanceProperty.Container<Searchable> {
        public  InstanceProperties(Searchable source){super(source);}
        public InstanceProperty<Searchable, String> criterionClass(){return new InstanceProperty<>(source,PackageProperties.searchable.criterionClass);}
        public InstanceProperty<Searchable, Link> delete(){return new InstanceProperty<>(source,PackageProperties.searchable.delete);}
        public InstanceProperty<Searchable, Boolean> emptyValue(){return new InstanceProperty<>(source,PackageProperties.searchable.emptyValue);}
        public InstanceProperty<Searchable, String> name(){return new InstanceProperty<>(source,PackageProperties.searchable.name);}
        public InstanceProperty<Searchable, Object> operator(){return new InstanceProperty<>(source,PackageProperties.searchable.operator);}
        public InstanceProperty<Searchable, Searchable.RenderedOperator> renderedOperator(){return new InstanceProperty<>(source,PackageProperties.searchable.renderedOperator);}
        public InstanceProperty<Searchable, SearchCriterion> searchCriterion(){return new InstanceProperty<>(source,PackageProperties.searchable.searchCriterion);}
        public InstanceProperty<Searchable, ValueEditor> valueEditor(){return new InstanceProperty<>(source,PackageProperties.searchable.valueEditor);}
      }
      
      public  InstanceProperties instance(Searchable instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _Searchable_RenderedOperator implements TypedProperty.Container {
      TypedProperty<Searchable.RenderedOperator, StandardSearchOperator> operator = new TypedProperty<>(Searchable.RenderedOperator.class, "operator");
      static class InstanceProperties extends 	InstanceProperty.Container<Searchable.RenderedOperator> {
         InstanceProperties(Searchable.RenderedOperator source){super(source);}
        InstanceProperty<Searchable.RenderedOperator, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.searchable_renderedOperator.operator);}
      }
      
       InstanceProperties instance(Searchable.RenderedOperator instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
