package com.cnx.publisher.utils

import java.text.SimpleDateFormat

class FilePathGenerator {

    companion object {
        fun getFilePath(): String {
            val formatter = SimpleDateFormat("dd-MM-yyyy")
            return formatter.format(System.currentTimeMillis()) + System.currentTimeMillis().toString().reversed().substring(0, 4)
        }
    }

}