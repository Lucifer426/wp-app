package com.watchparty.app

import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONArray
import org.json.JSONObject

object Net {
    var socket: Socket? = null
    var isHost = false
    var inRoom = false
    var joinedInfo: JSONObject? = null
    var lastCount = 1
    var history: JSONArray? = null

    fun create(): Socket {
        socket?.disconnect()
        inRoom = false
        lastCount = 1
        history = null
        val s = IO.socket("http://86.54.42.247")
        socket = s
        return s
    }
}
