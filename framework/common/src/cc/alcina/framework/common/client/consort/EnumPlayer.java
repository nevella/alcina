package cc.alcina.framework.common.client.consort;

import java.util.Collection;
import java.util.Collections;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.consort.Player.RunnablePlayer;
import cc.alcina.framework.common.client.util.Ax;

public abstract class EnumPlayer<E extends Enum> extends RunnablePlayer<E> {
	protected E from;

	protected E to;

	public EnumPlayer(E state) {
		super();
		int idx = state.ordinal();
		to = state;
		from = (E) (idx == 0 ? null
				: state.getClass().getEnumConstants()[idx - 1]);
	}

	public EnumPlayer(E from, E to) {
		this.from = from;
		this.to = to;
	}

	@Override
	public Collection<E> getProvides() {
		return Collections.singletonList(to);
	}

	@Override
	public Collection<E> getRequires() {
		return (Collection<E>) (from == null ? Collections.emptyList()
				: Collections.singletonList(from));
	}

	@Override
	public String toString() {
		return Ax.format("%s [%s->%s]", getClass().getSimpleName(), from, to);
	}

	public abstract static class EnumRunnableAsyncCallbackPlayer<C, E extends Enum>
			extends EnumPlayer<E> implements AsyncCallback<C>, Runnable {
		public EnumRunnableAsyncCallbackPlayer(E state) {
			super(state);
			runnable = this;
			setAsynchronous(true);
		}

		public EnumRunnableAsyncCallbackPlayer(E from, E to) {
			super(from, to);
			setAsynchronous(true);
			runnable = this;
		}

		@Override
		public void onFailure(Throwable caught) {
			consort.onFailure(caught);
		}

		@Override
		public void onSuccess(C result) {
			wasPlayed();
		}
	}
}