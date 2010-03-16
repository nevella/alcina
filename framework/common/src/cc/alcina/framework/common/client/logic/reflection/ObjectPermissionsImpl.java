package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Annotation;

@SuppressWarnings("all")
public class ObjectPermissionsImpl implements ObjectPermissions {
  private Permission create;
  public Permission create(){return create;}
  
  private Permission delete;
  public Permission delete(){return delete;}
  
  private Permission read;
  public Permission read(){return read;}
  
  private Permission write;
  public Permission write(){return write;}
  
  
  public Class<? extends Annotation> annotationType() {
    return ObjectPermissions.class;
  }
  
  public ObjectPermissionsImpl (Permission create, Permission delete, Permission read, Permission write){
    this.create = create;
    this.delete = delete;
    this.read = read;
    this.write = write;
  }
}
