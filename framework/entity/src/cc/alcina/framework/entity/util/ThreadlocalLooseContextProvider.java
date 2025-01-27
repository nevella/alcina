package cc.alcina.framework.entity.util;

import java.lang.StackWalker.StackFrame;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.context.LooseContextInstance;

@Registration(ClearStaticFieldsOnAppShutdown.class)
public class ThreadlocalLooseContextProvider extends LooseContext {
	private static ThreadLocal<ThreadlocalLooseContextProvider> threadLocalInstance = new ThreadLocal<>() {
		@Override
		protected synchronized ThreadlocalLooseContextProvider initialValue() {
			ThreadlocalLooseContextProvider provider = new ThreadlocalLooseContextProvider();
			return provider;
		}
	};

	private static boolean debugStackEntry;

	public static ThreadlocalLooseContextProvider cast() {
		return (ThreadlocalLooseContextProvider) LooseContext.getInstance();
	}

	/**
	 * Convenience "override" of LooseContextProvider.get()
	 */
	public static ThreadlocalLooseContextProvider get() {
		return ThreadlocalLooseContextProvider.cast();
	}

	public static boolean isDebugStackEntry() {
		return ThreadlocalLooseContextProvider.debugStackEntry;
	}

	public static void setDebugStackEntry(boolean debugStackEntry) {
		ThreadlocalLooseContextProvider.debugStackEntry = debugStackEntry;
	}

	public static ThreadlocalLooseContextProvider ttmInstance() {
		return new ThreadlocalLooseContextProvider();
	}

	@Override
	protected LooseContextInstance getContext0() {
		if (context == null) {
			context = new LooseContextInstanceJvm();
		}
		return context;
	}

	@Override
	public LooseContext getT() {
		return (LooseContext) threadLocalInstance.get();
	}

	@Override
	protected void removePerThreadContext0() {
		ThreadlocalLooseContextProvider contextProvider = threadLocalInstance
				.get();
		if (contextProvider != null && contextProvider.context != null) {
			((LooseContextInstanceJvm) contextProvider.context)
					.beforeRemovePerContext();
		}
		threadLocalInstance.remove();
	}

	static class LooseContextInstanceJvm extends LooseContextInstance {
		int pushCount;

		private Stack<StackInfo> stackInfoStack;

		List<StackInfo> changes = new ArrayList<>();

		public LooseContextInstanceJvm() {
		}

		protected void allowUnbalancedFrameRemoval(Class clazz,
				String pushMethodName) {
			if (isDebugStackEntry()) {
				stackInfoStack.peek().allowUnbalancedFrameRemoval(clazz,
						pushMethodName);
			}
		}

		@Override
		protected void cloneFieldsTo(LooseContextInstance other) {
			// does *not* clone any fields of this subclass (they're all
			// debug-related)
			super.cloneFieldsTo(other);
		}

		void beforeRemovePerContext() {
			if (isDebugStackEntry() && pushCount != 0) {
				Ax.sysLogHigh("Unbalanced stack");
				changes.forEach(Ax::out);
				throw new IllegalStateException(
						"Clearing context with non-zero stack depth");
			}
		}

		public void pop() {
			if (isDebugStackEntry()) {
				StackInfo info = stackInfoStack.size() > 0
						? stackInfoStack.pop()
						: null;
				StackInfo stackInfo = new StackInfo(false, pushCount);
				changes.add(stackInfo);
				if (info == null) {
					Ax.sysLogHigh("Unbalanced stack");
					changes.forEach(Ax::out);
					Preconditions.checkState(false);
				}
				if (!info.equals(stackInfo)
						&& !info.allowUnbalancedFrameRemoval) {
					Ax.sysLogHigh("Unbalanced stack");
					Ax.out(info);
					Ax.out(stackInfo);
					Ax.out("==================");
					changes.forEach(Ax::out);
					Preconditions.checkState(info.equals(stackInfo));
				}
			}
			super.pop();
			if (isDebugStackEntry()) {
				pushCount--;
				if (pushCount == 0) {
					changes.clear();
				}
			}
		}

		@Override
		public void push() {
			super.push();
			if (isDebugStackEntry()) {
				pushCount++;
				if (stackInfoStack == null) {
					stackInfoStack = new Stack<>();
				}
				StackInfo stackInfo = new StackInfo(true, pushCount);
				changes.add(stackInfo);
				stackInfoStack.push(stackInfo);
			}
		}

		static class StackInfo {
			private String className;

			private String methodName;

			private MethodType methodType;

			private boolean push;

			private int depth;

			private int lineNumber;

			boolean allowUnbalancedFrameRemoval;

			public StackInfo(boolean push, int depth) {
				this.push = push;
				this.depth = depth;
				StackWalker
						.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
						.walk(s -> {
							s.filter(frame -> notLooseContextFrame(frame))
									.limit(1).forEach(frame -> {
										this.className = frame.getClassName();
										this.methodName = frame.getMethodName();
										this.methodType = frame.getMethodType();
										this.lineNumber = frame.getLineNumber();
									});
							return null;
						});
			}

			void allowUnbalancedFrameRemoval(Class clazz,
					String pushMethodName) {
				allowUnbalancedFrameRemoval = className.equals(clazz.getName())
						&& methodName.equals(pushMethodName);
			}

			@Override
			public boolean equals(Object obj) {
				if (obj instanceof StackInfo) {
					StackInfo o = (StackInfo) obj;
					return className.equals(o.className)
							&& methodName.equals(o.methodName)
							&& methodType.equals(o.methodType);
				} else {
					return super.equals(obj);
				}
			}

			private boolean notLooseContextFrame(StackFrame frame) {
				if (StackInfo.class.getName().equals(frame.getClassName())) {
					return false;
				} else if (LooseContextInstanceJvm.class.getName()
						.equals(frame.getClassName())) {
					return false;
				} else if (LooseContextInstance.class.getName()
						.equals(frame.getClassName())) {
					return false;
				} else if (LooseContext.class.getName()
						.equals(frame.getClassName())) {
					return false;
				} else if (ThreadlocalLooseContextProvider.class.getName()
						.equals(frame.getClassName())) {
					return false;
				} else {
					return true;
				}
			}

			@Override
			public String toString() {
				return Ax.format("%s - %s - %s.%s::%s - %s",
						push ? "Push" : "Pop ", CommonUtils.padThree(depth),
						className, methodName, lineNumber, methodType);
			}
		}
	}
}
