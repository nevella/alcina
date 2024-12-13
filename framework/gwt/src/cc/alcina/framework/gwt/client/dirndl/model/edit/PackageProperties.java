package cc.alcina.framework.gwt.client.dirndl.model.edit;

import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.model.edit.EditArea;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentModel;
import java.lang.Boolean;
import java.lang.Class;
import java.lang.String;
import java.util.List;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    public static _EditArea editArea = new _EditArea();
    public static _MultipleSuggestions multipleSuggestions = new _MultipleSuggestions();
    
    public static class _EditArea implements TypedProperty.Container {
      public TypedProperty<EditArea, Boolean> contentEditable = new TypedProperty<>(EditArea.class, "contentEditable");
      public TypedProperty<EditArea, String> currentValue = new TypedProperty<>(EditArea.class, "currentValue");
      public TypedProperty<EditArea, Boolean> focusOnBind = new TypedProperty<>(EditArea.class, "focusOnBind");
      public TypedProperty<EditArea, FragmentModel> fragmentModel = new TypedProperty<>(EditArea.class, "fragmentModel");
      public TypedProperty<EditArea, String> placeholder = new TypedProperty<>(EditArea.class, "placeholder");
      public TypedProperty<EditArea, Boolean> stripFontTagsOnInput = new TypedProperty<>(EditArea.class, "stripFontTagsOnInput");
      public TypedProperty<EditArea, String> tag = new TypedProperty<>(EditArea.class, "tag");
      public TypedProperty<EditArea, String> value = new TypedProperty<>(EditArea.class, "value");
    }
    
    public static class _MultipleSuggestions implements TypedProperty.Container {
      public TypedProperty<MultipleSuggestions, List> choices = new TypedProperty<>(MultipleSuggestions.class, "choices");
      public TypedProperty<MultipleSuggestions, List> decorators = new TypedProperty<>(MultipleSuggestions.class, "decorators");
      public TypedProperty<MultipleSuggestions, EditArea> editArea = new TypedProperty<>(MultipleSuggestions.class, "editArea");
      public TypedProperty<MultipleSuggestions, List> selectedValues = new TypedProperty<>(MultipleSuggestions.class, "selectedValues");
      public TypedProperty<MultipleSuggestions, Class> valueTransformer = new TypedProperty<>(MultipleSuggestions.class, "valueTransformer");
      public TypedProperty<MultipleSuggestions, List> values = new TypedProperty<>(MultipleSuggestions.class, "values");
    }
    
//@formatter:on
}
