package cc.alcina.framework.servlet.component.gallery.home;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.servlet.component.gallery.GalleryPlace;
import java.util.List;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    static _GalleryHomeArea galleryHomeArea = new _GalleryHomeArea();
    
    static class _GalleryHomeArea implements TypedProperty.Container {
      TypedProperty<GalleryHomeArea, List> cards = new TypedProperty<>(GalleryHomeArea.class, "cards");
      TypedProperty<GalleryHomeArea, Heading> heading = new TypedProperty<>(GalleryHomeArea.class, "heading");
      TypedProperty<GalleryHomeArea, GalleryPlace> place = new TypedProperty<>(GalleryHomeArea.class, "place");
      static class InstanceProperties extends InstanceProperty.Container<GalleryHomeArea> {
         InstanceProperties(GalleryHomeArea source){super(source);}
        InstanceProperty<GalleryHomeArea, List> cards(){return new InstanceProperty<>(source,PackageProperties.galleryHomeArea.cards);}
        InstanceProperty<GalleryHomeArea, Heading> heading(){return new InstanceProperty<>(source,PackageProperties.galleryHomeArea.heading);}
        InstanceProperty<GalleryHomeArea, GalleryPlace> place(){return new InstanceProperty<>(source,PackageProperties.galleryHomeArea.place);}
      }
      
       InstanceProperties instance(GalleryHomeArea instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
