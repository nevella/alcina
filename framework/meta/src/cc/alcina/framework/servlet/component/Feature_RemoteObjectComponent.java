package cc.alcina.framework.servlet.component;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.meta.Feature_Ui_support;

/**
 * <h4>Documentation of general ROMCOM features, such as the
 * EnvironmentManager</h4>
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
	public interface _EnvironmentManager extends Feature {
	}

	/**
	 * <p>
	 * the client transport emits an observable when the inflight client to
	 * server message state is changed, which may be used by the ui to indicate
	 * slow processing time, or inflight-message state (for test clients)
	 */
	@Feature.Parent(Feature_RemoteObjectComponent.class)
	public interface _ClientMessageState extends Feature {
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
	public interface _ClientDebug extends Feature {
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
	public interface _ClientEventThrottling extends Feature {
	}

	/**
	 * Implementation features link here
	 */
	@Feature.Parent(Feature_RemoteObjectComponent.class)
	public interface _Impl extends Feature {
	}

	/**
	 * <p>
	 * FIXME - dirndl.2
	 * <p>
	 * Handoff is the process where the application state + dom model are passed
	 * from the server to the client (once the client has loaded/initialised the
	 * js model)
	 * 
	 * <p>
	 * It's not necessary - but reduces server load
	 * 
	 * <pre>
	 * <code>
	 * Sketch:
	 * 
	- create a state model [server/romcom - browser/Client] - sync
	- state is either global [tm etc] or on the model [dca]
	- global state must register, and implement an interface. can be in a temp non-syncable state
	- in the meantime...just flip at the appropriate point (in the case of jade, with the model calls cached]
	- use instanceoracle
	 -- write to local storage, cli-id, model signature, date
	 -- hvae the idea of a safepoint (top-level place switch)
	 * </code>
	 * </pre>
	 */
	@Feature.Parent(Feature_RemoteObjectComponent.class)
	public interface _Handoff extends Feature {
	}

	@Feature.Parent(Feature_RemoteObjectComponent.class)
	public interface _OffsetProtocol extends Feature {
	}
}
