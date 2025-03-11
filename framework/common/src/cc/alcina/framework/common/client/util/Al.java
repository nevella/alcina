package cc.alcina.framework.common.client.util;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.context.LooseContext;

/**
 * A centralised location for application contexts - 'am I running in a console
 * environment? gwt? a production server'
 */
public class Al {
	public static final LooseContext.Key<Boolean> CONTEXT_multiThreaded = LooseContext
			.key(Al.class, "CONTEXT_multiThreaded");

	public enum Context {
		not_set, console, gwt_dev, gwt_script, test_webapp, production_webapp,
		android_shell, android_app_dev, android_app_production
	}

	public static Context context = Context.not_set;

	public static boolean isConsole() {
		switch (context) {
		case console:
			return true;
		default:
			return false;
		}
	}

	public static boolean isBrowser() {
		switch (context) {
		case gwt_dev:
		case gwt_script:
			return true;
		default:
			return false;
		}
	}

	public static boolean isRomcom() {
		return GWT.isClient() && !isBrowser();
	}

	public static boolean isScript() {
		switch (context) {
		case gwt_script:
			return true;
		default:
			return false;
		}
	}

	public static boolean isNonProduction() {
		return !isProduction();
	}

	public static boolean isProduction() {
		switch (context) {
		case gwt_script:
		case production_webapp:
		case android_app_production:
			return true;
		default:
			return false;
		}
	}

	public static boolean isMultiThreaded() {
		return !isBrowser() && CONTEXT_multiThreaded.optional().orElse(true);
	}
}
