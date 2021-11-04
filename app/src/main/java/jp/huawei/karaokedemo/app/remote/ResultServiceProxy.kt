package jp.huawei.karaokedemo.app.remote

import android.os.IBinder
import android.os.Parcel


internal class ResultServiceProxy(private val remote: IBinder?) {

    companion object {
        private const val DESCRIPTOR = "com.huawei.karaokedemo.controller.IResultInterface"
        private const val CONNECT_COMMAND = IBinder.FIRST_CALL_TRANSACTION
        private const val DISCONNECT_COMMAND = IBinder.FIRST_CALL_TRANSACTION + 1
    }

    fun connect(deviceId: String) {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        data.writeInterfaceToken(DESCRIPTOR)
        data.writeString(deviceId)
        try {
            remote?.transact(CONNECT_COMMAND, data, reply, 0)
            reply.writeNoException()
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    fun disconnect(deviceId: String) {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        data.writeInterfaceToken(DESCRIPTOR)
        data.writeString(deviceId)
        try {
            remote?.transact(DISCONNECT_COMMAND, data, reply, 0)
            reply.writeNoException()
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

}