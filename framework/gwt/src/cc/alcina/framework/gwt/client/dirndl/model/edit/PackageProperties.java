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
    public static _ChoiceSuggestions choiceSuggestions = new _ChoiceSuggestions();
    
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
    
    public static class _ChoiceSuggestions implements TypedProperty.Container {
      public TypedProperty<ChoiceSuggestions, List> choices = new TypedProperty<>(ChoiceSuggestions.class, "choices");
      public TypedProperty<ChoiceSuggestions, List> decorators = new TypedProperty<>(ChoiceSuggestions.class, "decorators");
      public TypedProperty<ChoiceSuggestions, EditArea> editArea = new TypedProperty<>(ChoiceSuggestions.class, "editArea");
      public TypedProperty<ChoiceSuggestions, Boolean> magicName = new TypedProperty<>(ChoiceSuggestions.class, "magicName");
      public TypedProperty<ChoiceSuggestions, List> selectedValues = new TypedProperty<>(ChoiceSuggestions.class, "selectedValues");
      public TypedProperty<ChoiceSuggestions, Class> valueTransformer = new TypedProperty<>(ChoiceSuggestions.class, "valueTransformer");
      public TypedProperty<ChoiceSuggestions, List> values = new TypedProperty<>(ChoiceSuggestions.class, "values");
    }
    
//@formatter:on
}
