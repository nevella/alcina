As I go documentation of getting an Alcina template application up and running.

### Open Questions and Tasks ###

#### General Questions ####
  * Can I update my Alcina project yet?
  * How do I get all of the source compiled into the jars. I continue to have issues with code visibility in the Eclipse debugger.

#### Web Client ####
  * How should I implement custom validation? For example, ensuring that a subject public ID is unique within a study. I have seen how to enforce this globally (e.g., I require that study names are unique).
  * How should I implement custom permissions? For example, how can I limit the studies that are displayed to a particular user to the ones that they are members of? I can manually do this by post filtering the results I get back in my collection node, but I'm sure there is a better way to do this with the existing Alcina permissions model.
  * When do I need the @ClientInstantiable annotation?

#### RCP Client ####
  * For installations that do not use a central server, how can I set up the application so that the object graph is persisted to a local relational database (e.g., Derby) in the normalized form instead of as a list of transforms?
  * How can I configure the RCP app to talk to the same JBoss server as the web client?
  * How do we implement the client login code?

### Build Targets ###
  * For development, should only need the Hot Deploy targets, which use the JBOSS expanded ear/war format. If the target name contains GWT, it recompiles the GWT client code. This is necessary when either you want to view the application in production mode rather than hosted mode or when the RPC interfaces have changed. This may be changed if any of the fields or the classes reachable by the fields in MobilityLabRemoteService change. Note: you will see a "This application is out of date" exception if the RPC interfaces are incompatible.
  * Restart targets cause the JBoss server to be restarted and should be used if the server side code changes.

### Starting and debugging the JBoss Server ###
  * After running the template builder, a JBOSS instance is deployed to the `${project.work.dir}/deploy/jboss/jboss-5.1.0.GA`, where `project.work.dir` is defined in local.ant.properties.
  * To run the JBoss server, run the following code:
`${deploy.jboss.dir}/bin/run.sh -c all -b 0.0.0.0` (`run.bat` for Windows users)
  * To run the JBoss server in debug mode, add:
`-Xdebug -Xrunjdwp:transport=dt_socket,address=8790,server=y,suspend=n`
  * To attach to the debug JBoss, create a new debug target in Eclipse, specify the project as "com.apdm.mobilitylab.entity", the connection type as "Standard (Socket Attach)", the host as "localhost", and the port as "8790".

to the JVM arguments in the `:RESTART` target in `run.(bat/sh)`.
  * For persistence problems, the gwt compiler logs can be useful:
in

`/com.apdm.mobilitylab.entity/local.ant.properties`

set

`project.gwt.compile.flags=-soyc -compileReport`

then look in

`/com.apdm.mobilitylab.servlet/extras/com.apdm.mobilitylab.MobilityLabClient/rpcLog/com.apdm.mobilitylab.cs.remote.MobilityLabRemoteService-xxx.rpc.log...`

### Remote debugging of MobilityLab RCP app ###
add
```
-vmargs
-Xdebug
-Xrunjdwp:transport=dt_socket,address=8003,server=y,suspend=n
```
to MobilityLab.ini
Create a new Remote Java Application debug configuration. Use Standard (Socket Attach) Connection type, specify the host IP address and use port 8003.

### Managing the Hypersonic database ###
This is the database that the JBoss server uses for Java persistence.
  * Navigate to http://localhost:8080/jmx-console/ in a browser.
  * Click on the database=mobilityLabDb,service=Hypersonic link.
  * Click on the "Invoke" button next to the "startDatabaseManager" entry.

### Managing the Derby database ###
This is the database that the RCP app uses for Java persistence.
  * Open the SQuirreL SQL client.
  * Make sure that the database is not locked by the app (only one process can hold open a session at a time).
  * Use the "Apache Derby Embedded" driver.
  * Use a similar URL string:
```
jdbc:derby:path_to_.database_folder/persistedtransforms
```
  * Do not provide a login or password.


### Managing a PostgreSQL database ###
This is an alternate (and more current) database that JBoss uses for Java persistence.
  * To create the database
    * Log in as user postgres to the sql terminal "sudo -u postgres psql"
    * Create the database "CREATE DATABASE mobilitylab WITH OWNER mobilitylab;"
    * Delete the database "DROP DATABASE mobilitylab;"
  * standalone-preview.xml:
    * connection-url: jdbc:postgresql://localhost:5432/mobilitylab
    * driver: jtds-1.2.5.jar

### Configuring IIS ###
  * Use the project at http://tomcatiis.riaforge.org/. Download and run the installer.
  * Use the following web.config file in wwwroot to avoid the file upload limit imposed by IIS (or amend what is already there):
```
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <system.webServer>
		<security >
			<requestFiltering>
				<requestLimits maxAllowedContentLength="1024000000" />
			</requestFiltering>
		</security>
    </system.webServer>
	<system.web>
		<httpRuntime executionTimeout="54000" maxRequestLength="2097151"/>
	</system.web>
</configuration>
```

### Managing a Microsoft SQL database ###
  * standalone-preview.xml:
    * connection-url: jdbc:jtds:sqlserver://NeuroSQL.ohsu.edu:1344/neurology;integratedSecurity=true;
    * driver: postgresql-9.0-801.jdbc4.jar
    * setting up the indices:
```
USE mobilitylab
GO

CREATE INDEX dtepi_dtr_id
ON dbo.domain_transform_event(domaintransformrequestpersistent_id);

CREATE INDEX dtepi_obj_id
ON dbo.domain_transform_event(objectId);

CREATE INDEX dtepi_objectclassref_id
ON dbo.domain_transform_event(objectclassref_id);

CREATE INDEX dtepi_utcdate
ON dbo.domain_transform_event(utcDate);

GO
```

