package cc.alcina.framework.common.client.state;

import java.util.Collection;

/*
 * -- dependencies :: always attempt to resolve
 * -- preconditions :: if non-null, wait til met before player becomes active
 * -- provides :: use to satisfy other dependencies (only run if required, as well)
 *
 */
public abstract class Player<D> {
	public static final transient int LOW = 1;

	public static final transient int PRIORITY_NORMAL = 100;

	public static final transient int PRIORITY_IMMEDIATE = 1000;

	protected Runnable runnable;

	private Consort<D> consort;

	public Player(Runnable runnable) {
		this.runnable = runnable;
	}
	/*
	 * normally, call resolveRequires()
	 */
	public Collection<D> getRequires() {
		return null;
	}
	
	public Collection<D> resolveRequires(){
		return consort.resolveRequires(this);
	}

	public Collection<D> getPreconditions() {
		return null;
	}

	public int getPriority() {
		return PRIORITY_NORMAL;
	}

	public Collection<D> getProvides() {
		return null;
	}
	

	public Consort<D> getConsort() {
		return this.consort;
	}

	public boolean isCancellable() {
		return true;
	}

	public void play() {
		runnable.run();
		wasPlayed();
	}

	public void setConsort(Consort<D> consort) {
		this.consort = consort;
	}

	protected void wasPlayed() {
		if (consort != null) {
			consort.wasPlayed(this);
		}
	}

	public boolean isPerConsortSingleton() {
		return false;
	}

	public boolean isRemoveAfterPlay() {
		return true;
	}

	public abstract static class RunnablePlayer<D> extends Player<D> implements
			Runnable {
		public RunnablePlayer() {
			super(null);
			runnable = this;
		}
	}
}
