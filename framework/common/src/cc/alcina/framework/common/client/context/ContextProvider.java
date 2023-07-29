package cc.alcina.framework.common.client.context;

import java.util.function.Function;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.util.LooseContext;

public interface ContextProvider<C, F extends ContextFrame> {
	static <C, F extends ContextFrame> ContextProvider<C, F> createProvider(
			Function<C, F> frameConstructor, Runnable onPostRegisterCreated,
			C initialContext, Class<F> type, boolean multiple) {
		if (multiple) {
			return new Multiple<>(frameConstructor, onPostRegisterCreated,
					type);
		} else {
			return new Single<>(frameConstructor, initialContext,
					onPostRegisterCreated);
		}
	}

	F contextFrame();

	F createFrame(C context);

	void postRegisterCreatedFrame();

	void registerFrame(F frame);

	/**
	 * Piggybacks off LooseContext - the frame instance is simply that
	 * registered in the current LooseContext
	 * 
	 * 
	 *
	 * @param <F>
	 */
	public static class Multiple<C, F extends ContextFrame>
			implements ContextProvider<C, F> {
		private Function<C, F> frameConstructor;

		private String contextKey;

		private Runnable onPostRegisterCreated;

		public Multiple(Function<C, F> frameConstructor,
				Runnable onPostRegisterCreated, Class<F> frameType) {
			this.frameConstructor = frameConstructor;
			this.onPostRegisterCreated = onPostRegisterCreated;
			this.contextKey = frameType.getName();
		}

		@Override
		public F contextFrame() {
			F frame = LooseContext.get(contextKey);
			Preconditions.checkNotNull(frame, "Context frame not registered");
			return frame;
		}

		@Override
		public F createFrame(C context) {
			F frame = frameConstructor.apply(context);
			registerFrame(frame);
			postRegisterCreatedFrame();
			return frame;
		}

		@Override
		public void postRegisterCreatedFrame() {
			if (onPostRegisterCreated != null) {
				onPostRegisterCreated.run();
			}
		}

		@Override
		public void registerFrame(F frame) {
			LooseContext.set(contextKey, frame);
		}
	}

	/*
	 * Non thread-safe (generally for single-threaded vms)
	 */
	public static class Single<C, F extends ContextFrame>
			implements ContextProvider<C, F> {
		private F frame;

		private Function<C, F> frameConstructor;

		private Runnable onPostRegisterCreated;

		private C initialContext;

		public Single(Function<C, F> frameConstructor, C initialContext,
				Runnable onPostRegisterCreated) {
			this.frameConstructor = frameConstructor;
			this.initialContext = initialContext;
			this.onPostRegisterCreated = onPostRegisterCreated;
		}

		@Override
		public F contextFrame() {
			if (frame == null) {
				createFrame(initialContext);
			}
			return frame;
		}

		@Override
		public F createFrame(C context) {
			frame = frameConstructor.apply(context);
			postRegisterCreatedFrame();
			return frame;
		}

		@Override
		public void postRegisterCreatedFrame() {
			if (onPostRegisterCreated != null) {
				onPostRegisterCreated.run();
			}
		}

		@Override
		public void registerFrame(F frame) {
			throw new UnsupportedOperationException();
		}
	}
}
