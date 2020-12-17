package com.djcrontab.code.controlsurface

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket


class BitwigConnection(
    val sendToBitwig: Channel<String>,
    val receiveFromBitwig: Channel<String>,
    val host: String,
    val port: Int
) {
    private var clientSocket: Socket? = null
    private var outputStream: OutputStream? = null

    fun makeByteArray(s: String): ByteArray {
        val asByteArray = s.toByteArray()
        val header = asByteArray.let {
            val byteArray = ByteArray(4)
            var size = it.size
            for (i in 3 downTo 0) {
                byteArray[i] = (size and 0xff).toByte()
                size /= 256
            }
            byteArray
        }
        return header + asByteArray
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun reader() {
        val stream = clientSocket!!.getInputStream()
        val reader = stream.bufferedReader()
        while (true) {
            val line = reader.readLine()
            if (line != null) {
                receiveFromBitwig.send(line)
            } else {
                try {
                    clientSocket!!.close()
                } catch (_: IOException) {

                }
                break
            }

        }
    }

    suspend fun writeloop() {
        while (true) try {
            out(sendToBitwig.receive())
        } catch (e: IOException) {
            print("Not connected... retrying")
            delay(1000)
        }
    }

    fun out(s: String) {
        outputStream?.apply {
            write(makeByteArray(s))
            return
        }
        Log.v("ControlSurface", "socket closed, ignored")
    }

    init {
        val readFromRemote = Thread {
            while (true) {
                try {
                    Log.v("ControlSurface", "connecting.. $host $port")
                    clientSocket = Socket()
                    clientSocket!!.connect(InetSocketAddress(host, port))
                    outputStream = clientSocket!!.getOutputStream()

                    runBlocking { reader() }
                } catch (_: IOException) {

                }
                Log.v("ControlSurface", "not connected, retrying")
                Thread.sleep(1000)
            }
        }
        readFromRemote.start()

        val writeToRemote = Thread {
            runBlocking { writeloop() }
        }

        writeToRemote.start()
    }
}