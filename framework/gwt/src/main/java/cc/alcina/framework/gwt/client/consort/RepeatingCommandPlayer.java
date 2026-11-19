package cc.alcina.framework.gwt.client.consort;

import com.google.gwt.core.client.Scheduler.RepeatingCommand;

import cc.alcina.framework.common.client.consort.Player;

public abstract class RepeatingCommandPlayer<D> extends Player<D>
		implements Runnable, RepeatingCommand {
	protected RepeatingCommand delegate;

	public RepeatingCommandPlayer() {
		super(null);
		setAsynchronous(true);
		runnable = this;
	}

	@Override
	public boolean execute() {
		if (!consort.isRunning()) {
			return false;
		}
		try {
			boolean result = executeRepeatingCommand();
			if (result == false) {
				wasPlayed();
			}
			return result;
		} catch (Throwable e) {
			consort.onFailure(e);
			return false;
		}
	}

	protected boolean executeRepeatingCommand() {
		return delegate.execute();
	}
}