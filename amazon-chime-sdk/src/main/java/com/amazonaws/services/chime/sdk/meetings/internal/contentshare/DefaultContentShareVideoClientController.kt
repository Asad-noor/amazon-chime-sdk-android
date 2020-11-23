/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *  SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.contentshare

import android.content.Context
import com.amazonaws.services.chime.sdk.BuildConfig
import com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare.ContentShareObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCore
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.internal.utils.ObserverUtils
import com.amazonaws.services.chime.sdk.meetings.internal.video.VideoClientFactory
import com.amazonaws.services.chime.sdk.meetings.internal.video.VideoSourceAdapter
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionConfiguration
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import com.xodee.client.video.VideoClient

class DefaultContentShareVideoClientController(
    private val context: Context,
    private val logger: Logger,
    private val contentShareVideoClientObserver: ContentShareVideoClientObserver,
    private val configuration: MeetingSessionConfiguration,
    private val videoClientFactory: VideoClientFactory,
    private val eglCoreFactory: EglCoreFactory
) : ContentShareVideoClientController {
    private val observers = mutableSetOf<ContentShareObserver>()
    private val videoSourceAdapter = VideoSourceAdapter()
    private var videoClient: VideoClient? = null
    private var eglCore: EglCore? = null
    private var isSharing = false

    private val TAG = "DefaultContentShareVideoClientController"

    private val VIDEO_CLIENT_FLAG_ENABLE_USE_HW_DECODE_AND_RENDER = 1 shl 6
    private val VIDEO_CLIENT_FLAG_ENABLE_TWO_SIMULCAST_STREAMS = 1 shl 12
    private val VIDEO_CLIENT_FLAG_DISABLE_CAPTURER = 1 shl 20

    override fun startVideoSharing(videoSource: VideoSource) {
        // Stop sharing the current source
        if (isSharing) {
            stopVideoSharing()
        }

        // Start the given content share source
        if (eglCore == null) {
            logger.debug(TAG, "Creating EGL core")
            eglCore = eglCoreFactory.createEglCore()
        }

        if (videoClient == null) {
            initializeVideoClient()
        }
        startVideoClient()

        logger.debug(TAG, "Setting external video source to content share source")
        videoSourceAdapter.source = videoSource
        videoClient?.setExternalVideoSource(videoSourceAdapter, eglCore?.eglContext)
        logger.debug(TAG, "Setting sending to true")
        videoClient?.setSending(true)
        ObserverUtils.notifyObserverOnMainThread(observers) {
            it.onContentShareStarted()
        }
        isSharing = true
    }

    private fun initializeVideoClient() {
        logger.info(TAG, "Initializing video client")
        initializeAppDetailedInfo()
        // Thread safe operation
        VideoClient.initializeGlobals(context)
        videoClient = videoClientFactory.getVideoClient(contentShareVideoClientObserver)
        // Content share is send only
        videoClient?.setReceiving(false)
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

    private fun startVideoClient() {
        logger.info(TAG, "Starting content share video client for content share")
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
        logger.info(TAG, "Content share video client start result: $result")
    }

    override fun stopVideoSharing() {
        logger.info(TAG, "Stopping content share video client")
        videoClient?.stopService()
        videoClient?.destroy()
        videoClient = null

        isSharing = false
        eglCore?.release()
        eglCore = null
    }

    override fun subscribeToVideoClientStateChange(observer: ContentShareObserver) {
        observers.add(observer)
        contentShareVideoClientObserver.subscribeToVideoClientStateChange(observer)
    }

    override fun unsubscribeFromVideoClientStateChange(observer: ContentShareObserver) {
        observers.remove(observer)
        contentShareVideoClientObserver.unsubscribeFromVideoClientStateChange(observer)
    }
}
