/*
 * ToStringRenderer.java
 *
 * Created on April 12, 2007, 12:57 PM
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package cc.alcina.framework.gwt.client.gwittir.renderer;

import com.totsp.gwittir.client.ui.Renderer;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;

/**
 *
 * @author <a href="mailto:cooper@screaming-penguin.com">Robert "kebernet"
 *         Cooper</a>
 */
@Reflected
public class InstantiableToStringRenderer<T> implements Renderer<T, String> {
	/** Creates a new instance of ToStringRenderer */
	public InstantiableToStringRenderer() {
		super();
	}

	public String render(Object o) {
		return (o == null) ? "" : o.toString();
	}
}
