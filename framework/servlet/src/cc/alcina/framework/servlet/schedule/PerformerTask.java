package cc.alcina.framework.servlet.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import cc.alcina.framework.common.client.actions.SelfPerformer;

public abstract class PerformerTask implements SelfPerformer {
	@JsonIgnore
	protected transient Logger logger = LoggerFactory.getLogger(getClass());
}
