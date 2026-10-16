package com.example.tvremote

import android.content.Context
import android.media.AudioManager
import org.json.JSONObject

/** Maps a JSON command from the phone to an action on the TV. Returns true if it worked. */
object RemoteController {

    fun handle(cmd: JSONObject, context: Context): Boolean {
        val service = RemoteAccessibilityService.instance
        return when (cmd.optString("type")) {
            "volume" -> adjustVolume(context, cmd.optString("dir"))
            "text" -> service?.typeText(cmd.optString("value"), cmd.optBoolean("append", false)) ?: false
            "key" -> service?.globalAction(cmd.optString("name")) ?: false
            "dpad" -> service?.dpad(cmd.optString("dir")) ?: false
            "tap" -> service?.tapCenter() ?: false
            "swipe" -> service?.swipe(cmd.optString("dir"), cmd.optInt("distance", 300)) ?: false
            else -> false
        }
    }

    private fun adjustVolume(context: Context, dir: String): Boolean {
        val am = context.getSystemService(AudioManager::class.java) ?: return false
        when (dir) {
            "up" -> am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            "down" -> am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            "mute" -> am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI)
            else -> return false
        }
        return true
    }
}
