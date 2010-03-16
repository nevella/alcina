package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Annotation;

@SuppressWarnings("all")
public class ValidatorInfoImpl implements ValidatorInfo {
  private NamedParameter[] parameters;
  public NamedParameter[] parameters(){return parameters;}
  
  private Class validator;
  public Class validator(){return validator;}
  
  
  public Class<? extends Annotation> annotationType() {
    return ValidatorInfo.class;
  }
  
  public ValidatorInfoImpl (NamedParameter[] parameters, Class validator){
    this.parameters = parameters;
    this.validator = validator;
  }
}
