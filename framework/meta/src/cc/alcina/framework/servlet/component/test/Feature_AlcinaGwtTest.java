package cc.alcina.framework.servlet.component.test;

import cc.alcina.framework.common.client.meta.Feature_Ui_support;
import cc.alcina.framework.common.client.meta.Feature;

/**
 * This component provides a shell for testing advanced alcina/gwt features,
 * such as the devmode protocol subversion and remote mutation sync
 */
@Feature.Status.Ref(Feature.Status.Open.class)
@Feature.Parent(Feature_Ui_support.class)
public interface Feature_AlcinaGwtTest extends Feature {
}
