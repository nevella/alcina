package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Annotation;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;

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
