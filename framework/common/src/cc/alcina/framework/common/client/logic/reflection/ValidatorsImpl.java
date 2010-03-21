package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Annotation;
import cc.alcina.framework.common.client.logic.reflection.Validators;

@SuppressWarnings("all")
public class ValidatorsImpl implements Validators {
  private ValidatorInfo[] validators;
  public ValidatorInfo[] validators(){return validators;}
  
  private Class provider;
  public Class provider(){return provider;}
  
  
  public Class<? extends Annotation> annotationType() {
    return Validators.class;
  }
  
  public ValidatorsImpl (ValidatorInfo[] validators, Class provider){
    this.validators = validators;
    this.provider = provider;
  }
}
