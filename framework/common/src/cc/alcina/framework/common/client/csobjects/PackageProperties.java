package cc.alcina.framework.common.client.csobjects;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import java.lang.Object;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    public static _Bindable_Value bindable_value = new _Bindable_Value();
    
    public static class _Bindable_Value implements TypedProperty.Container {
      public TypedProperty<Bindable.Value, Object> value = new TypedProperty<>(Bindable.Value.class, "value");
      public static class InstanceProperties extends 	InstanceProperty.Container<Bindable.Value> {
        public  InstanceProperties(Bindable.Value source){super(source);}
        public InstanceProperty<Bindable.Value, Object> value(){return new InstanceProperty<>(source,PackageProperties.bindable_value.value);}
      }
      
      public  InstanceProperties instance(Bindable.Value instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
