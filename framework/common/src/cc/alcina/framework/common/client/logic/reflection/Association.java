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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@ClientVisible
/**
 * Provides information about both ends of an association. If an association is
 * reflected in the UI (e.g. in a dataobject tree), this annotation is required
 * (for listener creation)
 * 
 * @author Nick Reddel
 */
public @interface Association {
	boolean cascadeDeletes() default false;

	boolean dereferenceOnDelete() default false;

	/**
	 * The type name of the implementation class (component type for a
	 * collection, implementation type for an interface) of <i>this end</i> of
	 * this association - e.g. for:<br>
	 * <blockquote> <code>Bookstore Book.getBookstore() and<br>
	 * Set&lt;Book&gt; Bookstore.getBooks()
	 * </code> </blockquote> the <code>getBookstore()</code> method would have
	 * association with implementationClass "Bookstore" and the
	 * <code>getBookss()</code> method would have association with
	 * implementationClass "Book"
	 */
	Class implementationClass() default void.class;

	/**
	 * The property name of the <i>other end</i> of this association - e.g. for:
	 * <br>
	 * <blockquote> <code>Bookstore Book.getBookstore() and<br>
	 * Set&lt;Book&gt; Bookstore.getBooks()
	 * </code> </blockquote> the getBookstore method would have association with
	 * propertyName "books"
	 */
	String propertyName() default "";
}
