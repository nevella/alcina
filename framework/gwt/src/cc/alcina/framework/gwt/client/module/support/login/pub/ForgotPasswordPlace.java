package cc.alcina.framework.gwt.client.module.support.login.pub;

import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.BasePlaceTokenizer;

// FIXME - ui2 1x0 - implement
public class ForgotPasswordPlace extends BasePlace {
	public ForgotPasswordPlace() {
	}

	public static class ForgotPasswordPlaceTokenizer
			extends BasePlaceTokenizer<ForgotPasswordPlace> {
		@Override
		protected ForgotPasswordPlace getPlace0(String token) {
			ForgotPasswordPlace place = new ForgotPasswordPlace();
			return place;
		}

		@Override
		protected void getToken0(ForgotPasswordPlace place) {
			// no parameters
		}
	}
}
