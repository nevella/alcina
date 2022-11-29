package cc.alcina.extras.webdriver.tour;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import cc.alcina.extras.webdriver.WDToken;
import cc.alcina.extras.webdriver.WdExec;
import cc.alcina.extras.webdriver.tour.UIRendererWd.RenderedPopup;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.gwt.client.tour.Tour;
import cc.alcina.framework.gwt.client.tour.Tour.ConditionEvaluationContext;
import cc.alcina.framework.gwt.client.tour.TourImpl;
import cc.alcina.framework.gwt.client.tour.TourManager;
import cc.alcina.framework.gwt.client.tour.TourState;

/**
 * <h2>Debugging webdriver tours</h2>
 * <p>
 * Tours use the ProcessObserver module to intercept events for debugging. To
 * say, debug a step with text "Russel" in the caption or description fields,
 * add the following:
 * </p>
 * <code><pre>
static class BeforeActionObserver
implements ProcessObserver<TourManager.BeforeActionPerformed> {
&#64;Override
  public Class<BeforeActionPerformed> getObservableClass() {
    return TourManager.BeforeActionPerformed.class;
  }

  public void topicPublished(BeforeActionPerformed observable) {
    if (observable.contains("Russel")) {
	  boolean breakpoint = true;//set a breakpoint on this line
    }
  }
}

-- and register the observer --

ProcessObservers.observe(new BeforeActionObserver());
 *
 * </pre></code>
 *
 *
 *
 * @author nick@alcina.cc
 *
 */
public class TourManagerWd extends TourManager {
	public static final String PROP_FIRST_SUITE_STEP_PERFORMED = TourManagerWd.class
			.getName() + ".PROP_FIRST_STEP_PERFORMED";

	public static final String CONTEXT_HIDE_POPUPS = TourManagerWd.class
			.getName() + ".CONTEXT_HIDE_POPUPS";

	public static Tour deserialize(String json) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.addMixIn(Tour.RelativeTo.class, RelativeToMixin.class);
			mapper.addMixIn(Tour.Step.class, StepMixin.class);
			mapper.addMixIn(Tour.Condition.class, ConditionMixin.class);
			mapper.addMixIn(Tour.PositioningDirection.class, EnumMixin.class);
			mapper.addMixIn(Tour.Action.class, EnumMixin.class);
			mapper.addMixIn(Tour.Operator.class, EnumMixin.class);
			mapper.addMixIn(Tour.Pointer.class, EnumMixin.class);
			TourImpl value = mapper.readValue(json, TourImpl.class);
			return value;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static String readFile(String absolutePath, Class<?> clazz) {
		try {
			return ResourceUtilities.read(absolutePath);
		} catch (Exception e) {
			return ResourceUtilities.readClassPathResourceAsString(clazz,
					"res/" + new File(absolutePath).getName());
		}
	}

	private WDToken token;

	private WdExec exec;

	boolean hidePopups;

	public Topic<RenderedPopup> beforePopup = Topic.create();

	public Topic<RenderedPopup> afterPopup = Topic.create();

	public TourManagerWd(WdExec exec, WDToken token) {
		this.exec = exec;
		UIRendererWd.get().exec = exec;
		UIRendererWd.get().onTourInit();
		// assign 'webdriver' class to body
		this.token = token;
		hidePopups = LooseContext.is(CONTEXT_HIDE_POPUPS);
	}

	@Override
	public ConditionEvaluationContext createConditionEvaluationContext() {
		return new ConditionEvaluationContextWd(currentTour);
	}

	public void registerDebugHandler(Runnable runnable) {
		getElementException.add(runnable);
		beforePopup.add(e -> {
			String regex = "(?s)"
					+ Configuration.get("beforePopupRenderedDebugFilter");
			if (e.toString().matches(regex)) {
				runnable.run();
			}
		});
		afterPopup.add(e -> {
			String regex = "(?s)"
					+ Configuration.get("afterPopupRenderedDebugFilter");
			if (e.toString().matches(regex)) {
				runnable.run();
			}
		});
	}

	@Override
	protected void onNext() {
		token.getProperties().put(PROP_FIRST_SUITE_STEP_PERFORMED,
				Boolean.toString(true));
	}

	@Override
	protected boolean shouldRetry(DisplayStepPhase state) {
		return false;
	}

	public class ConditionEvaluationContextWd
			extends ConditionEvaluationContext {
		public ConditionEvaluationContextWd(TourState tourState) {
			super(tourState);
		}

		public WdExec getExec() {
			return exec;
		}

		@Override
		public boolean provideIsFirstStep() {
			boolean result = super.provideIsFirstStep() && !Boolean.valueOf(
					token.getProperties().get(PROP_FIRST_SUITE_STEP_PERFORMED));
			return result;
		}
	}

	public static class EnumDeserializer extends JsonDeserializer<Enum> {
		@Override
		public Enum deserialize(final JsonParser parser,
				final DeserializationContext context)
				throws IOException, JsonProcessingException {
			String string = parser.getValueAsString();
			if (string == null) {
				return null;
			}
			String currentName = parser.getCurrentName();
			Object currentValue = parser.getCurrentValue();
			PropertyDescriptor descriptor = SEUtilities
					.getPropertyDescriptorByName(currentValue.getClass(),
							currentName);
			Enum value = CommonUtils.getEnumValueOrNull(
					(Class<Enum>) descriptor.getPropertyType(), string, true,
					null);
			Objects.requireNonNull(value,
					Ax.format("value '%s' not found in enum %s", string,
							descriptor.getPropertyType().getSimpleName()));
			return value;
		}
	}

	public static class EnumSerializer extends JsonSerializer<Enum> {
		@Override
		public void serialize(final Enum value, final JsonGenerator gen,
				final SerializerProvider serializer)
				throws IOException, JsonProcessingException {
			gen.writeString(CommonUtils.friendlyConstant(value, "-"));
		}
	}

	public static class PathDeserializer extends JsonDeserializer<String> {
		@Override
		public String deserialize(final JsonParser parser,
				final DeserializationContext context)
				throws IOException, JsonProcessingException {
			String string = parser.getValueAsString();
			if (string == null) {
				return null;
			}
			if (string.startsWith("file://")) {
				String path = string.substring("file:/".length());
				try {
					String contents = ResourceUtilities.read(path);
					if (path.endsWith(".md")) {
						contents = Ax.format("%s\n%s", path, contents);
					}
					return contents;
				} catch (Exception e) {
					return "No file at path: " + path;
				}
			} else {
				return string;
			}
		}
	}

	@JsonDeserialize(as = TourImpl.ConditionImpl.class)
	static abstract class ConditionMixin {
	}

	@JsonSerialize(using = EnumSerializer.class)
	@JsonDeserialize(using = EnumDeserializer.class)
	static abstract class EnumMixin {
	}

	@JsonDeserialize(as = TourImpl.RelativeToImpl.class)
	static abstract class RelativeToMixin {
	}

	@JsonDeserialize(as = TourImpl.StepImpl.class)
	static abstract class StepMixin {
		@JsonDeserialize(using = PathDeserializer.class)
		public abstract String getDescription();
	}
}
