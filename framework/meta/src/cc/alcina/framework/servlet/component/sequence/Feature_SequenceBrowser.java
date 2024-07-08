package cc.alcina.framework.servlet.component.sequence;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.meta.Feature_Ui_feature;

/**
 * <h4>A UI for browsing sequences - with an app suggestor, a sequence table, a
 * detail view</h4>
 * <p>
 * Initial features/use cases:
 * <ul>
 * <li>"load ma" - specifies that the browser should render the sequence 'ma'
 * (details in the devconsole's local config)
 * <li>'high button' - highlight the sequence elements that contain property
 * text 'button', display the first in the details view with the content
 * highlighted
 * </ul>
 *
 *
 */
@Feature.Status.Ref(Feature.Status.Open.class)
@Feature.Parent(Feature_Ui_feature.class)
public interface Feature_SequenceBrowser extends Feature {
}
