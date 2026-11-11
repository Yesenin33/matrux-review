package ru.matrux.app

import android.app.Application
import ru.matrux.app.store.StoreSdk

class MatruxApplication : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        StoreSdk.init(this)
        graph = AppGraph(this)
    }
}

val android.content.Context.graph: AppGraph get() = (applicationContext as MatruxApplication).graph
