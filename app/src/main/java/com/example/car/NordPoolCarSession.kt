package com.example.car

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session

class NordPoolCarSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        return NordPoolCarScreen(carContext)
    }
}
