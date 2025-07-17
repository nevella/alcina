package cc.alcina.framework.gwt.client.module.support.login;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.place.BasePlace;
import java.lang.Object;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    public static _LoginArea loginArea = new _LoginArea();
    
    public static class _LoginArea implements TypedProperty.Container {
      public TypedProperty<LoginArea, Object> contents = new TypedProperty<>(LoginArea.class, "contents");
      public TypedProperty<LoginArea, BasePlace> place = new TypedProperty<>(LoginArea.class, "place");
      public static class InstanceProperties extends InstanceProperty.Container<LoginArea> {
        public  InstanceProperties(LoginArea source){super(source);}
        public InstanceProperty<LoginArea, Object> contents(){return new InstanceProperty<>(source,PackageProperties.loginArea.contents);}
        public InstanceProperty<LoginArea, BasePlace> place(){return new InstanceProperty<>(source,PackageProperties.loginArea.place);}
      }
      
      public  InstanceProperties instance(LoginArea instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
