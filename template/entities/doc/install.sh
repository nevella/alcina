#!/bin/bash
. ./env.local.sh

CVS_JADE3=$BARNET_CVS/projects/alcinaTemplate3

#step 2
cp $CVS_JADE3/entities/doc/postgres-ds.xml $JBOSS_DEFAULT_SERVER/deploy

#step 3
cp  $CVS_JADE3/entities/doc/barnet.p12 $JBOSS_DEFAULT_SERVER/conf
cp  $CVS_JADE3/entities/doc/bnc-authentication-url.properties $JBOSS_DEFAULT_SERVER/conf
cp  $CVS_JADE3/entities/doc/alcinaTemplate-server.properties $JBOSS_DEFAULT_SERVER/conf
cp  $CVS_JADE3/entities/doc/server.keystore $JBOSS_DEFAULT_SERVER/conf




cd $CVS_JADE3/entities/lib/jaxb2
cp jaxb-api-2.0.jar $JBOSS_DEFAULT_SERVER/lib
cp jaxb-xjc-2.0.1.jar $JBOSS_DEFAULT_SERVER/lib
cp jsr173_api.jar $JBOSS_DEFAULT_SERVER/lib

cd $CVS_JADE3/entities/lib/axis
cp axis.jar $JBOSS_DEFAULT_SERVER/lib
cp axis-ant.jar $JBOSS_DEFAULT_SERVER/lib
cp commons-discovery-0.4.jar $JBOSS_DEFAULT_SERVER/lib
cp jaxrpc.jar $JBOSS_DEFAULT_SERVER/lib
cp saaj.jar $JBOSS_DEFAULT_SERVER/lib

cd $CVS_JADE3/entities/lib/jdbc
cp postgresql-8.1-407.jdbc3.jar $JBOSS_DEFAULT_SERVER/lib

cd $CVS_JADE3/../common/java/lib/jboss-cache/lib
cp jboss-cache-jdk50.jar $JBOSS_DEFAULT_SERVER/lib
cp jgroups.jar $JBOSS_DEFAULT_SERVER/lib

cd $CVS_JADE3/../common/java/lib/jboss-cache/deploy
cp ejb3-entity-cache-service.xml $JBOSS_DEFAULT_SERVER/deploy



#step 3 revisited
echo Remember to CONFIGURE [jboss]/server/default/deploy/jboss-web.deployer/server.xml


#step 4 and on
cd $CVS_JADE3/entities/doc
./rebuild.sh

