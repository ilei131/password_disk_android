package com.passworddisk.app

import android.app.Application
import com.passworddisk.app.data.PasswordDatabase

class PasswordDiskApplication : Application() {
    val database: PasswordDatabase by lazy { PasswordDatabase.getDatabase(this) }
}