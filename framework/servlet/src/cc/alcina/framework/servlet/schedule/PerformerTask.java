package cc.alcina.framework.servlet.schedule;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.actions.SelfPerformer;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;

/**
 * <p>
 * A base task which is its own performer. It (and all subclasses) will be
 * annoted with @Bean(PropertySource.FIELDS), and subclass Beans 1.0 properties
 * migrated to fields
 *
 * <p>
 * The job system will clear all transient fields of the task class not marked
 * with {@link RetainTransient} (*but not its supertype transient fields, if
 * any*) on execution completion ({@link #onAfterEnd()})
 *
 *
 */
public abstract class PerformerTask implements SelfPerformer {
	@JsonIgnore
	protected transient Logger logger = LoggerFactory.getLogger(getClass());

	@Bean(PropertySource.FIELDS)
	public abstract static class Fields extends PerformerTask {
	}

	/**
	 * 
	 * Retain this particular transient field
	 *
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.FIELD)
	@Reflected
	public @interface RetainTransient {
	}

	@Override
	public void onAfterEnd() {
		SelfPerformer.super.onAfterEnd();
		Arrays.stream(getClass().getDeclaredFields())
				.filter(f -> Modifier.isTransient(f.getModifiers()))
				.filter(f -> f.getAnnotation(RetainTransient.class) == null)
				.forEach(f -> {
					try {
						f.setAccessible(true);
						f.set(this, null);
					} catch (Exception e) {
						throw WrappedRuntimeException.wrap(e);
					}
				});
	}
}
