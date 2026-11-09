package app.rork.qu1cg8ux8i9pbr72mmsss

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.navigation.AppNavigation
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.theme.AppTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                AppNavigation()
            }
        }
    }
}
