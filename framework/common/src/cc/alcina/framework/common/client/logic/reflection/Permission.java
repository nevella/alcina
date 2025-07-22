/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@ClientVisible
@Inherited
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.TYPE })
/**
 *
 * <p>
 * A given user passes a permission test if one of the following is true:
 * </p>
 *
 * <ul>
 * <li>The permission AccessLevel is one of [EVERYONE, LOGGED_IN, ADMIN,
 * DEVELOPER, ROOT] and their AccessLevel (which will be one of those values)
 * has an ordinal &gt;= the permission AccessLevel ordinal
 * <li>The permission AccessLevel == AccessLevel.GROUP and the user is a member
 * of the group with the name of the Permission rule() value *or* an Admin
 * <li>The permission AccessLevel == AccessLevel.ADMIN_OR_OWNER and the user is
 * the object owner *or* an Admin
 * <li>The permission rule() annotation attribute is non-empty, AccessLevel !=
 * AccessLevel.GROUP and the rule evaluator corresponding to the rule()
 * attribute returns true for the given user, object and context. The evaluator
 * can be found by searching for usages of the string value of the rule()
 * attribute
 * </ul>
 *
 * <p>
 * TODO:
 * </p>
 * <ul>
 * <li>Explain the rationale (as opposed to say java.security.Principal et al)
 * of the permissions system
 * <li>Explain assignment permissions
 * <li>Explain the permissions stack, how loginstate/user map to access level
 * <li>Interaction with projection (this also contributes to 'philosophy of
 * Alcina - details' - correct permissions for a large application are actually
 * *really hard* without something like projection
 * </ul>
 *
 * @author Nick Reddel
 */
public @interface Permission {
	AccessLevel access() default AccessLevel.DEVELOPER;

	String rule() default "";

	public static class SimplePermissions {
		public static Permission getPermission(AccessLevel level) {
			switch (level) {
			case ROOT:
				return new Permission() {
					@Override
					public AccessLevel access() {
						return AccessLevel.ROOT;
					}

					@Override
					public Class<? extends Annotation> annotationType() {
						return Permission.class;
					}

					@Override
					public String rule() {
						return null;
					}
				};
			case EVERYONE:
				return new Permission() {
					@Override
					public AccessLevel access() {
						return AccessLevel.EVERYONE;
					}

					@Override
					public Class<? extends Annotation> annotationType() {
						return Permission.class;
					}

					@Override
					public String rule() {
						return null;
					}
				};
			case ADMIN:
				return new Permission() {
					@Override
					public AccessLevel access() {
						return AccessLevel.ADMIN;
					}

					@Override
					public Class<? extends Annotation> annotationType() {
						return Permission.class;
					}

					@Override
					public String rule() {
						return null;
					}
				};
			case ADMIN_OR_OWNER:
				return new Permission() {
					@Override
					public AccessLevel access() {
						return AccessLevel.ADMIN_OR_OWNER;
					}

					@Override
					public Class<? extends Annotation> annotationType() {
						return Permission.class;
					}

					@Override
					public String rule() {
						return null;
					}
				};
			case LOGGED_IN:
				return new Permission() {
					@Override
					public AccessLevel access() {
						return AccessLevel.LOGGED_IN;
					}

					@Override
					public Class<? extends Annotation> annotationType() {
						return Permission.class;
					}

					@Override
					public String rule() {
						return null;
					}
				};
			default:
				return null;
			}
		}
	}
}
