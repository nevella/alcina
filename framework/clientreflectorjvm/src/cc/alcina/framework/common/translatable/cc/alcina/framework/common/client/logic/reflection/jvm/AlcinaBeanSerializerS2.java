package cc.alcina.framework.common.client.logic.reflection.jvm;

import cc.alcina.framework.common.client.util.AlcinaBeanSerializer;
import com.google.gwt.core.client.GwtScriptOnly;
/**
 * never used, but means we don't have to do weird bunnies for the hosted-mode version
 */
@GwtScriptOnly
public class AlcinaBeanSerializerS2 extends AlcinaBeanSerializer {
	@Override
	public <T> T deserialize(String jsonString) {
		return null;
	}
	@Override
	public String serialize(Object bean) {
		return null;
	}
}