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
package cc.alcina.framework.entity.persistence;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import cc.alcina.framework.common.client.csobjects.SearchResultsBase;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.search.EqlWithParameters;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.search.SingleTableSearchDefinition;


@Registration({ Searcher.class, SearchDefinition.class })
public class BasicSearcher implements Searcher {
	private EntityManager entityManager;

	public EntityManager getEntityManager() {
		return this.entityManager;
	}

	@Override
	public SearchResultsBase search(SearchDefinition def,
			EntityManager entityManager) {
		return searchWithTemp(def, entityManager, null);
	}

	private Query searchStub(SingleTableSearchDefinition sdef, String prefix,
			String postfix, boolean withOrderClause) {
		EqlWithParameters ewp = getEqlWithParameters(sdef, withOrderClause);
		Query query = getEntityManager()
				.createQuery(prefix + " " + ewp.eql + postfix);
		int i = 1;
		for (Object o : ewp.parameters) {
			query.setParameter(i++, o);
		}
		return query;
	}

	protected EqlWithParameters getEqlWithParameters(
			SingleTableSearchDefinition sdef, boolean withOrderClause) {
		EqlWithParameters ewp = sdef.eql(withOrderClause);
		return ewp;
	}

	/*
	 * Like this because of...gwt serialization. See SearchResultsBase usage of
	 * searchresult (which the db type may not implement - only required the
	 * result does)
	 * 
	 * hmmm...now got rid of searchresultsbase array (using garraylist instead)
	 * - but leaving this code as is for now
	 */
	protected SearchResultsBase searchWithTemp(SearchDefinition def,
			EntityManager entityManager, List tempObjects) {
		this.entityManager = entityManager;
		SearchResultsBase result = new SearchResultsBase();
		SingleTableSearchDefinition sdef = (SingleTableSearchDefinition) def;
		Long resultCount = 0L;
		List resultList = searchStub(sdef, sdef.resultEqlPrefix(), "", true)
				.setMaxResults(def.getResultsPerPage())
				.setFirstResult(def.getPageNumber() * def.getResultsPerPage())
				.getResultList();
		if (def.getResultsPerPage() < SearchDefinition.LARGE_SEARCH) {
			Query idQuery = searchStub(sdef, sdef.idEqlPrefix(), "", false);
			resultCount = (Long) idQuery.getSingleResult();
		} else {
			resultCount = Long.valueOf(resultList.size());
		}
		result.setTotalResultCount(
				resultCount == null ? 0 : resultCount.intValue());
		result.setPageNumber(def.getPageNumber());
		result.setPageResultCount(resultList.size());
		result.setSearchDefinition(def);
		if (tempObjects != null) {
			tempObjects.addAll(resultList);
		} else {
			result.setResults(resultList);
		}
		return result;
	}
}
