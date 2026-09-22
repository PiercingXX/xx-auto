# xx-auto

> A driving screen for a phone in a mount. Big targets, black ground, gets out
> of the way.

Shows what's playing, gives a thumb play / skip / thumbs-up, opens xx-maps,
dials a favourite. Manual launch by default; can open itself when the car's
Bluetooth connects. No `INTERNET`. Nothing leaves the phone.

Two flavors. `noGms` is the default and has no Google artifact on the
classpath. `gms` adds an opt-in templated **media** surface for a real Android
Auto head unit — never a map: navigation on the car screen belongs to xx-maps
(AU12). Both flavors are the same phone app.

```
package: com.piercingxx.xxauto
```

```sh
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

[design.md](design.md) · [todo.md](todo.md) · [contracts/XX-MAPS.md](contracts/XX-MAPS.md)
