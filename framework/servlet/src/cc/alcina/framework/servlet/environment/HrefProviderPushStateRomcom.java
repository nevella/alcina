package cc.alcina.framework.servlet.environment;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.gwt.client.place.BasePlace;

@Registration(
	value = BasePlace.HrefProvider.class,
	priority = Registration.Priority.REMOVE)
public class HrefProviderPushStateRomcom extends BasePlace.HrefProvider {
	@Override
	public String toHrefString(BasePlace basePlace) {
		return "/" + BasePlace.tokenFor(basePlace);
	}
}