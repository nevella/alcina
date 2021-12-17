package cc.alcina.framework.entity.util;

import java.lang.StackWalker.StackFrame;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.LooseContextInstance;

@RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class)
public class ThreadlocalLooseContextProvider extends LooseContext {
	private static ThreadLocal threadLocalInstance = new ThreadLocal() {
		@Override
		protected synchronized Object initialValue() {
			ThreadlocalLooseContextProvider provider = new ThreadlocalLooseContextProvider();
			return provider;
		}
	};

	public static ThreadlocalLooseContextProvider cast() {
		return (ThreadlocalLooseContextProvider) LooseContext.getInstance();
	}

	/**
	 * Convenience "override" of LooseContextProvider.get()
	 */
	public static ThreadlocalLooseContextProvider get() {
		return ThreadlocalLooseContextProvider.cast();
	}

	public static ThreadlocalLooseContextProvider ttmInstance() {
		return new ThreadlocalLooseContextProvider();
	}

	private static boolean debugStackEntry;

	public static boolean isDebugStackEntry() {
		return ThreadlocalLooseContextProvider.debugStackEntry;
	}

	public static void setDebugStackEntry(boolean debugStackEntry) {
		ThreadlocalLooseContextProvider.debugStackEntry = debugStackEntry;
	}

	@Override
	public LooseContext getT() {
		return (LooseContext) threadLocalInstance.get();
	}

	@Override
	protected LooseContextInstance getContext0() {
		if (context == null) {
			context = new LooseContextInstanceJvm();
		}
		return context;
	}

	static class LooseContextInstanceJvm extends LooseContextInstance {
		@Override
		public void push() {
			super.push();
			if (isDebugStackEntry()) {
				StackInfo stackInfo = new StackInfo(true);
				changes.add(stackInfo);
				set(STACK_INFO, stackInfo);
			}
		}
		public LooseContextInstanceJvm() {
		}

		List<StackInfo> changes = new ArrayList<>();

		public void pop() {
			if (isDebugStackEntry()) {
				StackInfo info = get(STACK_INFO);
				StackInfo stackInfo = new StackInfo(false);
				changes.add(stackInfo);
				if (info == null) {
					Ax.sysLogHigh("Unbalanced stack");
					changes.forEach(Ax::out);
				}
				Preconditions.checkState(info.equals(stackInfo));
			}
			super.pop();
		}

		static class StackInfo {
			private String className;

			private String methodName;

			private MethodType methodType;

			private boolean push;

			@Override
			public String toString() {
				return Ax.format("%s - %s.%s - %s", push ? "Push" : "Pop ",
						className, methodName, methodType);
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

			public StackInfo(boolean push) {
				this.push = push;
				StackWalker
						.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
						.walk(s -> {
							s.filter(frame -> notLooseContextFrame(frame))
									.limit(1).forEach(frame -> {
										this.className = frame.getClassName();
										this.methodName = frame.getMethodName();
										this.methodType = frame.getMethodType();
									});
							return null;
						});
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
		}
	}

	@Override
	protected void removePerThreadContext0() {
		threadLocalInstance.remove();
	}
}
