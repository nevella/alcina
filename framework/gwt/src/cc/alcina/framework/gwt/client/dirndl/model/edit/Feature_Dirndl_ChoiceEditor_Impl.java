package cc.alcina.framework.gwt.client.dirndl.model.edit;

import cc.alcina.framework.common.client.meta.Feature;

/**
 * Tracks ChoiceEditor implementation
 *
 */
/*
 * @formatter:off
 
  [these are some points in the implementation which are probably all done]

## tmorra

- migra to ProjectModel (romcom)
- get test to display
- test focuses (at end) the editable [will need to implement romcom/selection here]
- possibly implement fragmentmodel/pending as well.
- that (onclick/focus) causes decoratornodegen [extend hasdecorators]
- chooser answer/ask works on choice contents, not choice (hook up to original property)
- document pending vs 


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
