package i60r.activityrxexample

import android.app.Application
import android.util.Log
import i60r.activityrx.ActivityRx
import i60r.activityrx.On


/***
 * Created by 160R on 08.05.17.
 */

class TestApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        ActivityRx
                .init(this)

        ActivityRx
                .events()
                .subscribe {
                    Log.d(">>>>>>>>>>", it.component + " ~> " + it.on)
                    if (it.on == On.RESUME) {
                        Log.d("==>", "<==")
                    }
                }


        ActivityRx
                .bind(Activity1::class.java)
                .filter { it.on.visible }
                .flatMap {
                    it.ui.finish()
                    ActivityRx
                            .start(Activity2::class.java)
                            .filter { it.on.visible }
                }
                .flatMap {
                    it.ui.finish()
                    ActivityRx
                            .start(Activity3::class.java)
                            .filter { it.on.visible }
                            .takeUntil { it.on.visible }
                }
                .flatMap {
                    ActivityRx
                            .observe(it.ui)
                }
                .subscribe {
                    Log.d("=== === =>", "${it.component} ${it.on.name}")
                }
    }
}
