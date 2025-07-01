package cc.alcina.framework.gwt.client.dirndl.model.edit;

import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.model.edit.DecoratorNode;
import cc.alcina.framework.gwt.client.dirndl.model.edit.EditArea;
import cc.alcina.framework.gwt.client.dirndl.model.edit.StringInput;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentModel;
import java.lang.Boolean;
import java.lang.Class;
import java.lang.Object;
import java.lang.String;
import java.util.Date;
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
    public static _StringInput stringInput = new _StringInput();
    public static _StringInput_DateEditor stringInput_dateEditor = new _StringInput_DateEditor();
    public static _StringInput_DateInput stringInput_dateInput = new _StringInput_DateInput();
    public static _StringInput_Editor stringInput_editor = new _StringInput_Editor();
    
    public static class _ChoiceEditor implements TypedProperty.Container {
      public TypedProperty<ChoiceEditor, List> choices = new TypedProperty<>(ChoiceEditor.class, "choices");
      public TypedProperty<ChoiceEditor, List> decorators = new TypedProperty<>(ChoiceEditor.class, "decorators");
      public TypedProperty<ChoiceEditor, EditArea> editArea = new TypedProperty<>(ChoiceEditor.class, "editArea");
      public TypedProperty<ChoiceEditor, Boolean> magicName = new TypedProperty<>(ChoiceEditor.class, "magicName");
      public TypedProperty<ChoiceEditor, Boolean> magicName2 = new TypedProperty<>(ChoiceEditor.class, "magicName2");
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
      TypedProperty<ChoiceEditor.ChoiceNode, Boolean> magicName = new TypedProperty<>(ChoiceEditor.ChoiceNode.class, "magicName");
      TypedProperty<ChoiceEditor.ChoiceNode, Boolean> selected = new TypedProperty<>(ChoiceEditor.ChoiceNode.class, "selected");
      TypedProperty<ChoiceEditor.ChoiceNode, String> stringRepresentable = new TypedProperty<>(ChoiceEditor.ChoiceNode.class, "stringRepresentable");
    }
    
    public static class _ChoicesEditorMultiple implements TypedProperty.Container {
      public TypedProperty<ChoicesEditorMultiple, List> choices = new TypedProperty<>(ChoicesEditorMultiple.class, "choices");
      public TypedProperty<ChoicesEditorMultiple, List> decorators = new TypedProperty<>(ChoicesEditorMultiple.class, "decorators");
      public TypedProperty<ChoicesEditorMultiple, EditArea> editArea = new TypedProperty<>(ChoicesEditorMultiple.class, "editArea");
      public TypedProperty<ChoicesEditorMultiple, Boolean> magicName = new TypedProperty<>(ChoicesEditorMultiple.class, "magicName");
      public TypedProperty<ChoicesEditorMultiple, Boolean> magicName2 = new TypedProperty<>(ChoicesEditorMultiple.class, "magicName2");
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
      public TypedProperty<DecoratorNode, Boolean> magicName = new TypedProperty<>(DecoratorNode.class, "magicName");
      public TypedProperty<DecoratorNode, Boolean> selected = new TypedProperty<>(DecoratorNode.class, "selected");
      public TypedProperty<DecoratorNode, Object> stringRepresentable = new TypedProperty<>(DecoratorNode.class, "stringRepresentable");
      public TypedProperty<DecoratorNode, Boolean> valid = new TypedProperty<>(DecoratorNode.class, "valid");
    }
    
    public static class _EditArea implements TypedProperty.Container {
      public TypedProperty<EditArea, Boolean> contentEditable = new TypedProperty<>(EditArea.class, "contentEditable");
      public TypedProperty<EditArea, String> currentValue = new TypedProperty<>(EditArea.class, "currentValue");
      public TypedProperty<EditArea, Boolean> focusOnBind = new TypedProperty<>(EditArea.class, "focusOnBind");
      public TypedProperty<EditArea, FragmentModel> fragmentModel = new TypedProperty<>(EditArea.class, "fragmentModel");
      public TypedProperty<EditArea, Boolean> insertEditorStartCursorTarget = new TypedProperty<>(EditArea.class, "insertEditorStartCursorTarget");
      public TypedProperty<EditArea, Boolean> insertInterNonEditableCursorTargets = new TypedProperty<>(EditArea.class, "insertInterNonEditableCursorTargets");
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
      public TypedProperty<EntityNode, Boolean> magicName = new TypedProperty<>(EntityNode.class, "magicName");
      public TypedProperty<EntityNode, Boolean> selected = new TypedProperty<>(EntityNode.class, "selected");
      public TypedProperty<EntityNode, EntityLocator> stringRepresentable = new TypedProperty<>(EntityNode.class, "stringRepresentable");
    }
    
    public static class _StringInput implements TypedProperty.Container {
      public TypedProperty<StringInput, String> autocomplete = new TypedProperty<>(StringInput.class, "autocomplete");
      public TypedProperty<StringInput, Boolean> commitOnEnter = new TypedProperty<>(StringInput.class, "commitOnEnter");
      public TypedProperty<StringInput, String> currentValue = new TypedProperty<>(StringInput.class, "currentValue");
      public TypedProperty<StringInput, Boolean> disabled = new TypedProperty<>(StringInput.class, "disabled");
      public TypedProperty<StringInput, Boolean> ensureContentVisible = new TypedProperty<>(StringInput.class, "ensureContentVisible");
      public TypedProperty<StringInput, Boolean> focusOnBind = new TypedProperty<>(StringInput.class, "focusOnBind");
      public TypedProperty<StringInput, Boolean> moveCaretToEndOnFocus = new TypedProperty<>(StringInput.class, "moveCaretToEndOnFocus");
      public TypedProperty<StringInput, String> placeholder = new TypedProperty<>(StringInput.class, "placeholder");
      public TypedProperty<StringInput, Boolean> preserveSelectionOverFocusChange = new TypedProperty<>(StringInput.class, "preserveSelectionOverFocusChange");
      public TypedProperty<StringInput, String> rows = new TypedProperty<>(StringInput.class, "rows");
      public TypedProperty<StringInput, Boolean> selectAllOnFocus = new TypedProperty<>(StringInput.class, "selectAllOnFocus");
      public TypedProperty<StringInput, String> spellcheck = new TypedProperty<>(StringInput.class, "spellcheck");
      public TypedProperty<StringInput, String> tag = new TypedProperty<>(StringInput.class, "tag");
      public TypedProperty<StringInput, String> title = new TypedProperty<>(StringInput.class, "title");
      public TypedProperty<StringInput, String> type = new TypedProperty<>(StringInput.class, "type");
      public TypedProperty<StringInput, String> value = new TypedProperty<>(StringInput.class, "value");
    }
    
    public static class _StringInput_DateEditor implements TypedProperty.Container {
      public TypedProperty<StringInput.DateEditor, StringInput.DateInput> input = new TypedProperty<>(StringInput.DateEditor.class, "input");
      public TypedProperty<StringInput.DateEditor, Date> value = new TypedProperty<>(StringInput.DateEditor.class, "value");
    }
    
    public static class _StringInput_DateInput implements TypedProperty.Container {
      public TypedProperty<StringInput.DateInput, String> autocomplete = new TypedProperty<>(StringInput.DateInput.class, "autocomplete");
      public TypedProperty<StringInput.DateInput, Boolean> commitOnEnter = new TypedProperty<>(StringInput.DateInput.class, "commitOnEnter");
      public TypedProperty<StringInput.DateInput, String> currentValue = new TypedProperty<>(StringInput.DateInput.class, "currentValue");
      public TypedProperty<StringInput.DateInput, Boolean> disabled = new TypedProperty<>(StringInput.DateInput.class, "disabled");
      public TypedProperty<StringInput.DateInput, Boolean> ensureContentVisible = new TypedProperty<>(StringInput.DateInput.class, "ensureContentVisible");
      public TypedProperty<StringInput.DateInput, Boolean> focusOnBind = new TypedProperty<>(StringInput.DateInput.class, "focusOnBind");
      public TypedProperty<StringInput.DateInput, Boolean> moveCaretToEndOnFocus = new TypedProperty<>(StringInput.DateInput.class, "moveCaretToEndOnFocus");
      public TypedProperty<StringInput.DateInput, String> placeholder = new TypedProperty<>(StringInput.DateInput.class, "placeholder");
      public TypedProperty<StringInput.DateInput, Boolean> preserveSelectionOverFocusChange = new TypedProperty<>(StringInput.DateInput.class, "preserveSelectionOverFocusChange");
      public TypedProperty<StringInput.DateInput, String> rows = new TypedProperty<>(StringInput.DateInput.class, "rows");
      public TypedProperty<StringInput.DateInput, Boolean> selectAllOnFocus = new TypedProperty<>(StringInput.DateInput.class, "selectAllOnFocus");
      public TypedProperty<StringInput.DateInput, String> spellcheck = new TypedProperty<>(StringInput.DateInput.class, "spellcheck");
      public TypedProperty<StringInput.DateInput, String> tag = new TypedProperty<>(StringInput.DateInput.class, "tag");
      public TypedProperty<StringInput.DateInput, String> title = new TypedProperty<>(StringInput.DateInput.class, "title");
      public TypedProperty<StringInput.DateInput, String> type = new TypedProperty<>(StringInput.DateInput.class, "type");
      public TypedProperty<StringInput.DateInput, String> value = new TypedProperty<>(StringInput.DateInput.class, "value");
    }
    
    public static class _StringInput_Editor implements TypedProperty.Container {
      public TypedProperty<StringInput.Editor, String> autocomplete = new TypedProperty<>(StringInput.Editor.class, "autocomplete");
      public TypedProperty<StringInput.Editor, Boolean> commitOnEnter = new TypedProperty<>(StringInput.Editor.class, "commitOnEnter");
      public TypedProperty<StringInput.Editor, String> currentValue = new TypedProperty<>(StringInput.Editor.class, "currentValue");
      public TypedProperty<StringInput.Editor, Boolean> disabled = new TypedProperty<>(StringInput.Editor.class, "disabled");
      public TypedProperty<StringInput.Editor, Boolean> ensureContentVisible = new TypedProperty<>(StringInput.Editor.class, "ensureContentVisible");
      public TypedProperty<StringInput.Editor, Boolean> focusOnBind = new TypedProperty<>(StringInput.Editor.class, "focusOnBind");
      public TypedProperty<StringInput.Editor, Boolean> moveCaretToEndOnFocus = new TypedProperty<>(StringInput.Editor.class, "moveCaretToEndOnFocus");
      public TypedProperty<StringInput.Editor, String> placeholder = new TypedProperty<>(StringInput.Editor.class, "placeholder");
      public TypedProperty<StringInput.Editor, Boolean> preserveSelectionOverFocusChange = new TypedProperty<>(StringInput.Editor.class, "preserveSelectionOverFocusChange");
      public TypedProperty<StringInput.Editor, String> rows = new TypedProperty<>(StringInput.Editor.class, "rows");
      public TypedProperty<StringInput.Editor, Boolean> selectAllOnFocus = new TypedProperty<>(StringInput.Editor.class, "selectAllOnFocus");
      public TypedProperty<StringInput.Editor, String> spellcheck = new TypedProperty<>(StringInput.Editor.class, "spellcheck");
      public TypedProperty<StringInput.Editor, String> tag = new TypedProperty<>(StringInput.Editor.class, "tag");
      public TypedProperty<StringInput.Editor, String> title = new TypedProperty<>(StringInput.Editor.class, "title");
      public TypedProperty<StringInput.Editor, String> type = new TypedProperty<>(StringInput.Editor.class, "type");
      public TypedProperty<StringInput.Editor, String> value = new TypedProperty<>(StringInput.Editor.class, "value");
    }
    
//@formatter:on
}
