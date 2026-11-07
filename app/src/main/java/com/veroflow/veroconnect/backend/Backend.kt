package com.veroflow.veroconnect.backend

import android.content.Context

/**
 * Entry point of the on-device backend.
 *
 *   Backend.auth  -> register / login / profile / password   (AuthService)
 *   Backend.data  -> save and restore everything an account does   (DataService)
 *
 * The screens never touch SQL; they call these services, like a client calling a server API.
 */
object Backend {
    private var appContext: Context? = null
    private var database: VeroDatabase? = null

    lateinit var auth: AuthService
        private set
    lateinit var data: DataService
        private set

    /** Id of the signed-in account, or null for a guest / signed-out user. */
    @Volatile
    var accountId: Long? = null

    fun init(context: Context) {
        if (database != null) return
        appContext = context.applicationContext
        open()
    }

    private fun open() {
        val db = VeroDatabase(appContext!!)
        database = db
        auth = AuthService(db)
        data = DataService(db)
    }

    /** Deletes the database file (all accounts and saved data) and starts again with the demo account. */
    fun wipeAll() {
        val ctx = appContext ?: return
        accountId = null
        database?.close()
        ctx.deleteDatabase(VeroDatabase.DB_NAME)
        open()
    }
}
