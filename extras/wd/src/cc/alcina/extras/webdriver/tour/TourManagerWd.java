package cc.alcina.extras.webdriver.tour;

import java.beans.PropertyDescriptor;
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
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.gwt.client.tour.Tour;
import cc.alcina.framework.gwt.client.tour.Tour.ConditionEvaluationContext;
import cc.alcina.framework.gwt.client.tour.TourImpl;
import cc.alcina.framework.gwt.client.tour.TourManager;
import cc.alcina.framework.gwt.client.tour.TourState;

public class TourManagerWd extends TourManager {
	public static final String PROP_FIRST_SUITE_STEP_PERFORMED = TourManagerWd.class
			.getName() + ".PROP_FIRST_STEP_PERFORMED";

	public static Tour deseralize(String json) {
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

	private WDToken token;

	public TourManagerWd(WdExec exec, WDToken token) {
		UIRendererWd.get().exec = exec;
		this.token = token;
	}

	@Override
	protected ConditionEvaluationContext createConditionEvaluationContext() {
		return new ConditionEvaluationContextWd(currentTour);
	}

	@Override
	protected void onNext() {
		token.getTestInfo().put(PROP_FIRST_SUITE_STEP_PERFORMED,
				Boolean.toString(true));
	}

	@Override
	protected boolean shouldRetry(DisplayStepPhase state) {
		return false;
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
				return ResourceUtilities
						.read(string.substring("file:/".length()));
			} else {
				return string;
			}
		}
	}

	class ConditionEvaluationContextWd extends ConditionEvaluationContext {
		public ConditionEvaluationContextWd(TourState tourState) {
			super(tourState);
		}

		@Override
		public boolean provideIsFirstStep() {
			boolean result = super.provideIsFirstStep() && !Boolean.valueOf(
					token.getTestInfo().get(PROP_FIRST_SUITE_STEP_PERFORMED));
			return result;
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
