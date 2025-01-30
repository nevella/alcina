package cc.alcina.framework.servlet.component.entity.property;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.process.TreeProcess;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.traversal.InitialTextSelection;
import cc.alcina.framework.common.client.traversal.PlainTextSelection;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.util.StringMatches;
import cc.alcina.framework.common.client.util.StringMatches.PartialSubstring;
import cc.alcina.framework.servlet.component.entity.property.QueryPartLayer.PartSelection;
import cc.alcina.framework.servlet.component.traversal.StandardLayerAttributes;
import cc.alcina.framework.servlet.job.JobContext;

/**
 * <p>
 * Uses a traversal parser to parse a string such as 'nam "bruce le"', to
 * generate property proposals 'name EQ "bruce le"' and 'name MATCHES "bruce
 * le"'
 * 
 * <p>
 * Parts of this are context/branch dependent (valid property names for the
 * class, valid operators for the property)
 */
public class PropertyFilterParser2 {
	Logger logger = LoggerFactory.getLogger(getClass());

	public List<StandardLayerAttributes.Filter>
			proposeFilters(Class<? extends Entity> entityType, String query) {
		parse(entityType, query);
		traversal.throwExceptions();
		List<PartSelection> parts = traversal
				.getSelections(QueryPartLayer.PartSelection.class);
		logger.info("{} parts", parts.size());
		parts.forEach(i -> logger.info(
				"==========================================\n{}\n",
				i.getBranch().toResult().toStructuredString()));
		return null;
		// List<Filter> list =
		// Registry.query(MatcherPart.class).implementations()
		// .flatMap(
		// part -> part.proposeFilters(entityType, query).stream())
		// .collect(Collectors.toList());
		// return list;
	}

	SelectionTraversal traversal;

	public void initialiseTraversal(Class<? extends Entity> entityType,
			String text) {
		traversal = new SelectionTraversal();
		TreeProcess.Node parentNode = JobContext.getSelectedProcessNode();
		traversal.select(new Query(parentNode, entityType, text));
		RootLayer rootLayer = new RootLayer();
		traversal.setRootLayer(rootLayer);
	}

	void parse(Class<? extends Entity> entityType, String text) {
		initialiseTraversal(entityType, text);
		traversal.traverse();
	}

	static class Query extends InitialTextSelection
			implements PlainTextSelection {
		Class<? extends Entity> entityType;

		public Query(TreeProcess.Node parentNode,
				Class<? extends Entity> entityType, String text) {
			super(parentNode, text);
			this.entityType = entityType;
		}

		List<Property> getMatchingProperties(String text) {
			List<Property> properties = Reflections.at(entityType).properties()
					.stream().sorted(Comparator.comparing(Property::getName))
					.collect(Collectors.toList());
			List<Property> candidates = new StringMatches.PartialSubstring<Property>()
					.match(properties, Property::getName, text).stream()
					.map(PartialSubstring.Match::getValue)
					.collect(Collectors.toList());
			return candidates;
		}
	}
}
