package cc.alcina.extras.webdriver;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.meta.Feature_Ui_support;
import cc.alcina.framework.gwt.client.story.doc.Feature_StoryDoc;

/**
 *
 * Make tests declarative, and allow specification of type (test; doc; tour) and
 * ordering
 *
 * Also fixme/wd
 * 
 * [Note - implementation of this Feature is the {@link Feature_StoryDoc} class]
 */
@Feature.Status.Ref(Feature.Status.Open.class)
@Feature.Parent(Feature_Ui_support.class)
public interface Feature_DeclarativeTests extends Feature {
}
