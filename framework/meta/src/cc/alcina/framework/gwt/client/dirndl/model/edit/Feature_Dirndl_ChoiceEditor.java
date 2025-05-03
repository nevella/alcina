package cc.alcina.framework.gwt.client.dirndl.model.edit;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.gwt.client.dirndl.layout.Feature_Dirndl;

/**
 * <p>
 * The ChoiceEditor feature - an editable text area which acts as a single or
 * multiple choice selection UI
 *
 * <p>
 * Implementation is pretty complex - because of the complexity of managing DOM
 * contenteditable, to a large extent. It's tracked in an implementation feature
 * in the implementation package
 */
@Feature.Status.Ref(Feature.Status.In_Progress.class)
@Feature.Parent(Feature_Dirndl.class)
public interface Feature_Dirndl_ChoiceEditor extends Feature {
}
