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
package cc.alcina.framework.common.client.logic.permissions;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.Permission;

@Retention(RetentionPolicy.RUNTIME)
@Documented
/**
 *
 * @author Nick Reddel
 */
public @interface WebMethod {
	boolean allowExpiredAnonymousAuthenticationSession() default false;

	String comment() default "";

	Permission customPermission() default @Permission(
		access = AccessLevel.LOGGED_IN);

	boolean readonlyPermitted() default false;

	String rpcHandlerThreadNameSuffix() default "";
}
