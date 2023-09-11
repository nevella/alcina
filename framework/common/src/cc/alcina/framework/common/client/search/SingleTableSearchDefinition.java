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

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.actions.RemoteParameters;
import cc.alcina.framework.common.client.csobjects.SearchResult;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.search.SearchCriterion.Direction;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;

/**
 *
 * @author Nick Reddel
 */
@TypeSerialization(flatSerializable = false)
public abstract class SingleTableSearchDefinition<R extends SearchResult>
		extends SearchDefinition implements RemoteParameters {
	private Direction orderDirection;

	private String orderPropertyName;

	public SingleTableSearchDefinition() {
		init();
	}

	// TODO: 3.2, check no indjection attack
	public void checkFromClient() {
	}

	public abstract Class<?> eqlEntityClass();

	public Direction getOrderDirection() {
		return orderDirection;
	}

	public String getOrderPropertyName() {
		return orderPropertyName;
	}

	public abstract Class<? extends R> getResultClass();

	public boolean isOrderable() {
		return true;
	}

	public void setOrderDirection(Direction orderDirection) {
		this.orderDirection = orderDirection;
	}

	public void setOrderPropertyName(String orderPropertyName) {
		this.orderPropertyName = orderPropertyName;
	}

	protected abstract void init();

	@Override
	protected String orderEql() {
		if (orderPropertyName == null) {
			return "";
		}
		FormatBuilder format = new FormatBuilder();
		format.append(" ORDER BY ");
		if (provideIsEnumOrderProperty()) {
			FormatBuilder orderCase = new FormatBuilder();
			orderCase.append("(CASE ");
			orderCase.format("t.%s ", propertyAlias(orderPropertyName));
			Property property = Reflections.at(eqlEntityClass())
					.property(orderPropertyName);
			List<Enum> sorted = (List) List
					.of(property.getType().getEnumConstants()).stream()
					.sorted(Comparator
							.comparing(e -> e.toString().toUpperCase()))
					.collect(Collectors.toList());
			sorted.forEach(e -> {
				orderCase.format(" WHEN %s THEN %s ", e.ordinal(),
						sorted.indexOf(e));
			});
			orderCase.append(" ELSE NULL END");
			orderCase.append(")");
			String orderCaseStr = orderCase.toString();
			format.append(orderCaseStr);
		} else {
			if (provideIsStringOrderProperty()) {
				format.append("UPPER(");
			}
			format.format("t.%s", propertyAlias(orderPropertyName));
			if (provideIsStringOrderProperty()) {
				format.append(")");
			}
		}
		format.append(" ");
		if (orderDirection == Direction.DESCENDING) {
			format.append("DESC NULLS LAST");
		} else {
			format.append("ASC NULLS FIRST");
		}
		format.append(" ");
		return format.toString();
	}

	protected boolean provideIsEnumOrderProperty() {
		if (orderPropertyName == null) {
			return false;
		}
		Property property = provideProperty();
		return CommonUtils.isEnumOrEnumSubclass(property.getType());
	}

	protected boolean provideIsStringOrderProperty() {
		if (orderPropertyName == null) {
			return false;
		}
		Property property = provideProperty();
		return property.getType() == String.class;
	}

	Property provideProperty() {
		return Reflections.at(eqlEntityClass())
				.property(orderPropertyName.replaceFirst(".+\\.(.+)", "$1"));
	}
}
