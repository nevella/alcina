/*
 * Method.java
 *
 * Created on July 21, 2007, 4:36 PM
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package com.totsp.gwittir.rebind.beans;

import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JType;

/**
 * 
 * @author <a href="mailto:cooper@screaming-penguin.com">Robert "kebernet"
 *         Cooper</a> changed by nick cos conditional hashing always dodgy (and
 *         only used in IntrospectoGenerator) - using an external lookup instead
 *         for uniqueness
 */
public class MethodWrapper {
	private JType declaringType;

	private JMethod baseMethod;

	/** Creates a new instance of Method */
	public MethodWrapper(JType declaringType, JMethod baseMethod) {
		if (declaringType == null) {
			throw new NullPointerException("declaringType cannot be null.");
		}
		if (baseMethod == null) {
			throw new NullPointerException("baseMethod cannot be null.");
		}
		this.declaringType = declaringType;
		this.baseMethod = baseMethod;
	}

	public JMethod getBaseMethod() {
		return baseMethod;
	}

	public JType getDeclaringType() {
		return declaringType;
	}
}
