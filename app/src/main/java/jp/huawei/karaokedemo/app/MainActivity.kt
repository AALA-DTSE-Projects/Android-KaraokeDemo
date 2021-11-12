package jp.huawei.karaokedemo.app

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Process
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.huawei.ohos.localability.AbilityUtils
import jp.huawei.karaokedemo.R
import jp.huawei.karaokedemo.app.model.SetDeviceIdEvent
import jp.huawei.karaokedemo.app.model.TerminateEvent
import jp.huawei.karaokedemo.app.remote.ResultServiceProxy
import jp.huawei.karaokedemo.databinding.ActivityMainBinding
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MainActivity : AppCompatActivity() {
    companion object {
        const val DISTRIBUTED_PERMISSION_CODE = 0
        const val HARMONY_BUNDLE_NAME = "com.huawei.karaokedemo"
        const val HARMONY_ABILITY_NAME = "com.huawei.karaokedemo.MainAbility"
        const val HARMONY_SERVICE_ABILITY_NAME = "com.huawei.karaokedemo.ResultServiceAbility"
    }

    private lateinit var binding: ActivityMainBinding
    private var serviceConnection: ServiceConnection? = null
    private var resultServiceProxy: ResultServiceProxy? = null
    private val deviceIds = mutableListOf<String>()
    private val TAG = MainActivity::class.java.name

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

    override fun onDestroy() {
        super.onDestroy()
        resultServiceProxy?.let {
            deviceIds.forEach { deviceId ->
                Log.d(TAG, "Disconnect with $deviceId")
                it.disconnect(deviceId)
            }
        }
        serviceConnection?.let {
            AbilityUtils.disconnectAbility(this, it)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTerminateEvent(event: TerminateEvent) {
        finish()
    }

    @SuppressLint("SetTextI18n")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSetDeviceIdEvent(event: SetDeviceIdEvent) {
        val deviceId = event.deviceId
        deviceIds.add(deviceId)
        binding.deviceId.text = "Connected from device: $deviceId"
        connectToHarmonyService {
            it.connect(deviceId)
        }
        showMessage("Connected from mobile app. \n You can try singing on the mobile mic", true) {}
    }

    private fun setupBinding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private fun init() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestDistributedPermission()
            try {
                AbilityUtils.startAbility(
                    this,
                    Intent().setComponent(ComponentName(HARMONY_BUNDLE_NAME, HARMONY_ABILITY_NAME))
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
                return
            }
        }
        showMessage(message, true) {}
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun showPermissionRequest(permission: String) {
        if (shouldShowRequestPermissionRationale(permission)) {
            showMessage("We need the permission to exchange data across device", false) {
                requestPermissions(
                    arrayOf(
                        permission
                    ), DISTRIBUTED_PERMISSION_CODE
                )
            }
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

    private fun connectToHarmonyService(callback: (ResultServiceProxy) -> Unit)  {
        val intent = Intent()
        val componentName = ComponentName(HARMONY_BUNDLE_NAME, HARMONY_SERVICE_ABILITY_NAME)
        intent.component = componentName
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                Log.d(TAG, "Connected to $HARMONY_SERVICE_ABILITY_NAME")
                resultServiceProxy = ResultServiceProxy(service)
                serviceConnection = this
                callback.invoke(resultServiceProxy ?: return)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                resultServiceProxy = null
                serviceConnection = null
            }
        }
        AbilityUtils.connectAbility(this, intent, connection)
    }

    private fun showMessage(message: String, cancelable: Boolean, callBack: () -> Unit) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setCancelable(cancelable)
            .setPositiveButton("OK") { _,_ ->
                callBack.invoke()
            }.show()
    }
}