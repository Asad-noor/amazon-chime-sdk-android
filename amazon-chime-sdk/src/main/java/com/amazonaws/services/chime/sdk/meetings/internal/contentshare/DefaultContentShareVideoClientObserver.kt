/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *  SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.contentshare

import com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare.ContentShareObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare.ContentShareStatus
import com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare.ContentShareStatusCode
import com.amazonaws.services.chime.sdk.meetings.internal.utils.ObserverUtils
import com.amazonaws.services.chime.sdk.meetings.internal.video.TURNCredentials
import com.amazonaws.services.chime.sdk.meetings.internal.video.TURNRequestParams
import com.amazonaws.services.chime.sdk.meetings.session.URLRewriter
import com.amazonaws.services.chime.sdk.meetings.utils.DefaultModality
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import com.xodee.client.video.VideoClient
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class DefaultContentShareVideoClientObserver(
    private val logger: Logger,
    private val turnRequestParams: TURNRequestParams,
    private val urlRewriter: URLRewriter
) : ContentShareVideoClientObserver {
    private val TAG = "DefaultContentShareVideoClientObserver"
    private val TOKEN_HEADER = "X-Chime-Auth-Token"
    private val SYSPROP_USER_AGENT = "http.agent"
    private val USER_AGENT_HEADER = "User-Agent"
    private val CONTENT_TYPE_HEADER = "Content-Type"
    private val CONTENT_TYPE = "application/json"
    private val MEETING_ID_KEY = "meetingId"
    private val TOKEN_KEY = "_aws_wt_session"

    private val contentShareObservers = mutableSetOf<ContentShareObserver>()

    private val uiScope = CoroutineScope(Dispatchers.Main)
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    override fun subscribeToVideoClientStateChange(observer: ContentShareObserver) {
        contentShareObservers.add(observer)
    }

    override fun unsubscribeFromVideoClientStateChange(observer: ContentShareObserver) {
        contentShareObservers.remove(observer)
    }

    override fun isConnecting(client: VideoClient?) {
        logger.debug(TAG, "content share video client is connecting")
    }

    override fun didConnect(client: VideoClient?, controlStatus: Int) {
        logger.debug(TAG, "content share video client is connected")
    }

    override fun didFail(client: VideoClient?, status: Int, controlStatus: Int) {
        logger.info(TAG, "content share video client is failed with $controlStatus")
        ObserverUtils.notifyObserverOnMainThread(contentShareObservers) {
            it.onContentShareStopped(
                ContentShareStatus(
                    ContentShareStatusCode.VideoServiceFailed
                )
            )
        }
    }

    override fun didStop(controlStatus: VideoClient?) {
        logger.info(TAG, "content share video client is stopped")
        ObserverUtils.notifyObserverOnMainThread(contentShareObservers) {
            it.onContentShareStopped(
                ContentShareStatus(
                    ContentShareStatusCode.OK
                )
            )
        }
    }

    override fun requestTurnCreds(client: VideoClient?) {
        logger.info(TAG, "requestTurnCreds")
        uiScope.launch {
            val turnResponse: TURNCredentials? = doTurnRequest()
            with(turnResponse) {
                val isActive = client?.isActive ?: false
                if (this != null && isActive) {
                    val newUris = uris.map { url ->
                        url?.let {
                            urlRewriter(it)
                        }
                    }.toTypedArray()
                    client?.updateTurnCredentials(
                        username,
                        password,
                        ttl,
                        newUris,
                        turnRequestParams.signalingUrl,
                        VideoClient.VideoClientTurnStatus.VIDEO_CLIENT_TURN_FEATURE_ON
                    )
                } else {
                    client?.updateTurnCredentials(
                        null,
                        null,
                        null,
                        null,
                        null,
                        VideoClient.VideoClientTurnStatus.VIDEO_CLIENT_TURN_STATUS_CCP_FAILURE
                    )
                }
            }
        }
    }

    private suspend fun doTurnRequest(): TURNCredentials? {
        return withContext(ioDispatcher) {
            try {
                val response = StringBuffer()
                logger.info(TAG, "Making TURN Request")
                with(URL(turnRequestParams.turnControlUrl).openConnection() as HttpURLConnection) {
                    requestMethod = "POST"
                    doInput = true
                    doOutput = true
                    addRequestProperty(TOKEN_HEADER, "$TOKEN_KEY=${DefaultModality(turnRequestParams.joinToken).base()}")
                    setRequestProperty(CONTENT_TYPE_HEADER, CONTENT_TYPE)
                    val user_agent = System.getProperty(SYSPROP_USER_AGENT)
                    logger.info(TAG, "User Agent while doing TURN request is $user_agent")
                    setRequestProperty(USER_AGENT_HEADER, user_agent)
                    val out = BufferedWriter(OutputStreamWriter(outputStream))
                    out.write(
                        JSONObject().put(
                            MEETING_ID_KEY,
                            turnRequestParams.meetingId
                        ).toString()
                    )
                    out.flush()
                    out.close()
                    BufferedReader(InputStreamReader(inputStream)).use {
                        var inputLine = it.readLine()
                        while (inputLine != null) {
                            response.append(inputLine)
                            inputLine = it.readLine()
                        }
                        it.close()
                    }
                    if (responseCode == 200) {
                        logger.info(TAG, "TURN Request Success")
                        var responseObject = JSONObject(response.toString())
                        val jsonArray =
                            responseObject.getJSONArray(TURNCredentials.TURN_CREDENTIALS_RESULT_URIS)
                        val uris = arrayOfNulls<String>(jsonArray.length())
                        for (i in 0 until jsonArray.length()) {
                            uris[i] = jsonArray.getString(i)
                        }
                        TURNCredentials(
                            responseObject.getString(TURNCredentials.TURN_CREDENTIALS_RESULT_USERNAME),
                            responseObject.getString(TURNCredentials.TURN_CREDENTIALS_RESULT_PASSWORD),
                            responseObject.getString(TURNCredentials.TURN_CREDENTIALS_RESULT_TTL),
                            uris
                        )
                    } else {
                        logger.error(
                            TAG,
                            "TURN Request got error with Response code: $responseCode"
                        )
                        null
                    }
                }
            } catch (exception: Exception) {
                logger.error(TAG, "Exception while doing TURN Request: $exception")
                null
            }
        }
    }

    override fun getAvailableDnsServers(): Array<String> {
        return emptyArray()
    }

    override fun onCameraChanged() {
        // No implementation
    }

    override fun onMetrics(metrics: IntArray?, values: DoubleArray?) {
        // No implementation
    }

    override fun cameraSendIsAvailable(p0: VideoClient?, p1: Boolean) {
        // No implementation
    }

    override fun didReceiveFrame(
        client: VideoClient?,
        frame: Any?,
        profileId: String?,
        displayId: Int,
        pauseType: Int,
        videoId: Int
    ) {
        // No implementation
    }

    override fun pauseRemoteVideo(client: VideoClient?, display_id: Int, pause: Boolean) {
        // No implementation
    }
}
