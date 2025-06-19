package cc.alcina.framework.gwt.client.dirndl.model.edit;

import cc.alcina.framework.common.client.meta.Feature;

/**
 * Tracks ChoiceEditor implementation
 *
 */
/*
 * @formatter:off
 
Areas:
  - keyboard and selection behavior (Behaviors)
    - ExtendKeyboardNavigationAction
      Manages keyboard selection mutation (left, right arrows) to maintain content-non-editable logic.
	  Basically, arrow onto a non-editable selects the non-editable, next arrow moves off
    - EditInsertBehavior
	  Insert blank text nodes where appropriate (given editable insert constraints) to allow edits at appropriate locations
	  e.g. between two non-editables, insert a blank text node to allow choice insert there
	  Also ensure the editor contains one blank text node
	- NonEditableSelectionBehavior
	  If the text contents of a non-editable are selected, mark it (attribute) as selected
	  Also select entire non-editable (isolate) if partially selected
	  Also shift to blank text node if at start/end of non-editable element (normal clicks will select containing text)
	- CommitBehavior
	  Handle [esc] reverts changes, [enter] commits (and returns focus to the outer editable)
  


 @formatter:on
 */
@Feature.Status.Ref(Feature.Status.In_Progress.class)
@Feature.Parent(Feature_Dirndl_ChoiceEditor.class)
public interface Feature_Dirndl_ChoiceEditor_Impl extends Feature {
	/**
	 * <p>
	 * Documents the event (processobservable) sequence which occurs from when a
	 * user clicks on a choiceeditor to the display of a suggestor overlay
	 *
	 * Constraint: no more than 50% screen height (unless full-height), and
	 * customisable if non-editing
	 *
	 * 
	 *
	 */
	/*
	 * @formatter:off



	 * @formatter:on
	 */
	@Feature.Status.Ref(Feature.Status.Open.class)
	@Feature.Parent(Feature_Dirndl_ChoiceEditor.class)
	@Feature.Type.Ref(Feature.Type.Ui_support.class)
	public interface Decorator_To_Suggestor_Event_Sequence extends Feature {
	}
}
