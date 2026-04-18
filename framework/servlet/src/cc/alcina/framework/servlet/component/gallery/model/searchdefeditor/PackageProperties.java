package cc.alcina.framework.servlet.component.gallery.model.searchdefeditor;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceSearchDefinition;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.servlet.component.gallery.GalleryPlace;

public class PackageProperties {
	// auto-generated, do not modify
	//@formatter:off
    
    static _SearchDefinitionEditorGalleryArea searchDefinitionEditorGalleryArea = new _SearchDefinitionEditorGalleryArea();
    public static _SearchDefinitionEditorGalleryPlace_Definition searchDefinitionEditorGalleryPlace_definition = new _SearchDefinitionEditorGalleryPlace_Definition();
    
    static class _SearchDefinitionEditorGalleryArea implements TypedProperty.Container {
      TypedProperty<Gallery_SearchDefinitionEditorArea, SearchDefinitionEditorGalleryPlace.Definition> definition = new TypedProperty<>(Gallery_SearchDefinitionEditorArea.class, "definition");
      TypedProperty<Gallery_SearchDefinitionEditorArea, Heading> heading = new TypedProperty<>(Gallery_SearchDefinitionEditorArea.class, "heading");
      TypedProperty<Gallery_SearchDefinitionEditorArea, Gallery_SearchDefinitionEditorArea.InfoModel> model = new TypedProperty<>(Gallery_SearchDefinitionEditorArea.class, "model");
      TypedProperty<Gallery_SearchDefinitionEditorArea, GalleryPlace> place = new TypedProperty<>(Gallery_SearchDefinitionEditorArea.class, "place");
      static class InstanceProperties extends 	InstanceProperty.Container<Gallery_SearchDefinitionEditorArea> {
         InstanceProperties(Gallery_SearchDefinitionEditorArea source){super(source);}
        InstanceProperty<Gallery_SearchDefinitionEditorArea, SearchDefinitionEditorGalleryPlace.Definition> definition(){return new InstanceProperty<>(source,PackageProperties.searchDefinitionEditorGalleryArea.definition);}
        InstanceProperty<Gallery_SearchDefinitionEditorArea, Heading> heading(){return new InstanceProperty<>(source,PackageProperties.searchDefinitionEditorGalleryArea.heading);}
        InstanceProperty<Gallery_SearchDefinitionEditorArea, Gallery_SearchDefinitionEditorArea.InfoModel> model(){return new InstanceProperty<>(source,PackageProperties.searchDefinitionEditorGalleryArea.model);}
        InstanceProperty<Gallery_SearchDefinitionEditorArea, GalleryPlace> place(){return new InstanceProperty<>(source,PackageProperties.searchDefinitionEditorGalleryArea.place);}
      }
      
       InstanceProperties instance(Gallery_SearchDefinitionEditorArea instance) {
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
