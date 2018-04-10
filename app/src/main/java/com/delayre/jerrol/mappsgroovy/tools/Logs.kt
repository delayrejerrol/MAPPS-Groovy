package com.delayre.jerrol.mappsgroovy.tools

import android.util.Log

object Logs {

    /**
     * Priority constant for the println method; use Log.d.
     */
    fun d(tag: String, message: String) {
        Log.d(tag, message)
    }

    /**
     * Priority constant for the println method; use Log.e.
     */
    fun e(tag: String, message: String) {
        Log.e(tag, message)
    }

    /**
     * Priority constant for the println method; use Log.i.
     */
    fun i(tag: String, message: String) {
        Log.i(tag, message)
    }

    /**
     * Priority constant for the println method; use Log.v.
     */
    fun v(tag: String, message: String) {
        Log.v(tag, message)
    }

    /**
     * Priority constant for the println method; use Log.w.
     */
    fun w(tag: String, message: String) {
        Log.w(tag, message)
    }

    /**
     * What a Terrible Failure: Report a condition that should never happen.
     */
    fun wtf(tag: String, message: String) {
        Log.wtf(tag, message)
    }
}