package cc.alcina.framework.gwt.client.dirndl.model.edit;

import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.behaviour.KeyboardNavigation;
import cc.alcina.framework.gwt.client.dirndl.model.edit.EditArea;
import java.lang.Class;
import java.util.List;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    public static _MultipleSuggestions multipleSuggestions = new _MultipleSuggestions();
    
    public static class _MultipleSuggestions implements TypedProperty.Container {
      public TypedProperty<MultipleSuggestions, List> choices = new TypedProperty<>(MultipleSuggestions.class, "choices");
      public TypedProperty<MultipleSuggestions, List> decorators = new TypedProperty<>(MultipleSuggestions.class, "decorators");
      public TypedProperty<MultipleSuggestions, EditArea> editArea = new TypedProperty<>(MultipleSuggestions.class, "editArea");
      public TypedProperty<MultipleSuggestions, KeyboardNavigation> keyboardNavigation = new TypedProperty<>(MultipleSuggestions.class, "keyboardNavigation");
      public TypedProperty<MultipleSuggestions, List> selectedValues = new TypedProperty<>(MultipleSuggestions.class, "selectedValues");
      public TypedProperty<MultipleSuggestions, Class> valueTransformer = new TypedProperty<>(MultipleSuggestions.class, "valueTransformer");
      public TypedProperty<MultipleSuggestions, List> values = new TypedProperty<>(MultipleSuggestions.class, "values");
    }
    
//@formatter:on
}
