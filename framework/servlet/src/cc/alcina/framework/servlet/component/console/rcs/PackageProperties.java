package cc.alcina.framework.servlet.component.console.rcs;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequencePlace;
import cc.alcina.framework.servlet.component.console.ServerConsolePlace;
import cc.alcina.framework.servlet.component.console.rcs.RomcomSessionArea;
import cc.alcina.framework.servlet.component.sequence.SequenceComponentServer;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    static _RomcomSessionArea romcomSessionArea = new _RomcomSessionArea();
    static _RomcomSessionArea_Header romcomSessionArea_header = new _RomcomSessionArea_Header();
    
    static class _RomcomSessionArea implements TypedProperty.Container {
      TypedProperty<RomcomSessionArea, RomcomSessionArea.Header> header = new TypedProperty<>(RomcomSessionArea.class, "header");
      TypedProperty<RomcomSessionArea, SequenceComponentServer> model = new TypedProperty<>(RomcomSessionArea.class, "model");
      TypedProperty<RomcomSessionArea, ServerConsolePlace> place = new TypedProperty<>(RomcomSessionArea.class, "place");
      TypedProperty<RomcomSessionArea, SequencePlace> sequencePlace = new TypedProperty<>(RomcomSessionArea.class, "sequencePlace");
      static class InstanceProperties extends 	InstanceProperty.Container<RomcomSessionArea> {
         InstanceProperties(RomcomSessionArea source){super(source);}
        InstanceProperty<RomcomSessionArea, RomcomSessionArea.Header> header(){return new InstanceProperty<>(source,PackageProperties.romcomSessionArea.header);}
        InstanceProperty<RomcomSessionArea, SequenceComponentServer> model(){return new InstanceProperty<>(source,PackageProperties.romcomSessionArea.model);}
        InstanceProperty<RomcomSessionArea, ServerConsolePlace> place(){return new InstanceProperty<>(source,PackageProperties.romcomSessionArea.place);}
        InstanceProperty<RomcomSessionArea, SequencePlace> sequencePlace(){return new InstanceProperty<>(source,PackageProperties.romcomSessionArea.sequencePlace);}
      }
      
       InstanceProperties instance(RomcomSessionArea instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _RomcomSessionArea_Header implements TypedProperty.Container {
      TypedProperty<RomcomSessionArea.Header, SearchDefinition> searchDefinition = new TypedProperty<>(RomcomSessionArea.Header.class, "searchDefinition");
      static class InstanceProperties extends 	InstanceProperty.Container<RomcomSessionArea.Header> {
         InstanceProperties(RomcomSessionArea.Header source){super(source);}
        InstanceProperty<RomcomSessionArea.Header, SearchDefinition> searchDefinition(){return new InstanceProperty<>(source,PackageProperties.romcomSessionArea_header.searchDefinition);}
      }
      
       InstanceProperties instance(RomcomSessionArea.Header instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
