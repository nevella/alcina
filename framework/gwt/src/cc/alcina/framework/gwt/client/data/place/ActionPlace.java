package cc.alcina.framework.gwt.client.data.place;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.BasePlaceTokenizer;

public class ActionPlace extends BasePlace {
	public static String getActionToken(String actionName,
			String... parameters) {
		ActionPlace place = new ActionPlace();
		place.actionName = actionName;
		place.parameters.addAll(Arrays.asList(parameters));
		return new ActionsPlaceTokenizer().getToken(place);
	}

	public String actionName;

	public List<String> parameters = new ArrayList<>();

	public ActionPlace() {
	}

	public ActionPlace(String actionName) {
		this.actionName = actionName;
	}

	public void addParameter(Long longValue) {
		if (longValue == null) {
			parameters.add("0");
		} else {
			parameters.add(longValue.toString());
		}
	}

	public void addParameter(String string) {
		if (string == null) {
			string = "";
		}
		parameters.add(string);
	}

	public long longParameter(int idx) {
		return Long.parseLong(parameters.get(idx));
	}

	public static class ActionsPlaceTokenizer
			extends BasePlaceTokenizer<ActionPlace> {
		@Override
		public Class<ActionPlace> getTokenizedClass() {
			return ActionPlace.class;
		}

		@Override
		protected ActionPlace getPlace0(String token) {
			ActionPlace place = new ActionPlace();
			if (parts.length == 1) {
				return place;
			}
			place.actionName = parts[1];
			for (int i = 2; i < parts.length; i++) {
				place.parameters.add(parts[i]);
			}
			return place;
		}

		@Override
		protected void getToken0(ActionPlace place) {
			addTokenPart(place.actionName);
			for (String parameter : place.parameters) {
				addTokenPart(parameter);
			}
		}
	}

	public void addParameter(Enum e) {
		addParameter(e.toString().toLowerCase());
	}
}
