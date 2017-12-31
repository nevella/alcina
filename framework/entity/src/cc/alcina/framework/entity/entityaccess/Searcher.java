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
package cc.alcina.framework.entity.entityaccess;

import javax.persistence.EntityManager;

import cc.alcina.framework.common.client.csobjects.SearchResultsBase;
import cc.alcina.framework.common.client.search.SearchDefinition;

/**
 * 
 * @author Nick Reddel
 */
public interface Searcher<T extends SearchDefinition> {
	public static final String CONTEXT_RESULTS_ARE_DETACHED = Searcher.class
			.getName() + ".CONTEXT_RESULTS_ARE_DETACHED";

	public SearchResultsBase search(T def, int pageNumber, EntityManager em);
}
