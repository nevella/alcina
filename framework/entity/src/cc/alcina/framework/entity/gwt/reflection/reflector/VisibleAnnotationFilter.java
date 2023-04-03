package cc.alcina.framework.entity.gwt.reflection.reflector;

import java.lang.annotation.Annotation;
import java.util.function.Predicate;

public interface VisibleAnnotationFilter
		extends Predicate<Class<? extends Annotation>> {
}
