package cc.alcina.framework.servlet.component.gallery.model.fragmentdocument;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.servlet.component.gallery.GalleryPlace;
import java.util.List;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    static _FragmentDocumentGalleryArea fragmentDocumentGalleryArea = new _FragmentDocumentGalleryArea();
    public static _FragmentDocumentGalleryPlace_Definition fragmentDocumentGalleryPlace_definition = new _FragmentDocumentGalleryPlace_Definition();
    
    static class _FragmentDocumentGalleryArea implements TypedProperty.Container {
      TypedProperty<FragmentDocumentGalleryArea, String> documentTreeRepresentation = new TypedProperty<>(FragmentDocumentGalleryArea.class, "documentTreeRepresentation");
      TypedProperty<FragmentDocumentGalleryArea, String> heading = new TypedProperty<>(FragmentDocumentGalleryArea.class, "heading");
      TypedProperty<FragmentDocumentGalleryArea, String> markup = new TypedProperty<>(FragmentDocumentGalleryArea.class, "markup");
      TypedProperty<FragmentDocumentGalleryArea, GalleryPlace> place = new TypedProperty<>(FragmentDocumentGalleryArea.class, "place");
      TypedProperty<FragmentDocumentGalleryArea, Link> submit = new TypedProperty<>(FragmentDocumentGalleryArea.class, "submit");
      static class InstanceProperties extends 	InstanceProperty.Container<FragmentDocumentGalleryArea> {
         InstanceProperties(FragmentDocumentGalleryArea source){super(source);}
        InstanceProperty<FragmentDocumentGalleryArea, String> documentTreeRepresentation(){return new InstanceProperty<>(source,PackageProperties.fragmentDocumentGalleryArea.documentTreeRepresentation);}
        InstanceProperty<FragmentDocumentGalleryArea, String> heading(){return new InstanceProperty<>(source,PackageProperties.fragmentDocumentGalleryArea.heading);}
        InstanceProperty<FragmentDocumentGalleryArea, String> markup(){return new InstanceProperty<>(source,PackageProperties.fragmentDocumentGalleryArea.markup);}
        InstanceProperty<FragmentDocumentGalleryArea, GalleryPlace> place(){return new InstanceProperty<>(source,PackageProperties.fragmentDocumentGalleryArea.place);}
        InstanceProperty<FragmentDocumentGalleryArea, Link> submit(){return new InstanceProperty<>(source,PackageProperties.fragmentDocumentGalleryArea.submit);}
      }
      
       InstanceProperties instance(FragmentDocumentGalleryArea instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _FragmentDocumentGalleryPlace_Definition implements TypedProperty.Container {
      public TypedProperty<FragmentDocumentGalleryPlace.Definition, List> users = new TypedProperty<>(FragmentDocumentGalleryPlace.Definition.class, "users");
      public static class InstanceProperties extends 	InstanceProperty.Container<FragmentDocumentGalleryPlace.Definition> {
        public  InstanceProperties(FragmentDocumentGalleryPlace.Definition source){super(source);}
        public InstanceProperty<FragmentDocumentGalleryPlace.Definition, List> users(){return new InstanceProperty<>(source,PackageProperties.fragmentDocumentGalleryPlace_definition.users);}
      }
      
      public  InstanceProperties instance(FragmentDocumentGalleryPlace.Definition instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
