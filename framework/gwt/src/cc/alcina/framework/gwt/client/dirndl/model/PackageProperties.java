package cc.alcina.framework.gwt.client.dirndl.model;

import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.model.CollectionDeltaModel;
import cc.alcina.framework.gwt.client.dirndl.model.HeadingActions;
import java.lang.Object;
import java.lang.String;
import java.util.Collection;
import java.util.List;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    public static _CollectionDeltaModel collectionDeltaModel = new _CollectionDeltaModel();
    static _CollectionDeltaModel_RelativeInsert collectionDeltaModel_relativeInsert = new _CollectionDeltaModel_RelativeInsert();
    public static _HeadingActions headingActions = new _HeadingActions();
    public static _StandardModels_Panel standardModels_panel = new _StandardModels_Panel();
    
    public static class _CollectionDeltaModel implements TypedProperty.Container {
      public TypedProperty<CollectionDeltaModel, Collection> collection = new TypedProperty<>(CollectionDeltaModel.class, "collection");
      public TypedProperty<CollectionDeltaModel, CollectionDeltaModel.RelativeInsert> root = new TypedProperty<>(CollectionDeltaModel.class, "root");
    }
    
    static class _CollectionDeltaModel_RelativeInsert implements TypedProperty.Container {
      TypedProperty<CollectionDeltaModel.RelativeInsert, CollectionDeltaModel.RelativeInsert> after = new TypedProperty<>(CollectionDeltaModel.RelativeInsert.class, "after");
      TypedProperty<CollectionDeltaModel.RelativeInsert, CollectionDeltaModel.RelativeInsert> before = new TypedProperty<>(CollectionDeltaModel.RelativeInsert.class, "before");
      TypedProperty<CollectionDeltaModel.RelativeInsert, Object> collectionElement = new TypedProperty<>(CollectionDeltaModel.RelativeInsert.class, "collectionElement");
      TypedProperty<CollectionDeltaModel.RelativeInsert, Object> element = new TypedProperty<>(CollectionDeltaModel.RelativeInsert.class, "element");
      TypedProperty<CollectionDeltaModel.RelativeInsert, List> flushedContents = new TypedProperty<>(CollectionDeltaModel.RelativeInsert.class, "flushedContents");
    }
    
    public static class _HeadingActions implements TypedProperty.Container {
      public TypedProperty<HeadingActions, List> actions = new TypedProperty<>(HeadingActions.class, "actions");
      public TypedProperty<HeadingActions, String> heading = new TypedProperty<>(HeadingActions.class, "heading");
    }
    
    public static class _StandardModels_Panel implements TypedProperty.Container {
      public TypedProperty<StandardModels.Panel, HeadingActions> header = new TypedProperty<>(StandardModels.Panel.class, "header");
    }
    
//@formatter:on
}
