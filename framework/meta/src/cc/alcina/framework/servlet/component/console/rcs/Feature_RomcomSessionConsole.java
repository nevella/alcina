package cc.alcina.framework.servlet.component.console.rcs;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.servlet.component.console.Feature_ServerConsole;

/**
 * <h4>The romcom session console</h4>
 * <p>
 * List active + past sessions, filter by conditions such as 'long wait time',
 * 'large packet' etc
 */
@Feature.Status.Ref(Feature.Status.Open.class)
@Feature.Parent(Feature_ServerConsole.class)
public interface Feature_RomcomSessionConsole extends Feature {
	/*
	 * the dashboard displays various category summaries + a detail view of the
	 * active + historical sessions
	 */
	@Feature.Parent(Feature_RomcomSessionConsole.class)
	public interface _Dashboard extends Feature {
	}

	/*
	 * The cache can be an fs-backed summary store (from an fs-backed sequence
	 * store), or a similar persistent-object backed store
	 */
	@Feature.Parent(Feature_RomcomSessionConsole.class)
	public interface _Cache extends Feature {
	}

	/*
	 * Prune that cache
	 */
	@Feature.Parent(Feature_RomcomSessionConsole.class)
	public interface _Retention extends Feature {
	}

	/*
	 * Collect metrics of interest (large packets, slow first/second paint,
	 * exceptions) + add to the dashboard + search filters
	 */
	@Feature.Parent(Feature_RomcomSessionConsole.class)
	public interface _Canned extends Feature {
	}

	/*
	 * Ability to replay a session, assoc. UI
	 */
	@Feature.Parent(Feature_RomcomSessionConsole.class)
	public interface _Replay extends Feature {
	}
}
