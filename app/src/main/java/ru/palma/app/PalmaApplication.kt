package ru.palma.app

import android.app.Application
import ru.palma.app.store.StoreSdk

class PalmaApplication : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        StoreSdk.init(this)
        graph = AppGraph(this)
    }
}

val android.content.Context.graph: AppGraph get() = (applicationContext as PalmaApplication).graph
