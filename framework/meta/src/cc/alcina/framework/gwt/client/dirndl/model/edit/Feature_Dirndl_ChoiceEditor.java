package cc.alcina.framework.gwt.client.dirndl.model.edit;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.gwt.client.dirndl.layout.Feature_Dirndl;
import cc.alcina.framework.gwt.client.dirndl.model.search.Feature_Dirndl_SearchDefinitionEditor;

/**
 * <p>
 * The ChoiceEditor feature - an editable text area which acts as a single or
 * multiple choice selection UI
 *
 * <p>
 * Implementation is pretty complex - because of the complexity of managing DOM
 * contenteditable, to a large extent. It's tracked in an implementation feature
 * in the implementation package
 * 
 * <p>
 * Note that this a specialisation of ContentEditor, so where possible behavior
 * exists there rather than in the ChoiceEditor
 */
/*
 * @formatter:off
 
 Areas:
- keyboard and selection behavior
  - keyboard selection - behaves as though non-editables are single characters
    - shift-select selects whole non-editable
    - delete/backspace deletes whole non-editable
    - future - allow for levels of non-editables, such as search criteria containing criteria values
  - pointer selection - click on a non-editable selects whole non-editable
  - if an unorderered editor, keyboard input is only allowed at the end of the editable (affects keyboard, pointer selection)
- focus handling (editor) - delete uncommitted editors on focusout (ContentEditor unwraps but does not delete)
  - impl note: OnFocusLossUnwrapUncommittedDecorators behaviour
- commit behavior - unlike jira, focusout commits (if changed) and [esc] cancels
- single-selection:
  - existing values are hidden (display none) if the editor is input-dirty
  


  


 @formatter:on
 */
@Feature.Status.Ref(Feature.Status.In_Progress.class)
@Feature.Parent(Feature_Dirndl.class)
public interface Feature_Dirndl_ChoiceEditor extends Feature {
	/**
	 * Since the single selector doesn't have selectable/clearable decorators,
	 * add a clear button _if_ allow null
	 */
	@Feature.Parent(Feature_Dirndl_ChoiceEditor.class)
	@Feature.Status.Ref(Feature.Status.Open.class)
	public interface _SingleSelectionClear extends Feature {
	}
}
