package cc.alcina.framework.gwt.client.dirndl.model.edit;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.gwt.client.dirndl.layout.Feature_Dirndl;

/**
 * Tracks MultipleSuggestor implementation
 *
 */
/*
 * @formatter:off
 
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
@Feature.Parent(Feature_Dirndl.class)
public interface Feature_Dirndl_ChoiceSuggestions extends Feature {
}
