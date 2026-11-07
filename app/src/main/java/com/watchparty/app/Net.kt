package com.watchparty.app

import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

object Net {
    var socket: Socket? = null
    var isHost = false
    var joinedInfo: JSONObject? = null

    fun create(): Socket {
        socket?.disconnect()
        val s = IO.socket("http://86.54.42.247")
        socket = s
        return s
    }
}
