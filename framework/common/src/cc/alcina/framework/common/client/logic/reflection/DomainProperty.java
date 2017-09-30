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

@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Target({ ElementType.METHOD })
@ClientVisible
/**
 * Specifies runtime creation type for child properties
 */
public @interface DomainProperty {
	boolean eagerCreation() default false;

	boolean cloneForProvisionalEditing() default false;

	boolean cloneForDuplication() default false;

	boolean registerChildren() default false;

	/*
	 * This instructs TransformManager.objectsToDtes() to serialize the
	 * collection. It could probably be removed by doing some sort of loop
	 * checking in objectsToDtes() - or having a per-call policy. But works ok,
	 * if a bit layer-separation-gunky
	 */
	boolean serializeOnClient() default false;

	boolean silentFailOnIllegalWrites() default false;
	
	boolean ignoreForDeletionChecking() default false;
	
	boolean cascadeDeletionFromRef() default false;
	
	boolean serializeWithBeanSerialization() default false;
}
