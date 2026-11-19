package cc.alcina.framework.gwt.client.dirndl.model.edit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;

@ClientVisible
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ ElementType.METHOD, ElementType.FIELD })
public @interface FocusOnBindMarker {
}