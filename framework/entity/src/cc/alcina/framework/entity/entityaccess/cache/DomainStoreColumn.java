package cc.alcina.framework.entity.entityaccess.cache;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Target({ ElementType.METHOD })
/**
 * Retrieve from db - workaround for
 * 
 * @author nick@alcina.cc
 *
 */
public @interface DomainStoreColumn {
	// Method descriptor #18 ()Ljava/lang/String;
	public abstract java.lang.String mappedBy();

	public abstract java.lang.Class targetEntity();
}
