package cc.alcina.framework.common.client.csobjects.view;

import cc.alcina.framework.common.client.csobjects.view.EntityTransformModel;
import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
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
      public static class InstanceProperties extends InstanceProperty.Container<DomainView> {
        public  InstanceProperties(DomainView source){super(source);}
        public InstanceProperty<DomainView, Date> creationDate(){return new InstanceProperty<>(source,PackageProperties.domainView.creationDate);}
        public InstanceProperty<DomainView, ContentDefinition> entityDefinition(){return new InstanceProperty<>(source,PackageProperties.domainView.entityDefinition);}
        public InstanceProperty<DomainView, String> entityDefinitionSerialized(){return new InstanceProperty<>(source,PackageProperties.domainView.entityDefinitionSerialized);}
        public InstanceProperty<DomainView, EntityTransformModel> entityTransformModel(){return new InstanceProperty<>(source,PackageProperties.domainView.entityTransformModel);}
        public InstanceProperty<DomainView, String> entityTransformModelSerialized(){return new InstanceProperty<>(source,PackageProperties.domainView.entityTransformModelSerialized);}
        public InstanceProperty<DomainView, Long> id(){return new InstanceProperty<>(source,PackageProperties.domainView.id);}
        public InstanceProperty<DomainView, Date> lastModificationDate(){return new InstanceProperty<>(source,PackageProperties.domainView.lastModificationDate);}
        public InstanceProperty<DomainView, Long> localId(){return new InstanceProperty<>(source,PackageProperties.domainView.localId);}
        public InstanceProperty<DomainView, String> name(){return new InstanceProperty<>(source,PackageProperties.domainView.name);}
        public InstanceProperty<DomainView, Integer> versionNumber(){return new InstanceProperty<>(source,PackageProperties.domainView.versionNumber);}
      }
      
      public  InstanceProperties instance(DomainView instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
