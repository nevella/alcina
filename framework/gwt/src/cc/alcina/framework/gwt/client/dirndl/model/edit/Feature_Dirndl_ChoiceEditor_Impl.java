package cc.alcina.framework.gwt.client.dirndl.model.edit;

import cc.alcina.framework.common.client.meta.Feature;

/**
 * Tracks ChoiceEditor implementation
 *
 */
/*
 * @formatter:off
 
 Areas:
 - keyboard and selection behavior
   -  
  


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
