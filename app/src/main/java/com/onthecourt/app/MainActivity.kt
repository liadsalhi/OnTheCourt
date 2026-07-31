package com.onthecourt.app

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.firebase.auth.FirebaseAuth
import com.onthecourt.app.auth.LoginActivity
import com.onthecourt.app.databinding.ActivityMainBinding

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.nav_home)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val topLevelDestinations = setOf(
            R.id.sportSelectionFragment,
            R.id.myGamesFragment,
            R.id.friendsFragment
        )
        val appBarConfig = AppBarConfiguration(topLevelDestinations)
        setupActionBarWithNavController(navController, appBarConfig)

        // "Home" always hard-resets the back stack to sportSelectionFragment, even from
        // nested screens (courtMap/courtDetail/createGame/gameDetail/chat) — not just the
        // default NavigationUI popUpTo+saveState behavior, which only works for top-level
        // tab destinations and leaves nested screens on the stack.
        binding.bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.sportSelectionFragment) {
                navController.popBackStack(R.id.sportSelectionFragment, false)
                true
            } else {
                NavigationUI.onNavDestinationSelected(item, navController)
            }
        }

        // Update toolbar title and checked bottom-nav tab on destination change
        navController.addOnDestinationChangedListener { _, destination, _ ->
            supportActionBar?.title = when (destination.id) {
                R.id.sportSelectionFragment -> getString(R.string.nav_home)
                R.id.myGamesFragment -> getString(R.string.nav_my_games)
                R.id.friendsFragment -> getString(R.string.nav_friends)
                R.id.courtMapFragment -> getString(R.string.btn_search)
                R.id.courtDetailFragment -> getString(R.string.title_court)
                R.id.createGameFragment -> getString(R.string.create_game_title)
                R.id.gameDetailFragment -> getString(R.string.title_game)
                R.id.chatFragment -> getString(R.string.title_chat)
                else -> getString(R.string.app_name)
            }
            // NavigationUI sets the Up indicator itself and doesn't always respect
            // the toolbar's app:navigationIconTint, so force it white here too.
            binding.toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, R.color.on_primary))

            when (destination.id) {
                R.id.sportSelectionFragment, R.id.myGamesFragment, R.id.friendsFragment ->
                    binding.bottomNav.menu.findItem(destination.id)?.isChecked = true
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        return navHostFragment.navController.navigateUp() || super.onSupportNavigateUp()
    }
}