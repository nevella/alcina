package cc.alcina.framework.gwt.client.dirndl.model.component;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.model.HeadingActions;
import java.lang.Boolean;
import java.lang.String;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    static _StringArea_Expanded stringArea_expanded = new _StringArea_Expanded();
    
    static class _StringArea_Expanded implements TypedProperty.Container {
      TypedProperty<StringArea.Expanded, Boolean> fixedWidth = new TypedProperty<>(StringArea.Expanded.class, "fixedWidth");
      TypedProperty<StringArea.Expanded, HeadingActions> headingActions = new TypedProperty<>(StringArea.Expanded.class, "headingActions");
      TypedProperty<StringArea.Expanded, Boolean> preWrap = new TypedProperty<>(StringArea.Expanded.class, "preWrap");
      TypedProperty<StringArea.Expanded, String> value = new TypedProperty<>(StringArea.Expanded.class, "value");
      static class InstanceProperties extends InstanceProperty.Container<StringArea.Expanded> {
         InstanceProperties(StringArea.Expanded source){super(source);}
        InstanceProperty<StringArea.Expanded, Boolean> fixedWidth(){return new InstanceProperty<>(source,PackageProperties.stringArea_expanded.fixedWidth);}
        InstanceProperty<StringArea.Expanded, HeadingActions> headingActions(){return new InstanceProperty<>(source,PackageProperties.stringArea_expanded.headingActions);}
        InstanceProperty<StringArea.Expanded, Boolean> preWrap(){return new InstanceProperty<>(source,PackageProperties.stringArea_expanded.preWrap);}
        InstanceProperty<StringArea.Expanded, String> value(){return new InstanceProperty<>(source,PackageProperties.stringArea_expanded.value);}
      }
      
       InstanceProperties instance(StringArea.Expanded instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
