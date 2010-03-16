package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Annotation;

import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;

@SuppressWarnings("all")
public class PermissionImpl implements Permission {
  private AccessLevel access;
  public AccessLevel access(){return access;}
  
  private String rule;
  public String rule(){return rule;}
  
  
  public Class<? extends Annotation> annotationType() {
    return Permission.class;
  }
  
  public PermissionImpl (AccessLevel access, String rule){
    this.access = access;
    this.rule = rule;
  }
}
