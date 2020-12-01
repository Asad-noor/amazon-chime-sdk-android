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
    videoCaptureSource: DefaultScreenCaptureSource,
    private val context: Context
) : ContentShareSource() {
    override var videoSource: VideoSource? = videoCaptureSource

    fun start() {
        videoSource?.let {
            (it as DefaultScreenCaptureSource).start()
        }
    }

    fun stop() {
        videoSource?.let {
            context.stopService(Intent(context, ScreenCaptureService::class.java))
            (it as DefaultScreenCaptureSource).stop()
        }
    }

    fun release() {
        videoSource?.let {
            (it as DefaultScreenCaptureSource).release()
            videoSource = null
        }
    }

    fun addObserver(observer: CaptureSourceObserver) {
        videoSource?.let {
            (it as DefaultScreenCaptureSource).addCaptureSourceObserver(observer)
        }
    }

    fun removeObserver(observer: CaptureSourceObserver) {
        videoSource?.let {
            (it as DefaultScreenCaptureSource).removeCaptureSourceObserver(observer)
        }
    }
}
