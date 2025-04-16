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
    
    public static _ChoiceEditor choiceEditor = new _ChoiceEditor();
    public static _ChoicesEditorMultiple choicesEditorMultiple = new _ChoicesEditorMultiple();
    public static _EditArea editArea = new _EditArea();
    
    public static class _ChoiceEditor implements TypedProperty.Container {
      public TypedProperty<ChoiceEditor, List> choices = new TypedProperty<>(ChoiceEditor.class, "choices");
      public TypedProperty<ChoiceEditor, List> decorators = new TypedProperty<>(ChoiceEditor.class, "decorators");
      public TypedProperty<ChoiceEditor, EditArea> editArea = new TypedProperty<>(ChoiceEditor.class, "editArea");
      public TypedProperty<ChoiceEditor, Boolean> magicName = new TypedProperty<>(ChoiceEditor.class, "magicName");
      public TypedProperty<ChoiceEditor, Boolean> repeatableChoices = new TypedProperty<>(ChoiceEditor.class, "repeatableChoices");
      public TypedProperty<ChoiceEditor, Class> valueTransformer = new TypedProperty<>(ChoiceEditor.class, "valueTransformer");
      public TypedProperty<ChoiceEditor, List> values = new TypedProperty<>(ChoiceEditor.class, "values");
    }
    
    public static class _ChoicesEditorMultiple implements TypedProperty.Container {
      public TypedProperty<ChoicesEditorMultiple, List> choices = new TypedProperty<>(ChoicesEditorMultiple.class, "choices");
      public TypedProperty<ChoicesEditorMultiple, List> decorators = new TypedProperty<>(ChoicesEditorMultiple.class, "decorators");
      public TypedProperty<ChoicesEditorMultiple, EditArea> editArea = new TypedProperty<>(ChoicesEditorMultiple.class, "editArea");
      public TypedProperty<ChoicesEditorMultiple, Boolean> magicName = new TypedProperty<>(ChoicesEditorMultiple.class, "magicName");
      public TypedProperty<ChoicesEditorMultiple, Boolean> repeatableChoices = new TypedProperty<>(ChoicesEditorMultiple.class, "repeatableChoices");
      public TypedProperty<ChoicesEditorMultiple, List> selectedValues = new TypedProperty<>(ChoicesEditorMultiple.class, "selectedValues");
      public TypedProperty<ChoicesEditorMultiple, Class> valueTransformer = new TypedProperty<>(ChoicesEditorMultiple.class, "valueTransformer");
      public TypedProperty<ChoicesEditorMultiple, List> values = new TypedProperty<>(ChoicesEditorMultiple.class, "values");
    }
    
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
    
//@formatter:on
}
