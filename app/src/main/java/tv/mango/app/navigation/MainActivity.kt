package tv.mango.app.navigation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import tv.mango.app.databinding.ActivityMainBinding

/**
 * The application's only activity.
 *
 * Screens are fragments swapped inside a single container. A single activity
 * avoids the window-creation cost of an activity transition on every
 * navigation - measurable on this class of hardware - and keeps Back
 * behaviour in one place rather than spread across manifest task affinities.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
