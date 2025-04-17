# alcina > misc > debugging-and-metrics

## Romcom

To debug the protocol/metrics, use flight events on the server (can be a devconsole) -
see cc.alcina.framework.servlet.environment javadoc

- change romcom server config

```
EnvironmentManager.flightRecordingEnabled=true
FlightEventRecorder.enabled=true
```

- restart app, do what needs testing, stop

### Local

[TODO - navigate to 'most recent local flight' - also 'load flight' shd just change the sequence place]

### Remote

- move the events folder
  - The app logs will contain something like `FlightEventRecorder :: recording to /tmp/flight-event/flight-alcina-devconsole-20250314_074931_526.leela.local-0-ppqx-rpevqzu`
  - Move that folder to `/tmp/flight-event/extract/`
- Start the alcina devconsole
- Navgiate to <http://127.0.0.1:31009/seq>
- enter `load flight` in the omnibox

## WD

```
ElementQuery.debugClick=true
```
