package cc.alcina.framework.gwt.client.logic.reflection.test;

import java.util.function.Consumer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.ReflectionModule;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.resolution.ClientModule;
import cc.alcina.framework.common.client.reflection.ModuleReflector;

public class ReachabilityModuleTest {
	public void test() {
		Module1.get(m1 -> m1.log());
		Module2.get(m2 -> m2.log());
	}

	@Registration(LeftoverReflect.class)
	public static class LeftoverReflect {
		public void log() {
			RootPanel.get().add(new Label(getClass().getName()));
		}
	}

	@ReflectionModule("Module1")
	public static class Module1 extends ClientModule<Module1> {
		public static void get(Consumer<Module1> callback) {
			Module1 singleton = (Module1) registered.get(Module1.class);
			if (singleton != null) {
				callback.accept(singleton);
			} else {
				GWT.runAsync(Module1.class, new RunAsyncCallback() {
					@Override
					public void onFailure(Throwable reason) {
						throw new WrappedRuntimeException(reason);
					}

					@Override
					public void onSuccess() {
						callback.accept(new Module1());
					}
				});
			}
		}

		protected Module1() {
			new Leftover().log("asef");
		}

		public void log() {
			new Leftover().log("asef");
			Registry.impl(Module1Reflect.class).log();
		}

		@Override
		protected ModuleReflector createClientReflector() {
			return GWT.create(Module1Reflector.class);
		}

		@ReflectionModule("Module1")
		public abstract static class Module1Reflector extends ModuleReflector {
		}
	}

	@Registration(Module1Reflect.class)
	public static class Module1Reflect {
		public void log() {
			RootPanel.get().add(new Label(getClass().getName()));
		}
	}

	@ReflectionModule("Module2")
	public static class Module2 extends ClientModule<Module1> {
		public static void get(Consumer<Module2> callback) {
			Module2 singleton = (Module2) registered.get(Module2.class);
			if (singleton != null) {
				callback.accept(singleton);
			} else {
				GWT.runAsync(Module2.class, new RunAsyncCallback() {
					@Override
					public void onFailure(Throwable reason) {
						throw new WrappedRuntimeException(reason);
					}

					@Override
					public void onSuccess() {
						callback.accept(new Module2());
					}
				});
			}
		}

		public void log() {
			new Leftover().log("asefjj");
			Registry.impl(Module2Reflect.class).log();
		}

		@Override
		protected ModuleReflector createClientReflector() {
			return GWT.create(Module2Reflector.class);
		}

		@ReflectionModule("Module2")
		public abstract static class Module2Reflector extends ModuleReflector {
		}
	}

	@Registration(Module2Reflect.class)
	public static class Module2Reflect {
		public void log() {
			RootPanel.get().add(new Label(getClass().getName()));
		}
	}

	static class Leftover {
		public void log(String string) {
			RootPanel.get().add(new Label(string));
			Registry.impl(LeftoverReflect.class).log();
		}
	}
}
