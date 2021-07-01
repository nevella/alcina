package cc.alcina.extras.webdriver.tour;

import java.beans.PropertyDescriptor;
import java.io.IOException;

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

import cc.alcina.extras.webdriver.WdExec;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.gwt.client.tour.Tour;
import cc.alcina.framework.gwt.client.tour.TourImpl;
import cc.alcina.framework.gwt.client.tour.TourManager;

public class TourManagerWd extends TourManager {
	public static Tour deseralize(String json) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.addMixIn(Tour.RelativeTo.class, RelativeToMixin.class);
			mapper.addMixIn(Tour.Step.class, StepMixin.class);
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

	public TourManagerWd(WdExec exec) {
		UIRendererWd.get().exec = exec;
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
			String currentName = parser.getCurrentName();
			Object currentValue = parser.getCurrentValue();
			PropertyDescriptor descriptor = SEUtilities
					.getPropertyDescriptorByName(currentValue.getClass(),
							currentName);
			return CommonUtils.getEnumValueOrNull(
					(Class<Enum>) descriptor.getPropertyType(), string, true,
					null);
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

	@JsonSerialize(using = EnumSerializer.class)
	@JsonDeserialize(using = EnumDeserializer.class)
	static abstract class EnumMixin {
	}

	@JsonDeserialize(as = TourImpl.RelativeToImpl.class)
	static abstract class RelativeToMixin {
	}

	@JsonDeserialize(as = TourImpl.StepImpl.class)
	static abstract class StepMixin {
	}
}
