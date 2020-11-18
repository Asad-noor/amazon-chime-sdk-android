/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare

import android.content.Context
import com.amazonaws.services.chime.sdk.BuildConfig
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCore
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.internal.utils.ObserverUtils
import com.amazonaws.services.chime.sdk.meetings.internal.video.VideoClientFactory
import com.amazonaws.services.chime.sdk.meetings.internal.video.VideoClientLifecycleHandler
import com.amazonaws.services.chime.sdk.meetings.internal.video.VideoClientObserver
import com.amazonaws.services.chime.sdk.meetings.internal.video.VideoClientStateController
import com.amazonaws.services.chime.sdk.meetings.internal.video.VideoSourceAdapter
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionConfiguration
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionCredentials
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatus
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatusCode
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import com.xodee.client.video.VideoClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DefaultContentShareController(
    private val context: Context,
    private val logger: Logger,
    private val videoClientStateController: VideoClientStateController,
    private val videoClientObserver: VideoClientObserver,
    private val configuration: MeetingSessionConfiguration,
    private val videoClientFactory: VideoClientFactory,
    private val eglCoreFactory: EglCoreFactory
) : ContentShareController,
    VideoClientLifecycleHandler,
    AudioVideoObserver {
    private val defaultScope = CoroutineScope(Dispatchers.Default)
    private val observers = mutableSetOf<ContentShareObserver>()
    private val videoSourceAdapter = VideoSourceAdapter()
    private var videoClient: VideoClient? = null
    private var eglCore: EglCore? = null
    private var isCurrentSharing = false

    private val TAG = "DefaultContentShareController"

    private val VIDEO_CLIENT_FLAG_ENABLE_USE_HW_DECODE_AND_RENDER = 1 shl 6
    private val VIDEO_CLIENT_FLAG_ENABLE_TWO_SIMULCAST_STREAMS = 1 shl 12
    private val VIDEO_CLIENT_FLAG_DISABLE_CAPTURER = 1 shl 20

    companion object {
        const val CONTENT_MODALITY = "#content"

        fun createContentShareMeetingSessionConfiguration(
            configuration: MeetingSessionConfiguration
        ): MeetingSessionConfiguration {
            return MeetingSessionConfiguration(
                configuration.meetingId,
                MeetingSessionCredentials(
                    configuration.credentials.attendeeId,
                    configuration.credentials.externalUserId,
                    configuration.credentials.joinToken + CONTENT_MODALITY
                ),
                configuration.urls
            )
        }
    }

    init {
        videoClientStateController.bindLifecycleHandler(this)
        videoClientObserver.subscribeToVideoClientStateChange(this)
    }

    override fun startContentShare(source: ContentShareSource) {
        defaultScope.launch {
            // Stop sharing the current source
            if (isCurrentSharing) {
                stopContentShare()
            }

            // Start the given content share source
            source.videoSource?.let { videoSource ->

                if (eglCore == null) {
                    logger.debug(TAG, "Creating EGL core")
                    eglCore = eglCoreFactory.createEglCore()
                }

                videoClientStateController.start()

                logger.info(TAG, "Setting external video source to content share source")
                videoSourceAdapter.source = videoSource
                videoClient?.setExternalVideoSource(videoSourceAdapter, eglCore?.eglContext)
                logger.info(TAG, "Setting sending to true")
                videoClient?.setSending(true)
                ObserverUtils.notifyObserverOnMainThread(observers) {
                    it.onContentShareStarted()
                }
                isCurrentSharing = true
            }
        }
    }

    override fun initializeVideoClient() {
        logger.info(TAG, "Initializing video client")
        initializeAppDetailedInfo()
        VideoClient.initializeGlobals(context)
        videoClient = videoClientFactory.getVideoClient(videoClientObserver)
    }

    override fun startVideoClient() {
        // Content share is send only
        videoClient?.setReceiving(false)
        logger.info(TAG, "Starting video client for content share")
        var flag = 0
        flag = flag or VIDEO_CLIENT_FLAG_ENABLE_USE_HW_DECODE_AND_RENDER
        flag = flag or VIDEO_CLIENT_FLAG_ENABLE_TWO_SIMULCAST_STREAMS
        flag = flag or VIDEO_CLIENT_FLAG_DISABLE_CAPTURER
        val result = videoClient?.startServiceV3(
            "",
            "",
            configuration.meetingId,
            configuration.credentials.joinToken,
            false,
            0,
            flag,
            eglCore?.eglContext
        )
        logger.info(TAG, "Video client start result: $result")
    }

    override fun stopVideoClient() {
        logger.info(TAG, "Stopping video client")
        videoClient?.stopService()
    }

    override fun destroyVideoClient() {
        logger.info(TAG, "Destroying video client")
        videoClient?.destroy()
        videoClient = null
    }

    private fun initializeAppDetailedInfo() {
        val manufacturer = android.os.Build.MANUFACTURER
        val model = android.os.Build.MODEL
        val osVersion = android.os.Build.VERSION.RELEASE
        val packageName = context.packageName
        val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
        val appVer = packageInfo.versionName
        val appCode = packageInfo.versionCode.toString()
        val clientSource = "amazon-chime-sdk"
        val sdkVersion = BuildConfig.VERSION_NAME

        VideoClient.AppDetailedInfo.initialize(
            String.format("Android %s", appVer),
            appCode,
            model,
            manufacturer,
            osVersion,
            clientSource,
            sdkVersion
        )
    }

    override fun stopContentShare() {
        videoClientStateController.stop()

        isCurrentSharing = false
        eglCore?.release()
        eglCore = null
    }

    override fun addContentShareObserver(observer: ContentShareObserver) {
        observers.add(observer)
    }

    override fun removeContentShareObserver(observer: ContentShareObserver) {
        observers.remove(observer)
    }

    override fun onAudioSessionStartedConnecting(reconnecting: Boolean) {
        // No implementation
    }

    override fun onAudioSessionStarted(reconnecting: Boolean) {
        // No implementation
    }

    override fun onAudioSessionDropped() {
        // No implementation
    }

    override fun onAudioSessionStopped(sessionStatus: MeetingSessionStatus) {
        // No implementation
    }

    override fun onAudioSessionCancelledReconnect() {
        // No implementation
    }

    override fun onConnectionRecovered() {
        // No implementation
    }

    override fun onConnectionBecamePoor() {
        // No implementation
    }

    override fun onVideoSessionStartedConnecting() {
        logger.debug(TAG, "Content video session started connecting.")
    }

    override fun onVideoSessionStarted(sessionStatus: MeetingSessionStatus) {
        logger.debug(TAG, "Content share video session started.")
    }

    override fun onVideoSessionStopped(sessionStatus: MeetingSessionStatus) {
        logger.debug(TAG, "Content share video session stopped.")
        if (sessionStatus.statusCode == MeetingSessionStatusCode.OK) {
            ObserverUtils.notifyObserverOnMainThread(observers) {
                it.onContentShareStopped(ContentShareStatus(ContentShareStatusCode.OK))
            }
        } else {
            logger.error(TAG, "Content share stopped by video session status: $sessionStatus")
            ObserverUtils.notifyObserverOnMainThread(observers) {
                it.onContentShareStopped(ContentShareStatus(ContentShareStatusCode.VideoServiceFailed))
            }
        }
    }
}