## Backup/Restore of MSSQL database ##
  1. Create a complete backup through MSSQL Server Management Studio (right click on Databases/Database node)
  1. Restore this backup (right click on the Databases node)
  1. Add new user to the database:
    * Name = mobilityexchange/R0b07
    * Default schema = dbo
    * Database role membership = db\_owner

### Building the Mobility Lab Client ###
  1. Add the Ant view to the Eclipse project.
  1. Drag and drop `build.xml` into the Ant view.
  1. Run the `clean-all` target.
  1. Make sure the JBoss server is running.
  1. Run the `hot-deploy-and-gwt-no-restart` target.
  1. Make sure that the datasource is defined in the `MobilityLab-ds.xml` file and that this is in the `${deploy.jboss.dir}/server/all/deploy` directory.


### GWT Development Mode ###
  1. Right click on the `com.apdm.mobilitylab.client` project and select `"Debug As"->"Web Application"`.
  1. Stop the instance of the `Development Mode` view that runs as a result.
  1. Open the Debug Configuration dialog and add the `/alcina/framework/emul` src folder to the `MobilityLab.html` debug configuration (`Classpath->Advanced->AddFolder`).
  1. Run the `MobilityLab.html` debug configuration.
  1. Open http://localhost:8080/MobilityExchange.html?gwt.codesvr=127.0.0.1:9997

### Schema Migrations ###
#### Handling offline domain transforms ####
  * On the client side, add to MobilityLabRcpSerializedDomainLoader.MobilityLabDTEDeserializer.run().
  * On the server side, add to MobilityLabAppLifecycleServlet.onDomainTransformRequestPersistence().

#### Handling Class Renaming/Deletion ####
The ClassRef class is an abstraction of a class for use in domaintransform persistence tables - so:
| objectclassref\_id | bigint |
|:-------------------|:-------|
| user\_id | bigint |
| valueclassref\_id | bigint|

which refer to the the classref of the source/target of the transform.

The classref collection (table) is regenerated on webapp restart, by:
```
void cc.alcina.framework.entity.domaintransform.ClassrefScanner.scan(Map<String, Date> classes) throws Exception
```

It deletes classref objects for which there is no longer an actual class, and adds new classref objects

This, is of course, problematic if you want to rename an existing persistent class. But...rather than having to rename (potentially) millions of rows in the domaintransform table, you just have to rename the classref row:

```
update classref set refclassname='au.com.barnet.jade.cs.persistent.MoreExcitingClassName'
where refclassname='au.com.barnet.jade.cs.persistent.DullAndInaccurateClassName';
```

If, however, you want to actually delete (and not replace) a persistent object, you're going to have to delete any references to its classref:

```
select id from classref set refclassname='au.com.barnet.jade.cs.persistent.DeleteMe';

delete from domaintransformevent where objectclassref_id=<delete-me-id> or  valueclassref_id=<delete-me-id>;
```


#### Client only Architecture ####
  * The schema migration code will have to run in the client code, because there will be no server to talk to.

### Uploading Binary Data ###
  * I am using the Apache commons upload utilities to handle this.
  * Modifying the entity layer from the servlet: here is an an example of modyfying a DataUpload object:
```
EntityLayerLocator.get().commonPersistenceProvider().getCommonPersistence().getItemById(DataUpload.class,dataUploadId);
DataUpload d=EntityLayerLocator.get().commonPersistenceProvider().getCommonPersistence().getItemById(DataUpload.class,dataUploadId);
TransformManager.get().registerDomainObject(d);
d.setUploaded(true);
ServletLayerUtils.pushTransformsAsRoot();
```

### Permission Annotations ###
Priviledges are specified through a role and a rule. This is an **or** relationship.

### Random Notes ###
  * If there's an error in the server logs (terminal/jboss) then the server webapp needs to be fixed, recompiled, and reloaded (hot-deploy-restart). You'll see that in the client as a statuscodeexception (i.e http status code).
  * The DefaultCreateActionHandler class sets the disdisplayNamePropertyName for a given object (as defined in the @BeanInfo for the class) to be "New" + disdisplayNamePropertyName. You need to make sure this display name is a String and that this is the desired behavior.
  * **Adding new top level domain objects**. For example, I added the "People" objects that need to be read from the DB and displayed in the "People" tab. The whole handshake process is designed to reduce the number of "load object from server" calls - which means I needed to:
    * add a set property to the MobilityLabObjects class to hold the objects to be retrieved objects
    * add that field to registerableDomainObjects in MobilityLabObjects
    * populate the field in the persistence layer in MobilityLabPersistence.loadInitial()

# Jars in the lib directory of the template project #
|jar|where from|notes|
|:--|:---------|:----|
|alcina-entity.jar|from "make-jars" in alcina/build.xml (compiles to alcina/dist)|this should be removed and replaced with a dependency on dist/alcina-entity.jar and/or the project alcina. it's like it is because it was easier to configure the osgi/swt app that way (no pressing need to change, mind you, since the file can be updated via build scripts anywhere you like|
|derby.jar|apache derby project|no need to keep majorly updated (it's fairly stable), but easy to do so|
|gwittir-trunk.jar|built by nick, copy from an alcina template distro|this should only change for gwt minor release changes - i'll write a message on the alcina project google group when an update's necessary|
|gwt-user.jar|copy from an alcina template distro|this should only change for gwt minor release changes - i'll write a message on the alcina project google group when an update's necessary|
|log4j.jar|apache log4j project|no need to keep majorly updated (it's fairly stable), but easy to do so|
|mobilityLab\_entity.jar|from "make-jars" in mobilitylab\_entities/build.xml (compiles to /dist)|as with alcina-entity.jar, there are more elegant ways of doing this - but manual updates work as a first approximation|
|persistence-api-1.0.jar|java persistence annotations|will not change|