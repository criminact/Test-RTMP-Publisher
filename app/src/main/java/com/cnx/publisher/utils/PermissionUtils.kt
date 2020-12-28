package com.cnx.publisher.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager

class PermissionUtils(val mContext: Context) {

    fun checkPermission(permissionName: String): Boolean {
        return (mContext.checkSelfPermission(permissionName) == PackageManager.PERMISSION_GRANTED)
    }

    fun askPermission(permissionNames: Set<String>, requestCode: Int){
            (mContext as Activity).requestPermissions(permissionNames.toTypedArray(), requestCode)
    }

}