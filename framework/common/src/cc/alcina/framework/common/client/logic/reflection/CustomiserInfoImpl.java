package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Annotation;

@SuppressWarnings("all")
public class CustomiserInfoImpl implements CustomiserInfo {
  private NamedParameter[] parameters;
  public NamedParameter[] parameters(){return parameters;}
  
  private Class customiserClass;
  public Class customiserClass(){return customiserClass;}
  
  
  public Class<? extends Annotation> annotationType() {
    return CustomiserInfo.class;
  }
  
  public CustomiserInfoImpl (NamedParameter[] parameters, Class customiserClass){
    this.parameters = parameters;
    this.customiserClass = customiserClass;
  }
}
