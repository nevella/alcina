package cc.alcina.framework.common.client.gwittir.validator;

import java.io.Serializable;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

// typed server validation result to avoid two round trips
//
@Bean(PropertySource.FIELDS)
public class ServerValidationResult implements Serializable {
	public boolean ok;

	public String input;

	public String message;

	public TypedResult contentModel;

	public static class TypedResult extends Model {
	}
}
