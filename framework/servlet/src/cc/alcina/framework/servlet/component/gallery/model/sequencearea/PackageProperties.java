package cc.alcina.framework.servlet.component.gallery.model.sequencearea;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequencePlace;
import cc.alcina.framework.servlet.component.gallery.GalleryPlace;
import cc.alcina.framework.servlet.component.gallery.model.sequencearea.Gallery_SequenceAreaReportArea;
import java.lang.Object;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    static _Gallery_SequenceAreaReportArea gallery_sequenceAreaReportArea = new _Gallery_SequenceAreaReportArea();
    static _Gallery_SequenceAreaReportArea_Header gallery_sequenceAreaReportArea_header = new _Gallery_SequenceAreaReportArea_Header();
    
    static class _Gallery_SequenceAreaReportArea implements TypedProperty.Container {
      TypedProperty<Gallery_SequenceAreaReportArea, Gallery_SequenceAreaReportArea.Header> header = new TypedProperty<>(Gallery_SequenceAreaReportArea.class, "header");
      TypedProperty<Gallery_SequenceAreaReportArea, Object> model = new TypedProperty<>(Gallery_SequenceAreaReportArea.class, "model");
      TypedProperty<Gallery_SequenceAreaReportArea, GalleryPlace> place = new TypedProperty<>(Gallery_SequenceAreaReportArea.class, "place");
      TypedProperty<Gallery_SequenceAreaReportArea, SequencePlace> sequencePlace = new TypedProperty<>(Gallery_SequenceAreaReportArea.class, "sequencePlace");
      static class InstanceProperties extends InstanceProperty.Container<Gallery_SequenceAreaReportArea> {
         InstanceProperties(Gallery_SequenceAreaReportArea source){super(source);}
        InstanceProperty<Gallery_SequenceAreaReportArea, Gallery_SequenceAreaReportArea.Header> header(){return new InstanceProperty<>(source,PackageProperties.gallery_sequenceAreaReportArea.header);}
        InstanceProperty<Gallery_SequenceAreaReportArea, Object> model(){return new InstanceProperty<>(source,PackageProperties.gallery_sequenceAreaReportArea.model);}
        InstanceProperty<Gallery_SequenceAreaReportArea, GalleryPlace> place(){return new InstanceProperty<>(source,PackageProperties.gallery_sequenceAreaReportArea.place);}
        InstanceProperty<Gallery_SequenceAreaReportArea, SequencePlace> sequencePlace(){return new InstanceProperty<>(source,PackageProperties.gallery_sequenceAreaReportArea.sequencePlace);}
      }
      
       InstanceProperties instance(Gallery_SequenceAreaReportArea instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _Gallery_SequenceAreaReportArea_Header implements TypedProperty.Container {
      TypedProperty<Gallery_SequenceAreaReportArea.Header, SearchDefinition> searchDefinition = new TypedProperty<>(Gallery_SequenceAreaReportArea.Header.class, "searchDefinition");
      static class InstanceProperties extends InstanceProperty.Container<Gallery_SequenceAreaReportArea.Header> {
         InstanceProperties(Gallery_SequenceAreaReportArea.Header source){super(source);}
        InstanceProperty<Gallery_SequenceAreaReportArea.Header, SearchDefinition> searchDefinition(){return new InstanceProperty<>(source,PackageProperties.gallery_sequenceAreaReportArea_header.searchDefinition);}
      }
      
       InstanceProperties instance(Gallery_SequenceAreaReportArea.Header instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
