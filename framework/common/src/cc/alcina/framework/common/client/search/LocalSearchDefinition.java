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
package cc.alcina.framework.common.client.search;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.util.ComparableComparator;

/**
 *
 * @author Nick Reddel
 */
public abstract class LocalSearchDefinition extends SearchDefinition {
	private Class resultClass;

	protected Predicate buildFilter() {
		return null;
	}

	public Class getResultClass() {
		return resultClass;
	}

	public Collection search() {
		return (Collection) Domain.stream(getResultClass())
				.filter(buildFilter()).sorted(new ComparableComparator())
				.collect(Collectors.toList());
	}

	public void setResultClass(Class resultClass) {
		this.resultClass = resultClass;
	}
}
