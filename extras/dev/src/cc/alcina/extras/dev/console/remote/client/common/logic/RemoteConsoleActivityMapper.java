package cc.alcina.extras.dev.console.remote.client.common.logic;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.place.shared.Place;

import cc.alcina.extras.dev.console.remote.client.module.console.ConsoleActivity;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.BasePlaceTokenizer;

@Reflected
@Registration.Singleton(ActivityMapper.class)
public class RemoteConsoleActivityMapper implements ActivityMapper {
	@Override
	public Activity getActivity(Place place) {
		if (place instanceof ConsolePlace) {
			return new ConsoleActivity((ConsolePlace) place);
		}
		return null;
	}

	public static class ConsolePlace extends BasePlace {
	}

	public static class ConsolePlaceTokenizer
			extends BasePlaceTokenizer<ConsolePlace> {
		@Override
		protected ConsolePlace getPlace0(String token) {
			ConsolePlace place = new ConsolePlace();
			return place;
		}

		@Override
		public String getToken(ConsolePlace place) {
			// just one and very rooty
			return "";
		}

		@Override
		protected void getToken0(ConsolePlace place) {
		}
	}
}
