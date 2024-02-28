package cc.alcina.framework.gwt.client.dirndl.cmp;

import java.util.LinkedHashSet;
import java.util.Set;

import com.google.gwt.user.client.ui.SuggestOracle;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.place.BasePlace;

@Bean(PropertySource.FIELDS)
public class AppSuggestorRequest extends SuggestOracle.Request {
	public Set<Class<? extends CommandContext>> commandContexts = new LinkedHashSet<>();

	public BasePlace contextPlace;

	public void populateContext() {
		contextPlace = Client.currentPlace();
	}

	@Override
	public String toString() {
		return getQuery();
	}
}
