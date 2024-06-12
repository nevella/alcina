package cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;

@Bean(PropertySource.FIELDS)
public class AppSuggestionEntry<C extends AppSuggestionCategory>
		implements AppSuggestion {
	public String url;

	public String match;

	public String secondary;

	public Class<? extends ModelEvent> modelEvent;

	@ReflectiveSerializer.Checks(hasReflectedSubtypes = true)
	public C category;

	@Override
	public Object eventData() {
		return eventData;
	}

	/*
	 * This *could* be more complex - it'd be nice to be able to pass any
	 * serializable event data - but a String is a decent start
	 */
	public String eventData;

	@Override
	public String getDisplayString() {
		return null;
	}

	@Override
	public String getReplacementString() {
		return null;
	}

	public String provideFirst() {
		return match;
	}

	public C category() {
		return category;
	}

	@Override
	public String toString() {
		FormatBuilder format = new FormatBuilder().separator(" - ");
		format.append(match);
		format.appendIfNotBlank(secondary);
		format.separator("");
		format.appendIfNotBlankKv("url", url);
		if (modelEvent != null) {
			format.appendIfNotBlankKv("modelEvent", modelEvent.getSimpleName());
		}
		return format.toString();
	}

	@Override
	public Class<? extends ModelEvent> modelEvent() {
		return modelEvent;
	}

	@Override
	public String url() {
		return url;
	}

	@Override
	public String secondary() {
		return secondary;
	}
}
