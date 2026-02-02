package cc.alcina.framework.gwt.client.dirndl.model.search;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.gwt.client.dirndl.layout.Feature_Dirndl;

/**
 * <p>
 * The SearchDefinitionEditor feature - an editable search definition area, a
 * specialisation of the general ContentEditor
 *
 * <p>
 * Implementation is pretty complex, although more complexity is in the parent
 * really. It's tracked in an implementation feature in the implementation
 * package
 * 
 */
/*
 * @formatter:off
 
 Areas:
- TBD (track ChoiceEditor feature)
- Keep user-visibile feature spec here, [e.g. 'enter commits']


  


 @formatter:on
 */
@Feature.Status.Ref(Feature.Status.In_Progress.class)
@Feature.Parent(Feature_Dirndl.class)
public interface Feature_Dirndl_SearchDefinitionEditor extends Feature {
	/**
	 * <p>
	 * Clicking enter on a searchable data field (data, input, select etc) moves
	 * the cursor to the next suggestor possiblity
	 * 
	 * <p>
	 * Clicking enter when suggesting, if nothing is suggested, causes a
	 * searchdef commit (and search)
	 */
	@Feature.Parent(Feature_Dirndl_SearchDefinitionEditor.class)
	public interface _EnterBehaviour extends Feature {
	}
}
