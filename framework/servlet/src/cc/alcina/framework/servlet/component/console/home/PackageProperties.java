package cc.alcina.framework.servlet.component.console.home;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.servlet.component.console.ServerConsolePlace;
import java.util.List;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    static _ServerConsoleHomeArea serverConsoleHomeArea = new _ServerConsoleHomeArea();
    
    static class _ServerConsoleHomeArea implements TypedProperty.Container {
      TypedProperty<ServerConsoleHomeArea, List> cards = new TypedProperty<>(ServerConsoleHomeArea.class, "cards");
      TypedProperty<ServerConsoleHomeArea, Heading> heading = new TypedProperty<>(ServerConsoleHomeArea.class, "heading");
      TypedProperty<ServerConsoleHomeArea, ServerConsolePlace> place = new TypedProperty<>(ServerConsoleHomeArea.class, "place");
      static class InstanceProperties extends 	InstanceProperty.Container<ServerConsoleHomeArea> {
         InstanceProperties(ServerConsoleHomeArea source){super(source);}
        InstanceProperty<ServerConsoleHomeArea, List> cards(){return new InstanceProperty<>(source,PackageProperties.serverConsoleHomeArea.cards);}
        InstanceProperty<ServerConsoleHomeArea, Heading> heading(){return new InstanceProperty<>(source,PackageProperties.serverConsoleHomeArea.heading);}
        InstanceProperty<ServerConsoleHomeArea, ServerConsolePlace> place(){return new InstanceProperty<>(source,PackageProperties.serverConsoleHomeArea.place);}
      }
      
       InstanceProperties instance(ServerConsoleHomeArea instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
