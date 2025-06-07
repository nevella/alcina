package cc.alcina.framework.common.client.csobjects.view;

import cc.alcina.framework.common.client.csobjects.view.EntityTransformModel;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.common.client.publication.ContentDefinition;
import java.lang.Integer;
import java.lang.Long;
import java.lang.String;
import java.util.Date;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    public static _DomainView domainView = new _DomainView();
    
    public static class _DomainView implements TypedProperty.Container {
      public TypedProperty<DomainView, Date> creationDate = new TypedProperty<>(DomainView.class, "creationDate");
      public TypedProperty<DomainView, ContentDefinition> entityDefinition = new TypedProperty<>(DomainView.class, "entityDefinition");
      public TypedProperty<DomainView, String> entityDefinitionSerialized = new TypedProperty<>(DomainView.class, "entityDefinitionSerialized");
      public TypedProperty<DomainView, EntityTransformModel> entityTransformModel = new TypedProperty<>(DomainView.class, "entityTransformModel");
      public TypedProperty<DomainView, String> entityTransformModelSerialized = new TypedProperty<>(DomainView.class, "entityTransformModelSerialized");
      public TypedProperty<DomainView, Long> id = new TypedProperty<>(DomainView.class, "id");
      public TypedProperty<DomainView, Date> lastModificationDate = new TypedProperty<>(DomainView.class, "lastModificationDate");
      public TypedProperty<DomainView, Long> localId = new TypedProperty<>(DomainView.class, "localId");
      public TypedProperty<DomainView, String> name = new TypedProperty<>(DomainView.class, "name");
      public TypedProperty<DomainView, Integer> versionNumber = new TypedProperty<>(DomainView.class, "versionNumber");
    }
    
//@formatter:on
}
