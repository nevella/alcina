package cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestor;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor;
import java.lang.Boolean;
import java.lang.String;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    public static _AppSuggestor appSuggestor = new _AppSuggestor();
    
    public static class _AppSuggestor implements TypedProperty.Container {
      public TypedProperty<AppSuggestor, String> acceptedFilterText = new TypedProperty<>(AppSuggestor.class, "acceptedFilterText");
      public TypedProperty<AppSuggestor, AppSuggestor.Attributes> attributes = new TypedProperty<>(AppSuggestor.class, "attributes");
      public TypedProperty<AppSuggestor, Boolean> currentSelectionHandled = new TypedProperty<>(AppSuggestor.class, "currentSelectionHandled");
      public TypedProperty<AppSuggestor, String> filterText = new TypedProperty<>(AppSuggestor.class, "filterText");
      public TypedProperty<AppSuggestor, Suggestor> suggestor = new TypedProperty<>(AppSuggestor.class, "suggestor");
      public static class InstanceProperties extends InstanceProperty.Container<AppSuggestor> {
        public  InstanceProperties(AppSuggestor source){super(source);}
        public InstanceProperty<AppSuggestor, String> acceptedFilterText(){return new InstanceProperty<>(source,PackageProperties.appSuggestor.acceptedFilterText);}
        public InstanceProperty<AppSuggestor, AppSuggestor.Attributes> attributes(){return new InstanceProperty<>(source,PackageProperties.appSuggestor.attributes);}
        public InstanceProperty<AppSuggestor, Boolean> currentSelectionHandled(){return new InstanceProperty<>(source,PackageProperties.appSuggestor.currentSelectionHandled);}
        public InstanceProperty<AppSuggestor, String> filterText(){return new InstanceProperty<>(source,PackageProperties.appSuggestor.filterText);}
        public InstanceProperty<AppSuggestor, Suggestor> suggestor(){return new InstanceProperty<>(source,PackageProperties.appSuggestor.suggestor);}
      }
      
      public  InstanceProperties instance(AppSuggestor instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
