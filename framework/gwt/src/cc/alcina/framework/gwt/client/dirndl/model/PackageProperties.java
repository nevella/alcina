package cc.alcina.framework.gwt.client.dirndl.model;

import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.model.CollectionDeltaModel;
import java.lang.Object;
import java.util.Collection;
import java.util.List;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    public static _CollectionDeltaModel collectionDeltaModel = new _CollectionDeltaModel();
    static _CollectionDeltaModel_RelativeInsert collectionDeltaModel_relativeInsert = new _CollectionDeltaModel_RelativeInsert();
    
    public static class _CollectionDeltaModel implements TypedProperty.Container {
      public TypedProperty<CollectionDeltaModel, Collection> collection = new TypedProperty<>(CollectionDeltaModel.class, "collection");
      public TypedProperty<CollectionDeltaModel, CollectionDeltaModel.RelativeInsert> root = new TypedProperty<>(CollectionDeltaModel.class, "root");
    }
    
    static class _CollectionDeltaModel_RelativeInsert implements TypedProperty.Container {
      TypedProperty<CollectionDeltaModel.RelativeInsert, CollectionDeltaModel.RelativeInsert> after = new TypedProperty<>(CollectionDeltaModel.RelativeInsert.class, "after");
      TypedProperty<CollectionDeltaModel.RelativeInsert, CollectionDeltaModel.RelativeInsert> before = new TypedProperty<>(CollectionDeltaModel.RelativeInsert.class, "before");
      TypedProperty<CollectionDeltaModel.RelativeInsert, Object> element = new TypedProperty<>(CollectionDeltaModel.RelativeInsert.class, "element");
      TypedProperty<CollectionDeltaModel.RelativeInsert, List> flushedContents = new TypedProperty<>(CollectionDeltaModel.RelativeInsert.class, "flushedContents");
    }
    
//@formatter:on
}
