package cc.alcina.framework.servlet.component.romcom.client.common.logic;

import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;

@Registration.Singleton
public class RemoteComponentSettings {
	public static RemoteComponentSettings get() {
		return Registry.impl(RemoteComponentSettings.class);
	}

	public static String getSettings() {
		return get().getSettings0();
	}

	public static void setSettings(String settings) {
		get().setSettings0(settings);
	}

	void setSettings0(String settings) {
		Storage.getLocalStorageIfSupported().setItem(key(), settings);
	}

	String key() {
		return Ax.format("/settings/%s", Window.Location.getPath());
	}

	String getSettings0() {
		return Storage.getLocalStorageIfSupported().getItem(key());
	}
}
