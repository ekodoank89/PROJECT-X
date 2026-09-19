package hidden.the.projectx.xposed

import android.content.Context
import android.content.Intent
import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class MainHook : IXposedHookLoadPackage {

    companion object {
        const val TAG = "PROJECT-X-Xposed"
        const val ACTION_STOP_FAKE = "hidden.the.projectx.ACTION_STOP_FAKE"
    }

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        // Deteksi aplikasi Gojek Driver
        if (lpparam.packageName == "com.gojek.partner") {
            hookGojekOrder(lpparam)
        }

        // Deteksi aplikasi Grab Driver
        if (lpparam.packageName == "com.grabtaxi.driver2") {
            hookGrabOrder(lpparam)
        }
    }

    private fun hookGojekOrder(lpparam: LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                "com.google.firebase.messaging.FirebaseMessagingService",
                lpparam.classLoader,
                "onMessageReceived",
                "com.google.firebase.messaging.RemoteMessage",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val remoteMessage = param.args[0]
                        val context = param.thisObject as? Context ?: return

                        val dataMap = XposedHelpers.callMethod(remoteMessage, "getData") as? Map<*, *>
                        val notification = XposedHelpers.callMethod(remoteMessage, "getNotification")

                        val title = XposedHelpers.callMethod(notification, "getTitle") as? String ?: ""
                        val body = XposedHelpers.callMethod(notification, "getBody") as? String ?: ""
                        val payloadString = dataMap?.toString() ?: ""

                        Log.d(TAG, "Gojek FCM Message: $title | $body | $payloadString")

                        if (payloadString.contains("booking", ignoreCase = true) ||
                            title.contains("trip", ignoreCase = true) ||
                            body.contains("trip baru", ignoreCase = true) ||
                            body.contains("pesanan", ignoreCase = true)
                        ) {
                            XposedBridge.log("PROJECT-X: Orderan Gojek Terdeteksi! Mematikan Fake Location.")
                            triggerAutoStop(context)
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            XposedBridge.log("PROJECT-X Error Hook Gojek: ${e.message}")
        }
    }

    private fun hookGrabOrder(lpparam: LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                "com.google.firebase.messaging.FirebaseMessagingService",
                lpparam.classLoader,
                "onMessageReceived",
                "com.google.firebase.messaging.RemoteMessage",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val remoteMessage = param.args[0]
                        val context = param.thisObject as? Context ?: return

                        val dataMap = XposedHelpers.callMethod(remoteMessage, "getData") as? Map<*, *>
                        val payloadString = dataMap?.toString() ?: ""

                        Log.d(TAG, "Grab FCM Message: $payloadString")

                        if (payloadString.contains("job", ignoreCase = true) ||
                            payloadString.contains("booking", ignoreCase = true) ||
                            payloadString.contains("ping", ignoreCase = true)
                        ) {
                            XposedBridge.log("PROJECT-X: Orderan Grab Terdeteksi! Mematikan Fake Location.")
                            triggerAutoStop(context)
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            XposedBridge.log("PROJECT-X Error Hook Grab: ${e.message}")
        }
    }

    private fun triggerAutoStop(context: Context) {
        val intent = Intent(ACTION_STOP_FAKE).apply {
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            setPackage("hidden.the.projectx")
        }
        context.sendBroadcast(intent)
    }
}
