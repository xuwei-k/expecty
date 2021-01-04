
```
sbt
publishSigned
exit

env SCALAJS_VERSION="0.6.33" sbt
clean
expectyJS2_11/publishSigned
expectyJS2_12/publishSigned
expectyJS/publishSigned

env SCALANATIVE_VERSION="0.4.0-M2" sbt
clean
expectyNative2_11/publishSigned
```
