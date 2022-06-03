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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.reflection.Reflections;

@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Target({ ElementType.TYPE })
@ClientVisible
/**
 *
 * @author Nick Reddel
 */
public @interface ObjectPermissions {
	Permission create() default @Permission(access = AccessLevel.ROOT);

	Permission delete() default @Permission(access = AccessLevel.ROOT);

	Permission read() default @Permission(access = AccessLevel.ADMIN_OR_OWNER);

	Permission write() default @Permission(access = AccessLevel.ADMIN_OR_OWNER);

	public enum Action {
		create, delete, read, write;

		public Permission asPermission(Object withPermissions) {
			Class<?> clazz = withPermissions instanceof Class
					? (Class) withPermissions
					: withPermissions.getClass();
			ObjectPermissions objectPermissions = Reflections.at(clazz)
					.annotation(ObjectPermissions.class);
			if (objectPermissions == null) {
				objectPermissions = PermissionsManager.get()
						.getDefaultObjectPermissions();
			}
			switch (this) {
			case create:
				return objectPermissions.create();
			case delete:
				return objectPermissions.delete();
			case read:
				return objectPermissions.read();
			case write:
				return objectPermissions.write();
			default:
				throw new UnsupportedOperationException();
			}
		}
	}
}
