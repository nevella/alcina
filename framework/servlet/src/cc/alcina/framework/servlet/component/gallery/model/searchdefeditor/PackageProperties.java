package cc.alcina.framework.servlet.component.gallery.model.searchdefeditor;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceSearchDefinition;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.servlet.component.gallery.GalleryPlace;
import cc.alcina.framework.servlet.component.gallery.model.searchdefeditor.SearchDefinitionEditorArea_Gallery;
import cc.alcina.framework.servlet.component.gallery.model.searchdefeditor.SearchDefinitionEditorGalleryPlace;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    static _SearchDefinitionEditorArea_Gallery searchDefinitionEditorArea_gallery = new _SearchDefinitionEditorArea_Gallery();
    public static _SearchDefinitionEditorGalleryPlace_Definition searchDefinitionEditorGalleryPlace_definition = new _SearchDefinitionEditorGalleryPlace_Definition();
    
    static class _SearchDefinitionEditorArea_Gallery implements TypedProperty.Container {
      TypedProperty<SearchDefinitionEditorArea_Gallery, SearchDefinitionEditorGalleryPlace.Definition> definition = new TypedProperty<>(SearchDefinitionEditorArea_Gallery.class, "definition");
      TypedProperty<SearchDefinitionEditorArea_Gallery, Heading> heading = new TypedProperty<>(SearchDefinitionEditorArea_Gallery.class, "heading");
      TypedProperty<SearchDefinitionEditorArea_Gallery, SearchDefinitionEditorArea_Gallery.InfoModel> model = new TypedProperty<>(SearchDefinitionEditorArea_Gallery.class, "model");
      TypedProperty<SearchDefinitionEditorArea_Gallery, GalleryPlace> place = new TypedProperty<>(SearchDefinitionEditorArea_Gallery.class, "place");
      static class InstanceProperties extends 	InstanceProperty.Container<SearchDefinitionEditorArea_Gallery> {
         InstanceProperties(SearchDefinitionEditorArea_Gallery source){super(source);}
        InstanceProperty<SearchDefinitionEditorArea_Gallery, SearchDefinitionEditorGalleryPlace.Definition> definition(){return new InstanceProperty<>(source,PackageProperties.searchDefinitionEditorArea_gallery.definition);}
        InstanceProperty<SearchDefinitionEditorArea_Gallery, Heading> heading(){return new InstanceProperty<>(source,PackageProperties.searchDefinitionEditorArea_gallery.heading);}
        InstanceProperty<SearchDefinitionEditorArea_Gallery, SearchDefinitionEditorArea_Gallery.InfoModel> model(){return new InstanceProperty<>(source,PackageProperties.searchDefinitionEditorArea_gallery.model);}
        InstanceProperty<SearchDefinitionEditorArea_Gallery, GalleryPlace> place(){return new InstanceProperty<>(source,PackageProperties.searchDefinitionEditorArea_gallery.place);}
      }
      
       InstanceProperties instance(SearchDefinitionEditorArea_Gallery instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _SearchDefinitionEditorGalleryPlace_Definition implements TypedProperty.Container {
      public TypedProperty<SearchDefinitionEditorGalleryPlace.Definition, SequenceSearchDefinition> def = new TypedProperty<>(SearchDefinitionEditorGalleryPlace.Definition.class, "def");
      public static class InstanceProperties extends 	InstanceProperty.Container<SearchDefinitionEditorGalleryPlace.Definition> {
        public  InstanceProperties(SearchDefinitionEditorGalleryPlace.Definition source){super(source);}
        public InstanceProperty<SearchDefinitionEditorGalleryPlace.Definition, SequenceSearchDefinition> def(){return new InstanceProperty<>(source,PackageProperties.searchDefinitionEditorGalleryPlace_definition.def);}
      }
      
      public  InstanceProperties instance(SearchDefinitionEditorGalleryPlace.Definition instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
