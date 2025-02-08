package cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor;

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
    }
    
//@formatter:on
}
