package cc.alcina.framework.servlet.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import cc.alcina.framework.common.client.actions.SelfPerformer;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;

/**
 * A base task which is its own performer. It (and all subclasses) will be
 * annoted with @Bean(PropertySource.FIELDS), and subclass Beans 1.0 properties
 * migrated to fields
 *
 *
 *
 */
public abstract class PerformerTask implements SelfPerformer {
	@JsonIgnore
	protected transient Logger logger = LoggerFactory.getLogger(getClass());

	@Bean(PropertySource.FIELDS)
	public abstract static class Fields extends PerformerTask {
	}
}
