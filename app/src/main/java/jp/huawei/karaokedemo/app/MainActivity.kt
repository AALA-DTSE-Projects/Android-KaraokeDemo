package jp.huawei.karaokedemo.app

import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Process
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import jp.huawei.karaokedemo.R
import jp.huawei.karaokedemo.app.model.LyricsEvent
import jp.huawei.karaokedemo.app.model.TerminateEvent
import jp.huawei.karaokedemo.databinding.ActivityMainBinding
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MainActivity : AppCompatActivity() {
    companion object {
        const val DISTRIBUTED_PERMISSION_CODE = 0
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupBinding()
        init()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLyricsUpdate(event: LyricsEvent) {
        binding.lyrics.text = event.lyrics
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTerminateEvent(event: TerminateEvent) {
        finish()
    }

    private fun setupBinding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private fun init() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestDistributedPermission()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestDistributedPermission() {
        val requestedPermission = pickDistributedPermission()
        val message = when {
            requestedPermission == null -> {
                "Please add DISTRIBUTED permission to your MANIFEST"
            }
            checkSelfPermission(requestedPermission) == PackageManager.PERMISSION_DENIED -> {
                showPermissionRequest(requestedPermission)
                return
            }
            else -> {
                "permission $requestedPermission is already granted!"
            }
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun showPermissionRequest(permission: String) {
        if (shouldShowRequestPermissionRationale(permission)) {
            AlertDialog.Builder(this)
                .setMessage("We need the permission to exchange data across device")
                .setCancelable(true)
                .setPositiveButton("OK") { _,_ ->
                    requestPermissions(
                        arrayOf(
                            permission
                        ), DISTRIBUTED_PERMISSION_CODE
                    )
                }.show()
        } else {
            requestPermissions(arrayOf(permission), DISTRIBUTED_PERMISSION_CODE)
        }
    }

    private fun pickDistributedPermission(): String? {
        val privilegedPermission = "com.huawei.hwddmp.servicebus.BIND_SERVICE"
        val dangerousPermission = "com.huawei.permission.DISTRIBUTED_DATASYNC"
        val isPrivileged: Boolean = isPrivilegedApp(this, Process.myUid())
        var candidate: String? = null
        val packageManager = this.packageManager
        try {
            val packageInfo =
                packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            for (i in packageInfo.requestedPermissions.indices) {
                if (isPrivileged && privilegedPermission == packageInfo.requestedPermissions[i]) {
                    return privilegedPermission
                }
                if (candidate == null && dangerousPermission == packageInfo.requestedPermissions[i]) {
                    candidate = dangerousPermission
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
        }
        return candidate
    }

    private fun isPrivilegedApp(context: Context, uid: Int): Boolean {
        if (uid == Process.SYSTEM_UID) {
            return true
        }
        val pm: PackageManager = context.packageManager ?: return false
        return pm.checkSignatures(uid, Process.SYSTEM_UID) == PackageManager.SIGNATURE_MATCH
    }
}