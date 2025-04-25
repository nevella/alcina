package cc.alcina.framework.gwt.client.dirndl.model.edit;

import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.model.edit.DecoratorNode;
import cc.alcina.framework.gwt.client.dirndl.model.edit.EditArea;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentModel;
import java.lang.Boolean;
import java.lang.Class;
import java.lang.Object;
import java.lang.String;
import java.util.List;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    public static _ChoiceEditor choiceEditor = new _ChoiceEditor();
    static _ChoiceEditor_ChoiceNode choiceEditor_choiceNode = new _ChoiceEditor_ChoiceNode();
    public static _ChoicesEditorMultiple choicesEditorMultiple = new _ChoicesEditorMultiple();
    public static _DecoratorNode decoratorNode = new _DecoratorNode();
    public static _EditArea editArea = new _EditArea();
    public static _EntityNode entityNode = new _EntityNode();
    
    public static class _ChoiceEditor implements TypedProperty.Container {
      public TypedProperty<ChoiceEditor, List> choices = new TypedProperty<>(ChoiceEditor.class, "choices");
      public TypedProperty<ChoiceEditor, List> decorators = new TypedProperty<>(ChoiceEditor.class, "decorators");
      public TypedProperty<ChoiceEditor, EditArea> editArea = new TypedProperty<>(ChoiceEditor.class, "editArea");
      public TypedProperty<ChoiceEditor, Boolean> magicName = new TypedProperty<>(ChoiceEditor.class, "magicName");
      public TypedProperty<ChoiceEditor, Boolean> repeatableChoices = new TypedProperty<>(ChoiceEditor.class, "repeatableChoices");
      public TypedProperty<ChoiceEditor, Class> valueTransformer = new TypedProperty<>(ChoiceEditor.class, "valueTransformer");
      public TypedProperty<ChoiceEditor, List> values = new TypedProperty<>(ChoiceEditor.class, "values");
    }
    
    static class _ChoiceEditor_ChoiceNode implements TypedProperty.Container {
      TypedProperty<ChoiceEditor.ChoiceNode, String> content = new TypedProperty<>(ChoiceEditor.ChoiceNode.class, "content");
      TypedProperty<ChoiceEditor.ChoiceNode, Boolean> contentEditable = new TypedProperty<>(ChoiceEditor.ChoiceNode.class, "contentEditable");
      TypedProperty<ChoiceEditor.ChoiceNode, DecoratorNode.Descriptor> descriptor = new TypedProperty<>(ChoiceEditor.ChoiceNode.class, "descriptor");
      TypedProperty<ChoiceEditor.ChoiceNode, FragmentModel> fragmentModel = new TypedProperty<>(ChoiceEditor.ChoiceNode.class, "fragmentModel");
      TypedProperty<ChoiceEditor.ChoiceNode, DecoratorNode.InternalModel> internalModel = new TypedProperty<>(ChoiceEditor.ChoiceNode.class, "internalModel");
      TypedProperty<ChoiceEditor.ChoiceNode, String> stringRepresentable = new TypedProperty<>(ChoiceEditor.ChoiceNode.class, "stringRepresentable");
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
    
    public static class _DecoratorNode implements TypedProperty.Container {
      public TypedProperty<DecoratorNode, String> content = new TypedProperty<>(DecoratorNode.class, "content");
      public TypedProperty<DecoratorNode, Boolean> contentEditable = new TypedProperty<>(DecoratorNode.class, "contentEditable");
      public TypedProperty<DecoratorNode, DecoratorNode.Descriptor> descriptor = new TypedProperty<>(DecoratorNode.class, "descriptor");
      public TypedProperty<DecoratorNode, FragmentModel> fragmentModel = new TypedProperty<>(DecoratorNode.class, "fragmentModel");
      public TypedProperty<DecoratorNode, DecoratorNode.InternalModel> internalModel = new TypedProperty<>(DecoratorNode.class, "internalModel");
      public TypedProperty<DecoratorNode, Object> stringRepresentable = new TypedProperty<>(DecoratorNode.class, "stringRepresentable");
      public TypedProperty<DecoratorNode, Boolean> valid = new TypedProperty<>(DecoratorNode.class, "valid");
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
    
    public static class _EntityNode implements TypedProperty.Container {
      public TypedProperty<EntityNode, String> content = new TypedProperty<>(EntityNode.class, "content");
      public TypedProperty<EntityNode, Boolean> contentEditable = new TypedProperty<>(EntityNode.class, "contentEditable");
      public TypedProperty<EntityNode, DecoratorNode.Descriptor> descriptor = new TypedProperty<>(EntityNode.class, "descriptor");
      public TypedProperty<EntityNode, FragmentModel> fragmentModel = new TypedProperty<>(EntityNode.class, "fragmentModel");
      public TypedProperty<EntityNode, DecoratorNode.InternalModel> internalModel = new TypedProperty<>(EntityNode.class, "internalModel");
      public TypedProperty<EntityNode, EntityLocator> stringRepresentable = new TypedProperty<>(EntityNode.class, "stringRepresentable");
    }
    
//@formatter:on
}
