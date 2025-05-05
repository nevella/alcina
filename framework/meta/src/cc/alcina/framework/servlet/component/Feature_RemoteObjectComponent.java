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

	/**
	 * <p>
	 * the client transport emits an observable when the inflight client to
	 * server message state is changed, which may be used by the ui to indicate
	 * slow processing time, or inflight-message state (for test clients)
	 */
	@Feature.Parent(Feature_RemoteObjectComponent.class)
	public interface Feature_ClientMessageState extends Feature {
	}

	/**
	 * <p>
	 * a devtools call to __romcom_dp() will dump the protocol state:
	 * <ul>
	 * <li>client dumps its own state (active messages)
	 * <li>client sends a message 'dumpstate'
	 * <li>server dumps its state, sends response
	 * <li>client dumps server state
	 * <li>the client transport emits an observable when the inflight client to
	 * server message state is changed, which may be used by the ui to indicate
	 * slow processing time, or inflight-message state (for test clients)
	 * </ul>
	 */
	@Feature.Parent(Feature_RemoteObjectComponent.class)
	public interface Feature_ClientDebug extends Feature {
	}

	/**
	 * <p>
	 * repeated events (mousewheel, scroll) can overload the rpc system, so
	 * throttle in two ways:
	 * <ul>
	 * <li>limit the permissible in-flight envelope count
	 * <li>squelch the preview of repeating events
	 * <li>(if needed) throttle (EventCollator) those event types
	 * </ul>
	 */
	@Feature.Parent(Feature_RemoteObjectComponent.class)
	public interface Feature_ClientEventThrottling extends Feature {
	}

	/**
	 * Implementation features link here
	 */
	@Feature.Parent(Feature_RemoteObjectComponent.class)
	public interface Feature_Impl extends Feature {
	}
}
