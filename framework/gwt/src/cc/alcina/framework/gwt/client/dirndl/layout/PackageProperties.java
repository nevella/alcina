package cc.alcina.framework.gwt.client.dirndl.layout;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import java.lang.Boolean;
import java.lang.Object;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    public static _LeafModel_Toggle leafModel_toggle = new _LeafModel_Toggle();
    
    public static class _LeafModel_Toggle implements TypedProperty.Container {
      public TypedProperty<LeafModel.Toggle, Object> model = new TypedProperty<>(LeafModel.Toggle.class, "model");
      public TypedProperty<LeafModel.Toggle, Boolean> selected = new TypedProperty<>(LeafModel.Toggle.class, "selected");
      public static class InstanceProperties extends InstanceProperty.Container<LeafModel.Toggle> {
        public  InstanceProperties(LeafModel.Toggle source){super(source);}
        public InstanceProperty<LeafModel.Toggle, Object> model(){return new InstanceProperty<>(source,PackageProperties.leafModel_toggle.model);}
        public InstanceProperty<LeafModel.Toggle, Boolean> selected(){return new InstanceProperty<>(source,PackageProperties.leafModel_toggle.selected);}
      }
      
      public  InstanceProperties instance(LeafModel.Toggle instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
