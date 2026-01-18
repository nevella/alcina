package cc.alcina.framework.gwt.client.dirndl.model.search;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.search.Searchable;
import java.lang.String;
import java.util.List;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    public static _SearchDefinitionEditor searchDefinitionEditor = new _SearchDefinitionEditor();
    static _Searchable searchable = new _Searchable();
    
    public static class _SearchDefinitionEditor implements TypedProperty.Container {
      public TypedProperty<SearchDefinitionEditor, Link> go = new TypedProperty<>(SearchDefinitionEditor.class, "go");
      public TypedProperty<SearchDefinitionEditor, SearchDefinition> searchDefinition = new TypedProperty<>(SearchDefinitionEditor.class, "searchDefinition");
      public TypedProperty<SearchDefinitionEditor, List> searchables = new TypedProperty<>(SearchDefinitionEditor.class, "searchables");
      public static class InstanceProperties extends InstanceProperty.Container<SearchDefinitionEditor> {
        public  InstanceProperties(SearchDefinitionEditor source){super(source);}
        public InstanceProperty<SearchDefinitionEditor, Link> go(){return new InstanceProperty<>(source,PackageProperties.searchDefinitionEditor.go);}
        public InstanceProperty<SearchDefinitionEditor, SearchDefinition> searchDefinition(){return new InstanceProperty<>(source,PackageProperties.searchDefinitionEditor.searchDefinition);}
        public InstanceProperty<SearchDefinitionEditor, List> searchables(){return new InstanceProperty<>(source,PackageProperties.searchDefinitionEditor.searchables);}
      }
      
      public  InstanceProperties instance(SearchDefinitionEditor instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _Searchable implements TypedProperty.Container {
      TypedProperty<Searchable, String> criterionClass = new TypedProperty<>(Searchable.class, "criterionClass");
      TypedProperty<Searchable, String> name = new TypedProperty<>(Searchable.class, "name");
      TypedProperty<Searchable, SearchCriterion> searchCriterion = new TypedProperty<>(Searchable.class, "searchCriterion");
      TypedProperty<Searchable, Searchable.ValueEditor> valueEditor = new TypedProperty<>(Searchable.class, "valueEditor");
      static class InstanceProperties extends InstanceProperty.Container<Searchable> {
         InstanceProperties(Searchable source){super(source);}
        InstanceProperty<Searchable, String> criterionClass(){return new InstanceProperty<>(source,PackageProperties.searchable.criterionClass);}
        InstanceProperty<Searchable, String> name(){return new InstanceProperty<>(source,PackageProperties.searchable.name);}
        InstanceProperty<Searchable, SearchCriterion> searchCriterion(){return new InstanceProperty<>(source,PackageProperties.searchable.searchCriterion);}
        InstanceProperty<Searchable, Searchable.ValueEditor> valueEditor(){return new InstanceProperty<>(source,PackageProperties.searchable.valueEditor);}
      }
      
       InstanceProperties instance(Searchable instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
