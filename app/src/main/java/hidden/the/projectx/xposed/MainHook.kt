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
        var isFakeLocationEnabled = true
    }

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        // 1. Hooking pada aplikasi Gojek Driver
        if (lpparam.packageName == "com.gojek.partner") {
            hookGojekOrder(lpparam)
        }

        // 2. Hooking pada aplikasi Grab Driver
        if (lpparam.packageName == "com.grabtaxi.driver2") {
            hookGrabOrder(lpparam)
        }

        // 3. Hooking pada Framework System / Location Manager (opsional jika lokasi juga di-hook via Xposed)
        hookLocationService(lpparam)
    }

    // ==========================================
    // HOOK GOJEK DRIVER
    // ==========================================
    private fun hookGojekOrder(lpparam: LoadPackageParam) {
        try {
            // Hooking pada class penanganan Push Notification atau Order Event di Gojek
            XposedHelpers.findAndHookMethod(
                "com.google.firebase.messaging.FirebaseMessagingService",
                lpparam.classLoader,
                "onMessageReceived",
                "com.google.firebase.messaging.RemoteMessage",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val remoteMessage = param.args[0]
                        val context = param.thisObject as? Context ?: return

                        // Ambil data payload push notification
                        val dataMap = XposedHelpers.callMethod(remoteMessage, "getData") as? Map<*, *>
                        val notification = XposedHelpers.callMethod(remoteMessage, "getNotification")

                        val title = XposedHelpers.callMethod(notification, "getTitle") as? String ?: ""
                        val body = XposedHelpers.callMethod(notification, "getBody") as? String ?: ""
                        val payloadString = dataMap?.toString() ?: ""

                        Log.d(TAG, "Gojek FCM Message: $title | $body | $payloadString")

                        // Cek indikator orderan baru
                        if (payloadString.contains("booking", ignoreCase = true) ||
                            title.contains("trip", ignoreCase = true) ||
                            body.contains("trip baru", ignoreCase = true) ||
                            body.contains("pesanan", ignoreCase = true)
                        ) {
                            XposedBridge.log("PROJECT-X: Orderan Gojek Terdeteksi System-Level! Mematikan Fake Location.")
                            triggerAutoStop(context)
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            XposedBridge.log("PROJECT-X Error Hook Gojek: ${e.message}")
        }
    }

    // ==========================================
    // HOOK GRAB DRIVER
    // ==========================================
    private fun hookGrabOrder(lpparam: LoadPackageParam) {
        try {
            // Hooking pada penanganan message/GCM/FCM di Grab Driver
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

                        // Cek kata kunci payload orderan Grab
                        if (payloadString.contains("job", ignoreCase = true) ||
                            payloadString.contains("booking", ignoreCase = true) ||
                            payloadString.contains("ping", ignoreCase = true)
                        ) {
                            XposedBridge.log("PROJECT-X: Orderan Grab Terdeteksi System-Level! Mematikan Fake Location.")
                            triggerAutoStop(context)
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            XposedBridge.log("PROJECT-X Error Hook Grab: ${e.message}")
        }
    }

    // ==========================================
    // METODE PEMUTUSAN LOKASI (AUTO STOP)
    // ==========================================
    private fun triggerAutoStop(context: Context) {
        // Matikan flag global hook lokasi di memori
        isFakeLocationEnabled = false

        // Kirimkan Broadcast global ke modul utama PROJECT-X
        val intent = Intent(ACTION_STOP_FAKE).apply {
            // Pastikan menggunakan flag broadcast yang sesuai
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            setPackage("hidden.the.projectx") // Ganti dengan package modul Anda
        }
        context.sendBroadcast(intent)
    }

    private fun hookLocationService(lpparam: LoadPackageParam) {
        // Jika Anda melakukan bypass/spoofing lokasi langsung di level System Server/LocationManager
        try {
            XposedHelpers.findAndHookMethod(
                "android.location.Location",
                lpparam.classLoader,
                "getLatitude",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        // Jika isFakeLocationEnabled = false, lewati spoofing dan kembalikan ke lokasi asli GPS
                        if (!isFakeLocationEnabled) {
                            return
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            // Abaikan jika class tidak ditemukan
        }
    }
}
