package cc.alcina.framework.common.client.consort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;

import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.util.CommonUtils;

/*
 * -- dependencies :: always attempt to resolve -- preconditions :: if non-null,
 * wait til met before player becomes active -- provides :: use to satisfy other
 * dependencies (only run if required, as well)
 *
 */
public abstract class AbstractPlayer<D> implements Player<D> {
	protected Player.Support<D> support;

	protected AbstractPlayer() {
		support = new Player.Support<>(this);
	}

	public Player.Support<D> support() {
		return support;
	}

	public AbstractPlayer(Runnable runnable) {
		this();
		support().runnable = runnable;
	}

	public abstract static class RepeatingCommandPlayer<D>
			extends AbstractPlayer<D> implements Runnable, RepeatingCommand {
		protected RepeatingCommand delegate;

		public RepeatingCommandPlayer() {
			super(null);
			setAsynchronous(true);
			support().runnable = this;
		}

		@Override
		public boolean execute() {
			if (!getConsort().isRunning()) {
				return false;
			}
			try {
				boolean result = executeRepeatingCommand();
				if (result == false) {
					wasPlayed();
				}
				return result;
			} catch (Throwable e) {
				getConsort().onFailure(e);
				return false;
			}
		}

		protected boolean executeRepeatingCommand() {
			return delegate.execute();
		}
	}

	public abstract static class RunnableAsyncCallbackPlayer<C, D>
			extends AbstractPlayer<D> implements Runnable, AsyncCallback<C> {
		public RunnableAsyncCallbackPlayer() {
			super(null);
			setAsynchronous(true);
			support().runnable = this;
		}

		@Override
		public void onSuccess(C result) {
			wasPlayed();
		}

		@Override
		public void onFailure(Throwable caught) {
			super.onFailure(caught);
		}
	}

	public abstract static class RunnablePlayer<D> extends AbstractPlayer<D>
			implements Runnable {
		public RunnablePlayer() {
			super(null);
			support().runnable = this;
		}
	}
}
