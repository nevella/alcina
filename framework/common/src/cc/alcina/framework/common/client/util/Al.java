package cc.alcina.framework.common.client.util;

/**
 * A centralised location for application contexts - 'am I running in a console
 * environment? gwt? a production server'
 */
public class Al {
	public enum Context {
		not_set, console, gwt_dev, gwt_script, test_webapp, production_webapp,
		android_shell, android_app
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

	public static boolean isScript() {
		switch (context) {
		case gwt_script:
			return true;
		default:
			return false;
		}
	}
}
