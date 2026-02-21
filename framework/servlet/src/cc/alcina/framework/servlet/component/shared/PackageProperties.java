package cc.alcina.framework.servlet.component.shared;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.behaviour.KeyboardNavigation;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.servlet.component.shared.ExecCommandsArea;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    public static _ExecCommandsArea execCommandsArea = new _ExecCommandsArea();
    
    public static class _ExecCommandsArea implements TypedProperty.Container {
      public TypedProperty<ExecCommandsArea, ExecCommandsArea.CommandArea> commandArea = new TypedProperty<>(ExecCommandsArea.class, "commandArea");
      public TypedProperty<ExecCommandsArea, KeyboardNavigation.NavigationFilterTransformer> filter = new TypedProperty<>(ExecCommandsArea.class, "filter");
      public TypedProperty<ExecCommandsArea, Heading> heading = new TypedProperty<>(ExecCommandsArea.class, "heading");
      public static class InstanceProperties extends InstanceProperty.Container<ExecCommandsArea> {
        public  InstanceProperties(ExecCommandsArea source){super(source);}
        public InstanceProperty<ExecCommandsArea, ExecCommandsArea.CommandArea> commandArea(){return new InstanceProperty<>(source,PackageProperties.execCommandsArea.commandArea);}
        public InstanceProperty<ExecCommandsArea, KeyboardNavigation.NavigationFilterTransformer> filter(){return new InstanceProperty<>(source,PackageProperties.execCommandsArea.filter);}
        public InstanceProperty<ExecCommandsArea, Heading> heading(){return new InstanceProperty<>(source,PackageProperties.execCommandsArea.heading);}
      }
      
      public  InstanceProperties instance(ExecCommandsArea instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
