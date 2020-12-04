/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *  SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.contentshare

import com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare.ContentShareObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare.ContentShareStatus
import com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare.ContentShareStatusCode
import com.amazonaws.services.chime.sdk.meetings.internal.utils.ObserverUtils
import com.amazonaws.services.chime.sdk.meetings.internal.utils.TURNRequestUtils
import com.amazonaws.services.chime.sdk.meetings.internal.video.TURNCredentials
import com.amazonaws.services.chime.sdk.meetings.internal.video.TURNRequestParams
import com.amazonaws.services.chime.sdk.meetings.session.URLRewriter
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import com.xodee.client.video.VideoClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DefaultContentShareVideoClientObserver(
    private val logger: Logger,
    private val turnRequestParams: TURNRequestParams,
    private val urlRewriter: URLRewriter
) : ContentShareVideoClientObserver {
    private val TAG = "DefaultContentShareVideoClientObserver"

    private val contentShareObservers = mutableSetOf<ContentShareObserver>()

    private val uiScope = CoroutineScope(Dispatchers.Main)

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
        ObserverUtils.notifyObserverOnMainThread(contentShareObservers) {
            it.onContentShareStarted()
        }
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
            val turnResponse: TURNCredentials? = TURNRequestUtils.doTurnRequest(turnRequestParams, logger)
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
