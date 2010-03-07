package cc.alcina.framework.common.client.logic.reflection;

import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import java.lang.annotation.Annotation;

@SuppressWarnings("all")
public class PropertyPermissionsImpl implements PropertyPermissions {
  private Permission read;
  public Permission read(){return read;}
  
  private Permission write;
  public Permission write(){return write;}
  
  
  public Class<? extends Annotation> annotationType() {
    return PropertyPermissions.class;
  }
  
  public PropertyPermissionsImpl (Permission read, Permission write){
    this.read = read;
    this.write = write;
  }
}
