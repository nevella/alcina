package cc.alcina.template.search;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import cc.alcina.framework.common.client.csobjects.SearchResultsBase;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.template.AlcinaTemplate;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.entity.entityaccess.BasicSearcher;
import cc.alcina.framework.entity.entityaccess.Searcher;
import cc.alcina.template.cs.misc.search.DomainTransformEventInfo;
import cc.alcina.template.cs.misc.search.DomainTransformSearchDefinition;
import cc.alcina.template.j2seentities.DomainTransformEventPersistentImpl;

@AlcinaTemplate
@RegistryLocation(registryPoint = Searcher.class, targetClass = DomainTransformSearchDefinition.class)
@SuppressWarnings("unchecked")
public class DomainTransformSearcher extends BasicSearcher {
	@Override
	public SearchResultsBase search(SearchDefinition def, int pageNumber,
			EntityManager entityManager) {
		List<DomainTransformEventPersistentImpl> rawResults = new ArrayList<DomainTransformEventPersistentImpl>();
		SearchResultsBase resultsBase = super.searchWithTemp(def, pageNumber,
				entityManager, rawResults);
		List<DomainTransformEventInfo> processedResults = new ArrayList<DomainTransformEventInfo>();
		for (DomainTransformEventPersistentImpl ji : rawResults) {
			processedResults.add(ji.unwrap());
		}
		resultsBase.setResults(processedResults);
		return resultsBase;
	}
}
