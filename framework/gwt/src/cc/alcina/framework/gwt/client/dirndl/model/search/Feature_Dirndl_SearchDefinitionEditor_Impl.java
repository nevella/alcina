package cc.alcina.framework.gwt.client.dirndl.model.search;

import cc.alcina.framework.common.client.meta.Feature;

/**
 * Tracks SearchDefinitionEditor implementation
 *
 */
/*
 * @formatter:off
 
Areas:
  - TBD


 @formatter:on
 */
@Feature.Status.Ref(Feature.Status.In_Progress.class)
@Feature.Parent(Feature_Dirndl_SearchDefinitionEditor.class)
public interface Feature_Dirndl_SearchDefinitionEditor_Impl extends Feature {
	/**
	 * cunning impl - use css. the editor has 'initial-focus' on first focus,
	 * removed after first mutation
	 */
	@Feature.Parent(Feature_Dirndl_SearchDefinitionEditor_Impl.class)
	@Feature.Ref(Feature_Dirndl_SearchDefinitionEditor._HintBehaviour.class)
	public interface _HintBehaviour extends Feature {
	}
}
