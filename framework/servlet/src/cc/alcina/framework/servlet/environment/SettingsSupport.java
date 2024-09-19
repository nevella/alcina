package cc.alcina.framework.servlet.environment;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.PersistSettings;

public class SettingsSupport {
	public static <T extends Bindable> T deserializeSettings(Class<T> clazz,
			String settings) {
		T result = null;
		if (settings != null) {
			try {
				result = ReflectiveSerializer.deserializeRpc(settings);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (result == null) {
			result = Reflections.newInstance(clazz);
		}
		result.addPropertyChangeListener(
				evt -> persistSettings(evt.getSource()));
		return result;
	}

	public static void persistSettings(Object settings) {
		String json = ReflectiveSerializer.serialize(settings);
		PersistSettings persistSettings = new RemoteComponentProtocol.Message.PersistSettings();
		persistSettings.value = json;
		Environment.get().queue.send(persistSettings);
	}
}
