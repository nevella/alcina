#!/bin/bash
. ./env.local.sh

cd $BARNET_CVS/projects/common/java
ant make-jar
cd $BARNET_CVS/projects/alcinaTemplate3/server
ant clean-deployAndGwt -Ddeploy.jbossdeploy.dir=$JBOSS_DEFAULT_SERVER/deploy -Ddeploy.jbosslibs.dir=$JBOSS_DEFAULT_SERVER/lib
#yeah