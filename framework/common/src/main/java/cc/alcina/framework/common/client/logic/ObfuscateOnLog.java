package cc.alcina.framework.common.client.logic;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * <p>
 * Remove marked field or class from the request when logging RPC requests
 * </p>
 * <p>
 * Setting this on a RPC method allows obfuscation of individual parameters
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ObfuscateOnLog {
	/**
	 * <p>
	 * Parameter indicies to remove from RPC request logs
	 * </p>
	 * <p>
	 * Set as a list of indexes of arguments to remove
	 * </p>
	 */
	public int[] parameterIndiciesToRemove() default {};
}
