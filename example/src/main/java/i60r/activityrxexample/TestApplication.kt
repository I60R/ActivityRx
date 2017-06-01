package i60r.activityrxexample

import android.app.Application
import android.util.Log
import i60r.activityrx.Activities
import i60r.activityrx.ActivityRx
import i60r.activityrx.On


/***
 * Created by 160R on 08.05.17.
 */

class TestApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        ActivityRx.init(this)

        Activities.events()
                .subscribe {
                    Log.d("~~~ ~~~ ~>", "${it.id} ~> ${it.on}")
                    if (it.on == On.RESUME) {
                        Log.d("==>", "*\n*\n*\n*")
                    }
                }

        Activities.observe(Activity1::class.java)
                .filter { it.on.visible }
                .flatMap {
                    it.ui.finish()
                    Activities.start(Activity2::class.java)
                            .filter { it.on.visible }
                }
                .flatMap {
                    it.ui.finish()
                    Activities.start(Activity3::class.java)
                            .filter { it.on.visible }
                            .takeUntil { it.on.visible }
                }
                .flatMap {
                    Activities.current()
                            .subscribe {
                                Log.d("--- --- ->", "${it.id} -> ${it.on}")
                            }

                    Activities.observe(it.ui)
                }
                .subscribe {
                    Log.d("=== === =>", "${it.id} => ${it.on.name}")
                }
    }
}
