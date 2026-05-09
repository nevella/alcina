package cc.alcina.framework.servlet.component.gallery.model.sequencearea;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.servlet.component.gallery.GalleryPlace;
import cc.alcina.framework.servlet.component.sequence.SequenceComponentEditor;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    static _Gallery_SequenceAreaReportArea gallery_sequenceAreaReportArea = new _Gallery_SequenceAreaReportArea();
    
    static class _Gallery_SequenceAreaReportArea implements TypedProperty.Container {
      TypedProperty<Gallery_SequenceAreaReportArea, GalleryPlace> place = new TypedProperty<>(Gallery_SequenceAreaReportArea.class, "place");
      TypedProperty<Gallery_SequenceAreaReportArea, SequenceComponentEditor> sequenceEditor = new TypedProperty<>(Gallery_SequenceAreaReportArea.class, "sequenceEditor");
      static class InstanceProperties extends 	InstanceProperty.Container<Gallery_SequenceAreaReportArea> {
         InstanceProperties(Gallery_SequenceAreaReportArea source){super(source);}
        InstanceProperty<Gallery_SequenceAreaReportArea, GalleryPlace> place(){return new InstanceProperty<>(source,PackageProperties.gallery_sequenceAreaReportArea.place);}
        InstanceProperty<Gallery_SequenceAreaReportArea, SequenceComponentEditor> sequenceEditor(){return new InstanceProperty<>(source,PackageProperties.gallery_sequenceAreaReportArea.sequenceEditor);}
      }
      
       InstanceProperties instance(Gallery_SequenceAreaReportArea instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
