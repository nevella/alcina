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

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import cc.alcina.framework.common.client.csobjects.SearchResultsBase;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.search.EqlWithParameters;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.search.SingleTableSearchDefinition;

@RegistryLocation(registryPoint = Searcher.class, targetClass = SearchDefinition.class)
/**
 *
 * @author Nick Reddel
 */
public class BasicSearcher implements Searcher {
	private EntityManager entityManager;

	public EntityManager getEntityManager() {
		return this.entityManager;
	}

	@SuppressWarnings("unchecked")
	public SearchResultsBase search(SearchDefinition def, int pageNumber,
			EntityManager entityManager) {
		return searchWithTemp(def, pageNumber, entityManager, null);
	}

	/*
	 * Like this because of...gwt serialization. See SearchResultsBase usage of
	 * searchresult (which the db type may not implement - only required the
	 * result does)
	 */
	protected SearchResultsBase searchWithTemp(SearchDefinition def,
			int pageNumber, EntityManager entityManager, List tempObjects) {
		this.entityManager = entityManager;
		SearchResultsBase result = new SearchResultsBase();
		SingleTableSearchDefinition sdef = (SingleTableSearchDefinition) def;
		Long resultCount = 0L;
		List resultList = searchStub(sdef, sdef.resultEqlPrefix(), "", true)
				.setMaxResults(def.getResultsPerPage()).setFirstResult(
						pageNumber * def.getResultsPerPage()).getResultList();
		if (def.getResultsPerPage() < SearchDefinition.LARGE_SEARCH) {
			Query idQuery = searchStub(sdef, sdef.idEqlPrefix(), "", false);
			resultCount = (Long) idQuery.getSingleResult();
		} else {
			resultCount = new Long(resultList.size());
		}
		result.setTotalResultCount(resultCount == null ? 0 : resultCount
				.intValue());
		result.setPageNumber(pageNumber);
		result.setPageResultCount(resultList.size());
		result.setSearchDefinition(def);
		if (tempObjects != null) {
			tempObjects.addAll(resultList);
		} else {
			result.setResults(resultList);
		}
		return result;
	}

	private Query searchStub(SingleTableSearchDefinition sdef, String prefix,
			String postfix, boolean withOrderClause) {
		EqlWithParameters ewp = sdef.eql(withOrderClause);
		Query query = getEntityManager().createQuery(
				prefix + " " + ewp.eql + postfix);
		int i = 1;
		for (Object o : ewp.parameters) {
			query.setParameter(i++, o);
		}
		return query;
	}
}
