package cc.alcina.framework.servlet.component.gallery.model.searchdefeditor;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceSearchDefinition;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.servlet.component.gallery.GalleryPlace;
import cc.alcina.framework.servlet.component.gallery.model.searchdefeditor.SearchDefinitionEditorGalleryArea;
import cc.alcina.framework.servlet.component.gallery.model.searchdefeditor.SearchDefinitionEditorGalleryPlace;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    static _SearchDefinitionEditorGalleryArea searchDefinitionEditorGalleryArea = new _SearchDefinitionEditorGalleryArea();
    public static _SearchDefinitionEditorGalleryPlace_Definition searchDefinitionEditorGalleryPlace_definition = new _SearchDefinitionEditorGalleryPlace_Definition();
    
    static class _SearchDefinitionEditorGalleryArea implements TypedProperty.Container {
      TypedProperty<SearchDefinitionEditorGalleryArea, SearchDefinitionEditorGalleryPlace.Definition> definition = new TypedProperty<>(SearchDefinitionEditorGalleryArea.class, "definition");
      TypedProperty<SearchDefinitionEditorGalleryArea, Heading> heading = new TypedProperty<>(SearchDefinitionEditorGalleryArea.class, "heading");
      TypedProperty<SearchDefinitionEditorGalleryArea, SearchDefinitionEditorGalleryArea.InfoModel> model = new TypedProperty<>(SearchDefinitionEditorGalleryArea.class, "model");
      TypedProperty<SearchDefinitionEditorGalleryArea, GalleryPlace> place = new TypedProperty<>(SearchDefinitionEditorGalleryArea.class, "place");
      static class InstanceProperties extends InstanceProperty.Container<SearchDefinitionEditorGalleryArea> {
         InstanceProperties(SearchDefinitionEditorGalleryArea source){super(source);}
        InstanceProperty<SearchDefinitionEditorGalleryArea, SearchDefinitionEditorGalleryPlace.Definition> definition(){return new InstanceProperty<>(source,PackageProperties.searchDefinitionEditorGalleryArea.definition);}
        InstanceProperty<SearchDefinitionEditorGalleryArea, Heading> heading(){return new InstanceProperty<>(source,PackageProperties.searchDefinitionEditorGalleryArea.heading);}
        InstanceProperty<SearchDefinitionEditorGalleryArea, SearchDefinitionEditorGalleryArea.InfoModel> model(){return new InstanceProperty<>(source,PackageProperties.searchDefinitionEditorGalleryArea.model);}
        InstanceProperty<SearchDefinitionEditorGalleryArea, GalleryPlace> place(){return new InstanceProperty<>(source,PackageProperties.searchDefinitionEditorGalleryArea.place);}
      }
      
       InstanceProperties instance(SearchDefinitionEditorGalleryArea instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _SearchDefinitionEditorGalleryPlace_Definition implements TypedProperty.Container {
      public TypedProperty<SearchDefinitionEditorGalleryPlace.Definition, SequenceSearchDefinition> def = new TypedProperty<>(SearchDefinitionEditorGalleryPlace.Definition.class, "def");
      public static class InstanceProperties extends InstanceProperty.Container<SearchDefinitionEditorGalleryPlace.Definition> {
        public  InstanceProperties(SearchDefinitionEditorGalleryPlace.Definition source){super(source);}
        public InstanceProperty<SearchDefinitionEditorGalleryPlace.Definition, SequenceSearchDefinition> def(){return new InstanceProperty<>(source,PackageProperties.searchDefinitionEditorGalleryPlace_definition.def);}
      }
      
      public  InstanceProperties instance(SearchDefinitionEditorGalleryPlace.Definition instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
