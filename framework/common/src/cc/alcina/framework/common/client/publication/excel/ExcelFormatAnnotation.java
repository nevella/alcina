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
package cc.alcina.framework.common.client.publication.excel;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
/**
 *
 * @author Nick Reddel
 */
public @interface ExcelFormatAnnotation {
	public static final int DEFAULT_ORDER_POS = 100;

	String displayName() default "";

	boolean omit() default false;

	int order() default DEFAULT_ORDER_POS;

	String styleId() default "";

	ExcelDatatype type() default ExcelDatatype.String;
}
