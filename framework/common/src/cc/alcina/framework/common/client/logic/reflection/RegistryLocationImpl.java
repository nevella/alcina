package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Annotation;

@SuppressWarnings("all")
public class RegistryLocationImpl implements RegistryLocation {
  private boolean j2seOnly;
  public boolean j2seOnly(){return j2seOnly;}
  
  private Class registryPoint;
  public Class registryPoint(){return registryPoint;}
  
  private Class targetObject;
  public Class targetObject(){return targetObject;}
  
  
  public Class<? extends Annotation> annotationType() {
    return RegistryLocation.class;
  }
  
  public RegistryLocationImpl (boolean j2seOnly, Class registryPoint, Class targetObject){
    this.j2seOnly = j2seOnly;
    this.registryPoint = registryPoint;
    this.targetObject = targetObject;
  }
}
