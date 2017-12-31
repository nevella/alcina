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

import cc.alcina.framework.common.client.actions.RemoteParameters;
import cc.alcina.framework.common.client.csobjects.SearchResult;
import cc.alcina.framework.common.client.search.SearchCriterion.Direction;

/**
 *
 * @author Nick Reddel
 */
public abstract class SingleTableSearchDefinition<R extends SearchResult>
		extends SearchDefinition implements RemoteParameters {
	private Direction orderDirection;

	private String orderPropertyName;

	private transient Class<? extends R> resultClass;

	public SingleTableSearchDefinition() {
		init();
	}

	// TODO: 3.2, check no indjection attack
	public void checkFromClient() {
	}

	public Direction getOrderDirection() {
		return orderDirection;
	}

	public String getOrderPropertyName() {
		return orderPropertyName;
	}

	public Class<? extends R> getResultClass() {
		return resultClass;
	}

	public boolean isOrderable() {
		return true;
	}

	public void setOrderDirection(Direction orderDirection) {
		this.orderDirection = orderDirection;
	}

	public void setOrderPropertyName(String orderPropertyName) {
		this.orderPropertyName = orderPropertyName;
	}

	public void setResultClass(Class<? extends R> resultClass) {
		this.resultClass = resultClass;
	}

	protected abstract void init();

	@Override
	protected String orderEql() {
		if (orderPropertyName == null) {
			return "";
		}
		return " ORDER BY t." + propertyAlias(orderPropertyName) + " "
				+ (orderDirection == Direction.DESCENDING ? "DESC" : "ASC")
				+ " ";
	}
}
