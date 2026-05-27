package cc.alcina.framework.servlet.component.romcom;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.servlet.component.Feature_RemoteObjectComponent;

@Feature.Status.Ref(Feature.Status.Open.class)
// ref the client-visible package version of this 'feature'
@Feature.Parent(Feature_RemoteObjectComponent._Impl.class)
@Feature.Type.Ref(Feature.Type.Ui_implementation.class)
public interface Feature_Romcom_Impl extends Feature {
	/*
	 * Notes - romcom has to do some fancy layer passing to set the server
	 * cookie post auth change (login/logout)
	 */
	@Feature.Parent(Feature_Romcom_Impl.class)
	public interface _Authentication extends Feature {
	}

	/*
	 * Romcom reuses the client transform manager (and per-client-instance
	 * Domain)
	 */
	@Feature.Parent(Feature_Romcom_Impl.class)
	public interface _Transforms extends Feature {
	}

	/**
	 * <p>
	 * Windowstate (offsets, scroll etc) is key - batches are sent async from
	 * the client and accessed as needed by the server, to avoid blocking
	 * getBoundingClientRect() calls and such
	 * 
	 * <p>
	 * This feature is utterly critical for getting reasonable romcom
	 * performance - there's even a flag to totally disable invokesync to test
	 * 
	 * <p>
	 */
	@Feature.Parent(Feature_Romcom_Impl.class)
	public interface _WindowState extends Feature {
	}

	/**
	 * <p>
	 * This optimises client/server comms - the server needs to know the offsets
	 * of potentially many elements, but they can be computed from _relative_
	 * offsets (relative to the parent) which change infrequently. So optimise
	 * via a caching protocol. Implemented + a large performance improvement
	 * 
	 * <p>
	 * wip - ds.late - check the offsetprotocol json size + possibly client-side
	 * zip
	 */
	@Feature.Parent(Feature_Romcom_Impl.class)
	public interface _OffsetProtocol extends Feature {
	}

	/**
	 * <p>
	 * This optimises client/server comms - large string constants are cached
	 * client-side in local storage, and MutationRecord.innerMarkup is ...
	 * munged? extended? to support
	 * 
	 * <p>
	 * some considerations: onload cache invalidation and client-to-server cache
	 * comms
	 */
	@Feature.Parent(Feature_Romcom_Impl.class)
	public interface _StringProtocol extends Feature {
	}

	/**
	 * <ul>
	 * <li>Optimise various metrics (time to first paint, bytes to first paint.)
	 * <li>The metrics themselves are computed in RomcomSessionEntry
	 * <li>Page opts
	 * <ul>
	 * <li>Client should elide/mark the script in the head tag
	 * </ul>
	 * <li>Event opts
	 * <ul>
	 * <li>Specify max continuous-gesture (scroll, mousemove) event collation
	 * </ul>
	 * <li>Protocol opts
	 * <ul>
	 * <li>Make types final where possible to reduce type info
	 * <li>Session should be only id/auth post 1st msg
	 * <li>use default collections (e.g. arraylist)
	 * <li>(Later) zip client -- server
	 * </ul>
	 * </ul>
	 */
	@Feature.Parent(Feature_Romcom_Impl.class)
	public interface _Performance extends Feature {
	}

	/**
	 * <p>
	 * The goal here is to defer load of non-essential style/image content (and
	 * handoff javascript).
	 * <p>
	 * Briefly, the algorithm is:
	 * <ul>
	 * <li>client calls
	 * ResourceLoader.injectStyle(resourceName,resourcePrototype) (rather than
	 * StyleInjector.inject)
	 * <li>Al.isBrowser - this injects as normal (default ResourceLoader)
	 * <li>Al.isRomcom - this loads via ResourceLoaderRomcom (EnvRegistration)
	 * <ul>
	 * <li>Does the current signature exist on the browser? If yes, inject
	 * directly (assisted by StringProtocol)
	 * <li>If no, add a tracker to the queue
	 * <li>After sufficient time, (possibly just .5s) tracker begins resource
	 * load:
	 * <ul>
	 * <li>Send 'ResourceRequired' message to client
	 * <li>Client loads (via either url or LoadResource/Resource tx), sends
	 * 'ResourceLoaded' to server
	 * <li>Loader/tracker injects style
	 * </ul>
	 * </ul>
	 * </ul>
	 */
	@Feature.Parent(Feature_Romcom_Impl.class)
	public interface _ResourceLoader extends Feature {
	}
}
