package xyz.pepe.glacierclient.calculatorapp

import android.app.Application
import xyz.pepe.glacierclient.calculatorapp.di.AppContainer

class CalculatorApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
