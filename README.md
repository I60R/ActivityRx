# Manage Android Activities in reactive way

**without inheritance**

--------
--------

```kotlin
        Activities.init(this)

        Activities.events()
                .subscribe {
                    Log.d("ActivitiesLogger", "${it.id} ~> ${it.on}")
                    if (it.on == On.RESUME) {
                        Log.d("ActivitiesLogger", "*\n*\n*\n")
                    }
                }
```

--------
--------

```kotlin
        Activities.current()
                .filter { it.usable }
                .subscribe({
                    it.ui.finish()
                }, {}, {
                    Log.d("ActivitiesManager", "finish all activities")
                })
```

--------
--------

```kotlin
        Activities.observe(MainActivity::class.java)
                .subscribe {
                    Log.d("MainActivityLogger", "${it.on}")
                }
```

--------
--------

```kotlin
        Activities
                .bind(MainActivity::class.java)
                .subscribe({}, {}, {
                    Log.d("MainActivityBinding", "activity destroyed")
                })
```

--------
--------

```kotlin
        Activities.start(MainActivity::class.java)
                .filter { it.visible }.firstElement()
                .flatMap { it.ui.getUserInput() }
                .flatMap { validate(it) }
                .flatMap { 
                    Activities.start(SecondActivity::class.java, Bundle().apply { putString("inp", it) }) 
                            .filter { it.visible  }.firstElement()
                }
                .subscribe {
                    it.ui.displayInputFromBundle()
                }
```
