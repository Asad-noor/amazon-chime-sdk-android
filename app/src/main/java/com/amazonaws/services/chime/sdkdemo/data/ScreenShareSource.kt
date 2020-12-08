/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.data

import android.content.Context
import android.content.Intent
import com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare.ContentShareSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.CaptureSourceObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.DefaultScreenCaptureSource
import com.amazonaws.services.chime.sdkdemo.service.ScreenCaptureService

class ScreenShareSource(
    private val screenCaptureSource: DefaultScreenCaptureSource,
    private val context: Context
) : ContentShareSource() {
    override var videoSource: VideoSource? = screenCaptureSource

    fun start() {
        screenCaptureSource.start()
    }

    fun stop() {
        context.stopService(Intent(context, ScreenCaptureService::class.java))
        screenCaptureSource.stop()
    }

    fun release() {
        screenCaptureSource.release()
    }

    fun addObserver(observer: CaptureSourceObserver) {
        screenCaptureSource.addCaptureSourceObserver(observer)
    }

    fun removeObserver(observer: CaptureSourceObserver) {
        screenCaptureSource.removeCaptureSourceObserver(observer)
    }
}
