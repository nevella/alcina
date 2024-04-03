package cc.alcina.framework.servlet.component;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.meta.Feature_Ui_support;

/**
 * <h4>Documentation of general ROMCOM features, such as the manager</h4>
 *
 *
 */
@Feature.Status.Ref(Feature.Status.Open.class)
@Feature.Parent(Feature_Ui_support.class)
@Feature.Type.Ref(Feature.Type.Ui_support.class)
public interface Feature_RemoteObjectComponent extends Feature {
	@Feature.Parent(Feature_RemoteObjectComponent.class)
	public interface Feature_Environment extends Feature {
	}

	/**
	 * <p>
	 * The EnvironmentManager is responsible for the lifecycle of environments -
	 * particularly clearing references (since Environments consume significant
	 * memory), but also providing a list of active environments
	 */
	@Feature.Parent(Feature_RemoteObjectComponent.class)
	public interface Feature_EnvironmentManager extends Feature {
	}
}
