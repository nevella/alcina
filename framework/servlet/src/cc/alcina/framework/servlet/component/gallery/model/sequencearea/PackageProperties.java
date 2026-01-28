package cc.alcina.framework.servlet.component.gallery.model.sequencearea;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequencePlace;
import cc.alcina.framework.servlet.component.gallery.GalleryPlace;

public class PackageProperties {
	// auto-generated, do not modify
	//@formatter:off
    
    static _Gallery_SequenceAreasReportArea userSessionsReportArea = new _Gallery_SequenceAreasReportArea();
    static _Gallery_SequenceAreasReportArea_Header userSessionsReportArea_header = new _Gallery_SequenceAreasReportArea_Header();
    
    static class _Gallery_SequenceAreasReportArea implements TypedProperty.Container {
      TypedProperty<Gallery_SequenceAreasReportArea, Gallery_SequenceAreasReportArea.Header> header = new TypedProperty<>(Gallery_SequenceAreasReportArea.class, "header");
      TypedProperty<Gallery_SequenceAreasReportArea, Object> model = new TypedProperty<>(Gallery_SequenceAreasReportArea.class, "model");
      TypedProperty<Gallery_SequenceAreasReportArea, GalleryPlace> place = new TypedProperty<>(Gallery_SequenceAreasReportArea.class, "place");
      TypedProperty<Gallery_SequenceAreasReportArea, SequencePlace> sequencePlace = new TypedProperty<>(Gallery_SequenceAreasReportArea.class, "sequencePlace");
      static class InstanceProperties extends InstanceProperty.Container<Gallery_SequenceAreasReportArea> {
         InstanceProperties(Gallery_SequenceAreasReportArea source){super(source);}
        InstanceProperty<Gallery_SequenceAreasReportArea, Gallery_SequenceAreasReportArea.Header> header(){return new InstanceProperty<>(source,PackageProperties.userSessionsReportArea.header);}
        InstanceProperty<Gallery_SequenceAreasReportArea, Object> model(){return new InstanceProperty<>(source,PackageProperties.userSessionsReportArea.model);}
        InstanceProperty<Gallery_SequenceAreasReportArea, GalleryPlace> place(){return new InstanceProperty<>(source,PackageProperties.userSessionsReportArea.place);}
        InstanceProperty<Gallery_SequenceAreasReportArea, SequencePlace> sequencePlace(){return new InstanceProperty<>(source,PackageProperties.userSessionsReportArea.sequencePlace);}
      }
      
       InstanceProperties instance(Gallery_SequenceAreasReportArea instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
  
    
    static class _Gallery_SequenceAreasReportArea_Header implements TypedProperty.Container {
      TypedProperty<Gallery_SequenceAreasReportArea.Header, SearchDefinition> searchDefinition = new TypedProperty<>(Gallery_SequenceAreasReportArea.Header.class, "searchDefinition");
      static class InstanceProperties extends InstanceProperty.Container<Gallery_SequenceAreasReportArea.Header> {
         InstanceProperties(Gallery_SequenceAreasReportArea.Header source){super(source);}
        InstanceProperty<Gallery_SequenceAreasReportArea.Header, SearchDefinition> searchDefinition(){return new InstanceProperty<>(source,PackageProperties.userSessionsReportArea_header.searchDefinition);}
      }
      
       InstanceProperties instance(Gallery_SequenceAreasReportArea.Header instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
