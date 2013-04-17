package cc.alcina.framework.common.client.state;

import java.util.Collection;

/*
 * -- dependencies :: always attempt to resolve
 * -- preconditions :: if non-null, wait til met before player becomes active
 * -- provides :: use to satisfy other dependencies (only run if required, as well)
 *
 */
public abstract class Player {
	public static final transient int LOW = 1;

	public static final transient int PRIORITY_NORMAL = 100;

	public static final transient int PRIORITY_IMMEDIATE = 1000;

	private Runnable runnable;

	private Zone zone;

	public Player(Runnable runnable) {
		this.runnable = runnable;
	}

	public Collection getDependencies() {
		return null;
	}

	public Collection getPreconditions() {
		return null;
	}

	public int getPriority() {
		return PRIORITY_NORMAL;
	}

	public Collection getProvides() {
		return null;
	}

	public Zone getZone() {
		return this.zone;
	}

	public boolean isCancellable() {
		return true;
	}

	public void play() {
		runnable.run();
		wasPlayed();
	}

	public void setZone(Zone zone) {
		this.zone = zone;
	}

	protected void wasPlayed() {
		if (zone != null) {
			zone.wasPlayed(this);
		}
	}

	public boolean isPerZoneSingleton() {
		return false;
	}

	public boolean isRemoveAfterPlay() {
		return true;
	}
}
