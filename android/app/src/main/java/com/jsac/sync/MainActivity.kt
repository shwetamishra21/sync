package com.jsac.sync

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ⚠️ OPTIONAL: Uncomment the lines below to force clear DataStore on NEXT app launch
        // This is useful for development/testing to always start from login screen
        //
        // IMPORTANT: Remove these lines after testing!
        //
        // lifecycleScope.launch {
        //     applicationContext.dataStore.edit { preferences ->
        //         preferences.clear()
        //         Log.d("MainActivity", "🗑️ DataStore cleared on app launch")
        //     }
        // }

        setContentView(R.layout.activity_main)
    }
}

/*
 * HOW TO USE DEBUG DATASTORE CLEAR:
 *
 * 1. Uncomment the lifecycleScope.launch block above
 * 2. Add import: import androidx.lifecycle.lifecycleScope
 * 3. Add import: import androidx.datastore.preferences.preferencesDataStore
 * 4. Add import: import androidx.datastore.preferences.core.edit
 * 5. Add import: import android.util.Log
 * 6. Run the app - DataStore will be cleared
 * 7. Remove the code
 * 8. Rebuild and test
 *
 * This will force the login screen to appear on next app launch
 */