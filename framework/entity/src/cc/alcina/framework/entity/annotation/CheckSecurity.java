package cc.alcina.framework.entity.annotation;

public @interface CheckSecurity {
	public boolean checked() default false;
	public String checkedBy() default "";
	public String reviewedBy() default "";
}
