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
	 * <li>If typing into a whitespace-only box, ensure no font tags are
	 * generated (fix previous issue)
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
}
