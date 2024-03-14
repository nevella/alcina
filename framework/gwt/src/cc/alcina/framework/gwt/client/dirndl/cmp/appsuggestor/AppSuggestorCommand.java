package cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.function.Predicate;

import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Al;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext;

/**
 * Used to expose ModelEvent types in the app suggestor, depending on the
 * context
 *
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
@ClientVisible
public @interface AppSuggestorCommand {
	/*
	 * In which contexts the action is available
	 */
	Class<? extends CommandContext>[] contexts() default {};

	String description() default "";

	/*
	 * default signifies no filter
	 */
	Class<? extends Filter> filter() default Filter.class;

	String name();

	/*
	 * default value has no OmniEvent, so terminates ancestor chain
	 */
	Class<? extends AppSuggestorEvent> parent() default AppSuggestorEvent.class;

	@Reflected
	public interface Filter
			extends Predicate<Class<? extends AppSuggestorEvent>> {
		public static class IsAdmin implements Filter {
			@Override
			public boolean test(Class<? extends AppSuggestorEvent> t) {
				return PermissionsManager.get().isAdmin();
			}
		}

		public static class IsDeveloper implements Filter {
			@Override
			public boolean test(Class<? extends AppSuggestorEvent> t) {
				return PermissionsManager.isDeveloper();
			}
		}

		public static class IsConsole implements Filter {
			@Override
			public boolean test(Class<? extends AppSuggestorEvent> t) {
				return Al.isConsole();
			}
		}
	}

	public static class Support {
		public static boolean testFilter(
				Class<? extends AppSuggestorEvent> eventClass,
				AppSuggestorCommand omniCommand) {
			Class<? extends Filter> filterClass = omniCommand.filter();
			if (filterClass != AppSuggestorCommand.Filter.class) {
				boolean test = Reflections.newInstance(filterClass)
						.test(eventClass);
				if (!test) {
					return false;
				}
			}
			return true;
		}

		public static Class<? extends CommandContext>[]
				contexts(AppSuggestorCommand command) {
			AppSuggestorCommand cursor = command;
			while (true) {
				if (cursor.contexts().length > 0) {
					return cursor.contexts();
				}
				Class<? extends AppSuggestorEvent> parent = cursor.parent();
				if (parent == AppSuggestorEvent.class) {
					return new Class[0];
				}
				cursor = Reflections.at(parent)
						.annotation(AppSuggestorCommand.class);
			}
		}
	}
}
