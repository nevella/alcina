package cc.alcina.extras.webdriver;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.meta.Feature_Ui_support;

/**
 *
 * Make tests declarative, and allow specification of type (test; doc; tour) and
 * ordering
 *
 * Also fixme/wd
 */
@Feature.Status.Ref(Feature.Status.Open.class)
@Feature.Parent(Feature_Ui_support.class)
public interface Feature_DeclarativeTests extends Feature {
}
