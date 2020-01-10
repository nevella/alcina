package cc.alcina.framework.gwt.client.module.support.login.pub;

import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.BasePlaceTokenizer;

public class LoginPlace extends BasePlace {
	public LoginPlace() {
	}

	public static class LoginPlaceTokenizer
			extends BasePlaceTokenizer<LoginPlace> {
		@Override
		public Class<LoginPlace> getTokenizedClass() {
			return LoginPlace.class;
		}

		@Override
		protected LoginPlace getPlace0(String token) {
			LoginPlace place = new LoginPlace();
			return place;
		}

		@Override
		protected void getToken0(LoginPlace place) {
			// no parameters
		}
	}
}
