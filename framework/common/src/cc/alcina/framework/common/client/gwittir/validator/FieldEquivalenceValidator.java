package cc.alcina.framework.common.client.gwittir.validator;

import javax.xml.validation.Validator;

import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;
import com.totsp.gwittir.client.validator.ValidationException;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.gwt.client.gwittir.RequiresContextBindable;

@ClientInstantiable
public class FieldEquivalenceValidator
		implements ParameterisedValidator, RequiresContextBindable {
	public static final String FIELD_NAME = "FieldEquivalenceValidator.FIELD_NAME";

	private String otherFieldName;

	private SourcesPropertyChangeEvents bindable;
	@Override
	public Object validate(Object value) throws ValidationException {
		if(value==null){
			return value;
		}
		Object otherValue = Reflections.propertyAccessor().getPropertyValue(bindable, otherFieldName);
		if(otherValue==null){
			return value;
		}
		if(!value.equals(otherValue)){
			throw new ValidationException("Values not equal",FieldEquivalenceValidator.class);
		}
		return value;
	}

	@Override
	public void setParameters(NamedParameter[] params) {
		otherFieldName=NamedParameter.Support.stringValue(params, FIELD_NAME, "");
	}

	@Override
	public void setBindable(SourcesPropertyChangeEvents bindable) {
		this.bindable = bindable;
	}
}
