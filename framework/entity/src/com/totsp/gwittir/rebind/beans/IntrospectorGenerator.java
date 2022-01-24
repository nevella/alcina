/*
 * IntrospectorGenerator.java
 *
 * Created on July 15, 2007, 2:21 PM
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

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

/**
 * (Nick) Pretty much a complete rewrite of Robert's generator - because we
 * don't care about GWT compiler optimisations (we do our own multi-pass
 * compilation), we can use native JSNI methods for much better obfuscated size
 * and improved performance
 *
 * @author <a href="mailto:cooper@screaming-penguin.com">Robert "kebernet"
 *         Cooper</a>
 *
 */
public class IntrospectorGenerator extends Generator {
	/** Creates a new instance of IntrospectorGenerator */
	public IntrospectorGenerator() {
	}

	@Override
	public String generate(TreeLogger logger, GeneratorContext context,
			String typeName) throws UnableToCompleteException {
		throw new UnsupportedOperationException();
	}
}
