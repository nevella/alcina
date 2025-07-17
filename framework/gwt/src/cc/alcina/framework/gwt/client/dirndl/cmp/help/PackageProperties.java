package cc.alcina.framework.gwt.client.dirndl.cmp.help;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.cmp.help.HelpArea;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel;
import cc.alcina.framework.gwt.client.place.BasePlace;
import java.lang.Class;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    static _HelpArea helpArea = new _HelpArea();
    static _HelpArea_ActivityRoute helpArea_activityRoute = new _HelpArea_ActivityRoute();
    
    static class _HelpArea implements TypedProperty.Container {
      TypedProperty<HelpArea, LeafModel.TagMarkup> markup = new TypedProperty<>(HelpArea.class, "markup");
      static class InstanceProperties extends InstanceProperty.Container<HelpArea> {
         InstanceProperties(HelpArea source){super(source);}
        InstanceProperty<HelpArea, LeafModel.TagMarkup> markup(){return new InstanceProperty<>(source,PackageProperties.helpArea.markup);}
      }
      
       InstanceProperties instance(HelpArea instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _HelpArea_ActivityRoute implements TypedProperty.Container {
      TypedProperty<HelpArea.ActivityRoute, HelpArea> area = new TypedProperty<>(HelpArea.ActivityRoute.class, "area");
      TypedProperty<HelpArea.ActivityRoute, Class> channel = new TypedProperty<>(HelpArea.ActivityRoute.class, "channel");
      TypedProperty<HelpArea.ActivityRoute, BasePlace> place = new TypedProperty<>(HelpArea.ActivityRoute.class, "place");
      static class InstanceProperties extends InstanceProperty.Container<HelpArea.ActivityRoute> {
         InstanceProperties(HelpArea.ActivityRoute source){super(source);}
        InstanceProperty<HelpArea.ActivityRoute, HelpArea> area(){return new InstanceProperty<>(source,PackageProperties.helpArea_activityRoute.area);}
        InstanceProperty<HelpArea.ActivityRoute, Class> channel(){return new InstanceProperty<>(source,PackageProperties.helpArea_activityRoute.channel);}
        InstanceProperty<HelpArea.ActivityRoute, BasePlace> place(){return new InstanceProperty<>(source,PackageProperties.helpArea_activityRoute.place);}
      }
      
       InstanceProperties instance(HelpArea.ActivityRoute instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
