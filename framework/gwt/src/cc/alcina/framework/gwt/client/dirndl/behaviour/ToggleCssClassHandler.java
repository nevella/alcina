package cc.alcina.framework.gwt.client.dirndl.behaviour;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;

import cc.alcina.framework.common.client.logic.reflection.ClientVisible;

public class ToggleCssClassHandler extends NodeEventHandler {
	@Override
	public void onEvent(NodeEvent.Context eventContext) {
		Arrays.stream(eventContext.annotation(ToggleCssClassHandlerArgs.class)
				.value())
				.forEach(cssClass -> eventContext.resolveHandlerTarget()
						.getElement().toggleClassName(cssClass));
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD })
	public @interface ToggleCssClassHandlerArgs {
		String[] value();
	}
}
