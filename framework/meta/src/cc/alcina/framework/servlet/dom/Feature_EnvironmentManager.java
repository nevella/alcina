package cc.alcina.framework.servlet.dom;

import cc.alcina.framework.common.client.meta.Feature;

/**
 * <h3>Status notes for the EnvironmentManager</h3>
 * <p>
 * Most applicable singletons/statics (GWT client reachable, basically) now
 * support multiple clients - see
 * https://docs.google.com/spreadsheets/d/1zYT-egG3E1tN-tECNy8X345_2-wKDl9whJrWnFQx2OQ/edit#gid=841747916
 * <p>
 * The two exceptions are Al (Alcina context) - what to do about GWT.isClient? -
 * and Window.scroll dispatch (slightly tricky for perf reasons). These don't
 * need to be addressed until there's a specific requirenent (aka a very rich
 * client)
 */
@Feature.Type.Ref(Feature.Type.Ui_support.class)
public interface Feature_EnvironmentManager extends Feature {
}