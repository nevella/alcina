/*
 * CompositeValidator.java
 *
 * Created on July 16, 2007, 5:05 PM
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
package cc.alcina.framework.common.client.gwittir.validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import com.totsp.gwittir.client.validator.ValidationException;
import com.totsp.gwittir.client.validator.Validator;

/**
 *
 * @author <a href="mailto:cooper@screaming-penguin.com">Robert "kebernet"
 *         Cooper</a>
 * 
 *         Modified - Nick Reddel, added getValidators() for framework support
 */
public class CompositeValidator implements Validator.Bidi {
	private List<Validator> validators = new ArrayList<>();

	/** Creates a new instance of CompositeValidator */
	public CompositeValidator() {
	}

	public CompositeValidator add(Validator v) {
		validators.add(v);
		return this;
	}

	public List<Validator> getValidators() {
		return this.validators;
	}

	/*
	 * This just returns the input value (chaining validator transforms is just
	 * too error-prone)
	 * 
	 * Dirndl - no member can be an async/server validator (if some sort of
	 * composite is required, handle it all server-side)
	 */
	public Object validate(Object value) throws ValidationException {
		for (Iterator it = validators.iterator(); it.hasNext();) {
			Validator validator = (Validator) it.next();
			value = validator.validate(value);
		}
		return value;
	}

	@Override
	public Validator inverseValidator() {
		CompositeValidator inverseValidator = new CompositeValidator();
		List<Validator> reversedValidators = validators.stream()
				.collect(Collectors.toList());
		Collections.reverse(reversedValidators);
		for (Validator validator : reversedValidators) {
			if (validator instanceof Validator.Bidi) {
				inverseValidator
						.add(((Validator.Bidi) validator).inverseValidator());
			} else {
				return null;
			}
		}
		return inverseValidator;
	}
}
