package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;

/**
 * <p>
 * Marks a property as transient for serialization, by default in all contexts.
 * Respected by the following serializers: AlcinaBeanSerializer,
 * ReflectiveSerializer, FlatTreeSerializer
 *
 * <p>
 * See {@link TransienceContext} for an explanation of serialization contexts.
 *
 * 
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
// where transience is simple, just use the java keyword 'transient' on a field
@Target({ ElementType.METHOD, ElementType.FIELD })
@ClientVisible
public @interface AlcinaTransient {
	TransienceContext[] unless() default {};

	// empty implies 'transient in all contexts' - non empty restricts
	// transience to the selected contexts
	TransienceContext[] value() default {};

	public static class Support {
		private static final String CONTEXT_TRANSIENCE_CONTEXTS = AlcinaTransient.Support.class
				.getName() + ".CONTEXT_TRANSIENCE_CONTEXTS";

		public static void clearTransienceContext() {
			setTransienceContexts((TransienceContext[]) null);
		}

		public static TransienceContext[] getTransienceContexts() {
			TransienceContext[] types = LooseContext
					.get(CONTEXT_TRANSIENCE_CONTEXTS);
			if (types == null) {
				types = new TransienceContext[0];
			}
			return types;
		}

		public static TransienceContext[] getTransienceContextsNoDefault() {
			TransienceContext[] types = LooseContext
					.get(CONTEXT_TRANSIENCE_CONTEXTS);
			return types;
		}

		public static boolean isContextTransient(AlcinaTransient annotation) {
			return isTransient(annotation, getTransienceContexts());
		}

		public static boolean isTransient(AlcinaTransient annotation,
				TransienceContext... types) {
			if (annotation == null) {
				return false;
			}
			List<TransienceContext> ifPresent = Arrays
					.asList(annotation.value());
			if (ifPresent.isEmpty()) {
				List<TransienceContext> unless = Arrays
						.asList(annotation.unless());
				if (unless.isEmpty()) {
					return true;
				} else {
					return types != null
							&& Arrays.stream(types).noneMatch(unless::contains);
				}
			} else {
				Preconditions.checkState(annotation.unless().length == 0);
				return types != null
						&& Arrays.stream(types).anyMatch(ifPresent::contains);
			}
		}

		public static void
				setTransienceContexts(TransienceContext... contexts) {
			if (contexts == null) {
				LooseContext.remove(CONTEXT_TRANSIENCE_CONTEXTS);
			} else {
				LooseContext.set(CONTEXT_TRANSIENCE_CONTEXTS, contexts);
			}
		}
	}

	/*
	 * Control *when* a property is transient. As an example, often properties
	 * should be serialized on the server but not when sending to a client.
	 */
	public enum TransienceContext {
		/*
		 * Default, transient in all contexts
		 */
		ALL,
		/*
		 * Transient when sending to or from client
		 */
		CLIENT,
		/*
		 * Transient when sending from client
		 */
		RPC,
		/*
		 * Transient when persisting a job's task
		 */
		JOB,
		/*
		 * Transient when persisting on server
		 */
		SERVER,
		/*
		 * Transient when serializing an api response
		 */
		API
	}
}
