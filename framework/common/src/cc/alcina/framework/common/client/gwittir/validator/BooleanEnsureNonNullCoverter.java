package cc.alcina.framework.common.client.gwittir.validator;

import java.util.Date;

import com.totsp.gwittir.client.beans.Converter;

public class BooleanEnsureNonNullCoverter
		implements Converter<Boolean, Boolean> {
	public static final BooleanEnsureNonNullCoverter INSTANCE = new BooleanEnsureNonNullCoverter();

	public Boolean convert(Boolean original) {
		return original == null ? Boolean.FALSE : original;
	}

	public String convert(Date original) {
		return original == null ? null : String.valueOf(original.getTime());
	}
}
