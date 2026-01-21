package cc.alcina.framework.gwt.client.dirndl.model.search;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.gwt.client.dirndl.model.Dropdown;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.search.Searchable;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;
import java.lang.String;
import java.util.List;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    public static _SearchDefinitionEditor searchDefinitionEditor = new _SearchDefinitionEditor();
    static _Searchable searchable = new _Searchable();
    static _Searchable_OperatorSelector searchable_operatorSelector = new _Searchable_OperatorSelector();
    static _Searchable_RenderedOperator searchable_renderedOperator = new _Searchable_RenderedOperator();
    
    public static class _SearchDefinitionEditor implements TypedProperty.Container {
      public TypedProperty<SearchDefinitionEditor, Link> go = new TypedProperty<>(SearchDefinitionEditor.class, "go");
      public TypedProperty<SearchDefinitionEditor, List> searchables = new TypedProperty<>(SearchDefinitionEditor.class, "searchables");
      public static class InstanceProperties extends InstanceProperty.Container<SearchDefinitionEditor> {
        public  InstanceProperties(SearchDefinitionEditor source){super(source);}
        public InstanceProperty<SearchDefinitionEditor, Link> go(){return new InstanceProperty<>(source,PackageProperties.searchDefinitionEditor.go);}
        public InstanceProperty<SearchDefinitionEditor, List> searchables(){return new InstanceProperty<>(source,PackageProperties.searchDefinitionEditor.searchables);}
      }
      
      public  InstanceProperties instance(SearchDefinitionEditor instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _Searchable implements TypedProperty.Container {
      TypedProperty<Searchable, String> criterionClass = new TypedProperty<>(Searchable.class, "criterionClass");
      TypedProperty<Searchable, String> name = new TypedProperty<>(Searchable.class, "name");
      TypedProperty<Searchable, Dropdown> operatorDropdown = new TypedProperty<>(Searchable.class, "operatorDropdown");
      TypedProperty<Searchable, Searchable.RenderedOperator> renderedOperator = new TypedProperty<>(Searchable.class, "renderedOperator");
      TypedProperty<Searchable, SearchCriterion> searchCriterion = new TypedProperty<>(Searchable.class, "searchCriterion");
      TypedProperty<Searchable, Searchable.ValueEditor> valueEditor = new TypedProperty<>(Searchable.class, "valueEditor");
      static class InstanceProperties extends InstanceProperty.Container<Searchable> {
         InstanceProperties(Searchable source){super(source);}
        InstanceProperty<Searchable, String> criterionClass(){return new InstanceProperty<>(source,PackageProperties.searchable.criterionClass);}
        InstanceProperty<Searchable, String> name(){return new InstanceProperty<>(source,PackageProperties.searchable.name);}
        InstanceProperty<Searchable, Dropdown> operatorDropdown(){return new InstanceProperty<>(source,PackageProperties.searchable.operatorDropdown);}
        InstanceProperty<Searchable, Searchable.RenderedOperator> renderedOperator(){return new InstanceProperty<>(source,PackageProperties.searchable.renderedOperator);}
        InstanceProperty<Searchable, SearchCriterion> searchCriterion(){return new InstanceProperty<>(source,PackageProperties.searchable.searchCriterion);}
        InstanceProperty<Searchable, Searchable.ValueEditor> valueEditor(){return new InstanceProperty<>(source,PackageProperties.searchable.valueEditor);}
      }
      
       InstanceProperties instance(Searchable instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _Searchable_OperatorSelector implements TypedProperty.Container {
      TypedProperty<Searchable.OperatorSelector, StandardSearchOperator> operator = new TypedProperty<>(Searchable.OperatorSelector.class, "operator");
      static class InstanceProperties extends InstanceProperty.Container<Searchable.OperatorSelector> {
         InstanceProperties(Searchable.OperatorSelector source){super(source);}
        InstanceProperty<Searchable.OperatorSelector, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.searchable_operatorSelector.operator);}
      }
      
       InstanceProperties instance(Searchable.OperatorSelector instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _Searchable_RenderedOperator implements TypedProperty.Container {
      TypedProperty<Searchable.RenderedOperator, StandardSearchOperator> operator = new TypedProperty<>(Searchable.RenderedOperator.class, "operator");
      static class InstanceProperties extends InstanceProperty.Container<Searchable.RenderedOperator> {
         InstanceProperties(Searchable.RenderedOperator source){super(source);}
        InstanceProperty<Searchable.RenderedOperator, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.searchable_renderedOperator.operator);}
      }
      
       InstanceProperties instance(Searchable.RenderedOperator instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
