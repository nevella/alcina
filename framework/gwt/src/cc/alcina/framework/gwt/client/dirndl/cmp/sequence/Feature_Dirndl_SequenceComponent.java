package cc.alcina.framework.gwt.client.dirndl.cmp.sequence;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.gwt.client.dirndl.layout.Feature_Dirndl;

/**
 * <p>
 * The SequenceComponent feature - a coupled sequence definition editor,
 * sequence view (table display) ad detail view (row display)
 *
 * <p>
 * Standalone implementation is the SequenceBrowser romcom component (which
 * handles an arbitrary sequence place), but the component can also be embedded
 * (see the Gallery)
 * 
 */
/*
 * @formatter:off
- Keep user-visibile feature spec here, [e.g. 'enter commits']
 @formatter:on
 */
@Feature.Status.Ref(Feature.Status.In_Progress.class)
@Feature.Parent(Feature_Dirndl.class)
public interface Feature_Dirndl_SequenceComponent extends Feature {
	/**
	 * <p>
	 * Committing the definition editor (press enter in the editor) causes a
	 * filter and url/place update
	 * 
	 */
	@Feature.Parent(Feature_Dirndl_SequenceComponent.class)
	public interface _DefinitionEditor_Coupling extends Feature {
	}
}
