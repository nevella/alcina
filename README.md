## Alcina ##
Alcina is:
  * an application framework that provides transparent client-server propogation of changes to the app domain
  * "a stab at a generalised solution to webapp online/offline persistence".

#### How it works - the very simple version: ####
  * Objects are loaded from the server or instantiated locally, and registered with the `TransformManager`
  * If the objects are to be edited in the browser UI, they're bound to the widgets via the (great) Gwittir library
  * The transform manager propogates serialized client graph changes to the server, while keeping a Gears/HTML5 local storage copy for offline app reload/remote synchronisation and offline load.
  * Servlets and JPA take care of commiting these changes to the server graph.

Note - this is _any_ domain object graph (with a few caveats) - the functionality encompasses a subset of possible graphs persistable via JPA - but a subset that's essentially equal in functionality - see a [working example](ExampleOfflineEditableDomain.md).

#### Getting started: ####
  * _**(Requires Ant 1.6+, Java 6+, Git)**_
  * Check out the source: `git clone https://code.google.com/p/alcina/`
  * `> cd alcina/tools/appcreator`
  * `> ant`
  * wait a wee while (this will download all required jars, the JBoss app server, compile the java and gwt code, launch the app server)
  * once appserver launch is complete, go to http://localhost:8080/MyAppClient.html in your favourite browser


What has been created is  a "basic" demo app shell - "basic" being in quotes, because the supporting code is _not_ super-simple - see FrameworkApologia. Note that Alcina is basically two things - the client-server transform support, and various GWT client modules which take advantage of that support...the former could be called a toolkit if you really wanted to stretch things...


#### Transform Manager and other base features ####
  * SimplePersistentObjectModification
  * ClientSideAnnotationsAndInstantiation
  * PersistenceOptions
  * [Client and server annotated registry to reduce code coupling](Registry.md)


#### Other features (client application framework) ####
  * [Annotations - property permissions, validators, edit customisers etc](ClientFrameworkAnnotations.md)
  * [Top-to-bottom (client > server > entity and back) search and result rendering support](ObjectTreeRenderingAndSearch.md)
  * Editing "mini-ide" - think Django on annotated steroids
  * A fairly robust layout system
  * Gazillions of widgets, customisers, renderers

**Build/deployment requirements**  (automatically downloaded/configured by the appcreator)
  * JPA/EJB3 support in the servlet container
  * GWT 2.1.0

#### Documentation ####
Apart from the Wiki, check out the Google Docs (that's where new content is going, probly):
https://drive.google.com/#folders/0BxERFypYFvg7bUlRMkFVRVJObFk

#### Contact, help etc ####
I'm _very_ interested in people's opinions/suggestions/critiques of this project - it has sort of surprising ramifications for all aspects of webapp development - in a way, it's a 20% cure for "all the things I used to hate" about the way webapps get messy, the other 80% being GWT. And there's still a lot that's undocumented or just not obvious from reading the code, which it might help to ask me about. So...please drop me a line at http://groups.google.com/group/alcina-users - or my email (not hard to extract, if you're human). Best, Nick.

**Progress**
  * Update 4 - June 16 - Milestone 0.1 - hurray. There's still a lot of cleaning and beautification to do, so much of the API is subject to change (mostly naming changes), but functional apps can be built on what's currently in the tree.
  * Update 3 - May 30 - No luck - yet closer - template application now in the svn tree, just need to write the template mover/deployer (but it's already doable manually)
  * Update 2 - April 21 - Well, yeah, still not there for M0.1, but close - basic conflict resolution stuff done, so just need to clean up the handshake and get the app template creator working. Call it April 30 with luck.
  * Update 1 (slipping a bit) - a preliminary version of the code is in the source tree - but Milestone0.1 is what will be the first generally useful release, and it doesn't look like it'll be out until at least April 15.
  * The code's been in production use for about a year, target date for getting (cleaned, documented) code up is March 15, 2010

#### Not much change
In 8 years - at this level, but underneath...lordy! 