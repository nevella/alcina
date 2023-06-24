package cc.alcina.framework.gwt.client.dirndl.model.edit;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.meta.Feature.Type.Ui_support;
import cc.alcina.framework.gwt.client.dirndl.layout.Feature_Dirndl;

/**
 * Tracks ContentDecorator improvements/tests
 *
 * FIXME - Feature_Dirndl_ContentDecorator
 */
@Feature.Status.Ref(Feature.Status.In_Progress.class)
@Feature.Parent(Feature_Dirndl.class)
public interface Feature_Dirndl_ContentDecorator extends Feature {
	/**
	 * <p>
	 * If the cursor is at the end of a decorator tag, move it to an editable
	 * location - either to the start of the next logical text node (if not a
	 * decorator descendant), or insert a zero-length text node after the
	 * decorator and position there
	 *
	 * <p>
	 * State - RelativeInputModel positions selection points at 'end' - which
	 * ...nah. Look at ProseMirror :
	 *
	 * Ensure zero-widthspace before + after each decorator
	 *
	 * Remove cursor expansion validation
	 *
	 * validate on CE setup, input
	 *
	 * FIXME - ContentEditableSync - revisit
	 *
	 */
	@Feature.Status.Ref(Feature.Status.In_Progress.class)
	@Feature.Parent(Feature_Dirndl_ContentDecorator.class)
	@Feature.Type.Ref(Ui_support.class)
	public interface Constraint_NonSuggesting_DecoratorTag_EndOfDecoratorCursor
			extends Feature {
	}

	/**
	 * <p>
	 * Selection modification: decorator tags should not allow partial
	 * selection:
	 * <ul>
	 * <li>When not suggesting, selection of part of a decorator tag results in
	 * selection of the whole tag (DONE)
	 * <li>When building the decorator (typing into the decorator tag, which
	 * acts as a suggestor filter), stop decorating if the DOM selection becomes
	 * non-collapsed (actually - no - editing should certainly allow
	 * non-collapsed selection), or exits (any part of the DOM Selection) the
	 * decorator (TODO)
	 * <li>On mutation, check all mentions via dom traversal, (TODO: optimise
	 * via DomDocument.getElementByTagName with an index driven by
	 * DomMutations)), delete any with empty text/no entity id (DONE) (although
	 * it turns out the mention was deleted - weird font resurrection is webkit
	 * CR issue)
	 * <li>If typing into a text node that's a direct child of the CE root,
	 * ensure no font tags are generated (fix previous issue)
	 * </ul>
	 *
	 *
	 */
	@Feature.Status.Ref(Feature.Status.In_Progress.class)
	@Feature.Parent(Feature_Dirndl_ContentDecorator.class)
	@Feature.Type.Ref(Ui_support.class)
	public interface Constraint_NonSuggesting_DecoratorTag_Selection
			extends Feature {
	}

	/**
	 * Exit the suggestor if the trigger is deleted
	 *
	 * @author nick@alcina.cc
	 *
	 */
	@Feature.Status.Ref(Feature.Status.Open.class)
	@Feature.Parent(Feature_Dirndl_ContentDecorator.class)
	@Feature.Type.Ref(Ui_support.class)
	public interface Constraint_NonSuggesting_DecoratorTag_TriggerDeletion
			extends Feature {
	}

	/**
	 * Sync a content editable node to a model tree rooted in a container which
	 * provides quick access to something like 'nodes by tag/model class'
	 *
	 * <ul>
	 * <li>Can't use model fields, since the document structure is arbitrary
	 * <li>Need to model 'generic element/attr/txt/pi/comment' and descend
	 * <li>Model comment, cdata, pi as special tags
	 * </ul>
	 *
	 * @author nick@alcina.cc
	 *
	 */
	@Feature.Status.Ref(Feature.Status.Open.class)
	@Feature.Parent(Feature_Dirndl_ContentDecorator.class)
	@Feature.Type.Ref(Ui_support.class)
	public interface ContentEditableSync extends Feature {
	}

	/**
	 * Resize to fit based on content domrect
	 *
	 * Constraint: no more than 50% screen height (unless full-height), and
	 * customisable if non-editing
	 *
	 * @author nick@alcina.cc
	 *
	 */
	@Feature.Status.Ref(Feature.Status.Open.class)
	@Feature.Parent(Feature_Dirndl_ContentDecorator.class)
	@Feature.Type.Ref(Ui_support.class)
	public interface Resizing extends Feature {
	}
}
