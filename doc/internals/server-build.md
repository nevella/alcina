# alcina > internals > server-build

Assuming you're building to a container, the container build will be defined by <container-name>.json

The build folder is defined by the property `run/env/APP_PATH` - e.g `/alcina/build/alcina/demo/dev`

Inside that build folder, the `alcina` folder will be bind-mounted to `/opt/jboss/.alcina`, and the app data folder at 
`/opt/jboss/.alcina/<appname>-server` -- on the host that will be something like `/alcina/build/alcina/demo/dev/alcina//<appname>-server`

