package cc.alcina.framework.servlet.component.console;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.meta.Feature.Type.Ui_app;
import cc.alcina.framework.common.client.meta.Feature_Ui_app;
import cc.alcina.framework.common.client.meta.Feature_Ui_support;
import cc.alcina.framework.servlet.component.Feature_RemoteObjectComponent;

/**
 * <h4>Alcina console tools</h4>
 * <p>
 * UI tools to support alcina operation
 */
@Feature.Status.Ref(Feature.Status.Open.class)
@Feature.Parent(Feature_Ui_app.class)
public interface Feature_AlcinaConsole extends Feature {
}
