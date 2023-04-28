package cc.alcina.framework.servlet.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import cc.alcina.framework.common.client.actions.SelfPerformer;

/**
 * A base task which is its own performer
 * 
 * @author nick@alcina.cc
 *
 */
public abstract class PerformerTask implements SelfPerformer {
	@JsonIgnore
	protected transient Logger logger = LoggerFactory.getLogger(getClass());
}
