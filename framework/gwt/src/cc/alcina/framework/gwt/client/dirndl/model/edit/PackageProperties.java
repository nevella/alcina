package cc.alcina.framework.gwt.client.dirndl.model.edit;

import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.model.edit.EditArea;
import java.lang.Class;
import java.util.List;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    public static _MultipleSuggestor multipleSuggestor = new _MultipleSuggestor();
    
    public static class _MultipleSuggestor implements TypedProperty.Container {
      public TypedProperty<MultipleSuggestor, EditArea> area = new TypedProperty<>(MultipleSuggestor.class, "area");
      public TypedProperty<MultipleSuggestor, List> choices = new TypedProperty<>(MultipleSuggestor.class, "choices");
      public TypedProperty<MultipleSuggestor, List> selectedValues = new TypedProperty<>(MultipleSuggestor.class, "selectedValues");
      public TypedProperty<MultipleSuggestor, Class> valueTransformer = new TypedProperty<>(MultipleSuggestor.class, "valueTransformer");
      public TypedProperty<MultipleSuggestor, List> values = new TypedProperty<>(MultipleSuggestor.class, "values");
    }
    
//@formatter:on
}
