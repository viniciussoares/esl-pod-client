package br.com.wakim.eslpodclient.view

import android.content.Context
import android.os.Bundle
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.app.NavUtils
import android.support.v4.app.TaskStackBuilder
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import br.com.wakim.eslpodclient.R
import br.com.wakim.eslpodclient.dagger.ActivityComponent
import br.com.wakim.eslpodclient.dagger.AppComponent
import br.com.wakim.eslpodclient.dagger.module.ActivityModule
import br.com.wakim.eslpodclient.extensions.snack
import br.com.wakim.eslpodclient.extensions.startActivity
import br.com.wakim.eslpodclient.presenter.Presenter
import br.com.wakim.eslpodclient.rx.PermissionPublishSubject
import br.com.wakim.eslpodclient.settings.view.SettingsActivity
import butterknife.bindOptionalView
import org.jetbrains.anko.find
import org.jetbrains.anko.findOptional
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper

open class BaseActivity<T : Presenter<*>> : AppCompatActivity(), PermissionRequester {

    companion object {
        final const val PARENT_EXTRA = "PARENT_EXTRA"
    }

    val toolbar : Toolbar? by bindOptionalView(R.id.toolbar)
    val drawerLayout : DrawerLayout? by bindOptionalView(R.id.drawer_layout)
    val navigationView : NavigationView? by bindOptionalView(R.id.navigation_view)

    lateinit var activityComponent : ActivityComponent
    lateinit var presenter : T

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityComponent = getAppComponent()!!.plus(ActivityModule(this))
    }

    override fun onStart() {
        super.onStart()
        presenter.onStart()
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase))
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)

        toolbar?.let {
            setSupportActionBar(toolbar)

            val actionBar = supportActionBar!!

            actionBar.setDisplayHomeAsUpEnabled(true)

            if (navigationView != null) {
                actionBar.setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp)

                (navigationView as NavigationView).setNavigationItemSelectedListener { item ->
                    if (item.itemId == R.id.drawer_settings) {
                        startActivity<SettingsActivity>()
                        true
                    } else {
                        false
                    }
                }
            } else {
                actionBar.setDisplayShowHomeEnabled(true)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        if (intent.hasExtra(PARENT_EXTRA)) {
            supportFinishAfterTransition()
            return true
        }

        val parentIntent = NavUtils.getParentActivityIntent(this)

        if (parentIntent == null) {
            supportFinishAfterTransition()
            return true
        }

        if (NavUtils.shouldUpRecreateTask(this, parentIntent)) {

            TaskStackBuilder.create(this)
                    .addNextIntentWithParentStack(parentIntent)
                    .startActivities()

            supportFinishAfterTransition()
            return true;
        } else {
            startActivity(parentIntent)
            supportFinishAfterTransition()

            return true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item!!.itemId == android.R.id.home) {
            drawerLayout?.openDrawer(GravityCompat.START) ?: finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        presenter.onRestoreInstanceState(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        presenter.onSaveInstanceState(outState!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        presenter.onResume()
    }

    override fun onStop() {
        super.onStop()
        presenter.onStop()
    }

    fun getAppComponent() : AppComponent? {
        return applicationContext.getSystemService(AppComponent::class.java.simpleName) as AppComponent?
    }

    override fun getSystemService(name: String?): Any? {
        if (ActivityComponent::class.java.simpleName == name) {
            return activityComponent
        }

        return super.getSystemService(name)
    }

    override fun requestPermissions(requestCode: Int, vararg permissions: String) {
        ActivityCompat.requestPermissions(this, permissions, requestCode)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        PermissionPublishSubject.INSTANCE
                .publishSubject
                .onNext(PermissionPublishSubject.Permission(requestCode, permissions, grantResults))
    }

    open fun showMessage(messageResId: Int) {
        val view = findOptional<CoordinatorLayout>(R.id.coordinator_layout) ?: find(android.R.id.content)

        snack(view, message = messageResId)
    }
}