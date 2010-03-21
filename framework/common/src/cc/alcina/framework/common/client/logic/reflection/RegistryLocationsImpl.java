package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Annotation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocations;

@SuppressWarnings("all")
public class RegistryLocationsImpl implements RegistryLocations {
  private RegistryLocation[] value;
  public RegistryLocation[] value(){return value;}
  
  
  public Class<? extends Annotation> annotationType() {
    return RegistryLocations.class;
  }
  
  public RegistryLocationsImpl (RegistryLocation[] value){
    this.value = value;
  }
}
