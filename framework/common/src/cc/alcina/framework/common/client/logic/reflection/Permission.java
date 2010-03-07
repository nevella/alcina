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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;


@Retention(RetentionPolicy.RUNTIME)
@Documented
@ClientVisible
/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public @interface Permission {
	AccessLevel access() default AccessLevel.DEVELOPER;

	String rule() default "";

	public static class SimplePermissions {
		public static Permission getPermission(AccessLevel level) {
			switch (level) {
			case ROOT:
				return new Permission() {
					public AccessLevel access() {
						return AccessLevel.ROOT;
					}

					public String rule() {
						return null;
					}

					public Class<? extends Annotation> annotationType() {
						return Permission.class;
					}
				};
			case EVERYONE:
				return new Permission() {
					public AccessLevel access() {
						return AccessLevel.EVERYONE;
					}

					public String rule() {
						return null;
					}

					public Class<? extends Annotation> annotationType() {
						return Permission.class;
					}
				};
			case ADMIN:
				return new Permission() {
					public AccessLevel access() {
						return AccessLevel.ADMIN;
					}

					public String rule() {
						return null;
					}

					public Class<? extends Annotation> annotationType() {
						return Permission.class;
					}
				};
			case ADMIN_OR_OWNER:
				return new Permission() {
					public AccessLevel access() {
						return AccessLevel.ADMIN_OR_OWNER;
					}

					public String rule() {
						return null;
					}

					public Class<? extends Annotation> annotationType() {
						return Permission.class;
					}
				};
			case LOGGED_IN:
				return new Permission() {
					public AccessLevel access() {
						return AccessLevel.LOGGED_IN;
					}

					public String rule() {
						return null;
					}

					public Class<? extends Annotation> annotationType() {
						return Permission.class;
					}
				};
			default:
				return null;
			}
		}
	}
}
