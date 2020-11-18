/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.view.Display
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoContentHint
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrame
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSink
import com.amazonaws.services.chime.sdk.meetings.internal.utils.ObserverUtils
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking

class DefaultScreenCaptureSource(
    private val context: Context,
    private val logger: Logger,
    private val surfaceTextureCaptureSourceFactory: SurfaceTextureCaptureSourceFactory,
    private val mediaProjection: MediaProjection,
    private val displayManager: DisplayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
) : VideoCaptureSource, VideoSink {
    private lateinit var displayMetrics: DisplayMetrics
    private var virtualDisplay: VirtualDisplay? = null

    // This source provides a surface we pass into the system APIs
    // and then starts emitting frames once the system starts drawing to the
    // surface. To speed up restart, since theses sources have to wait on
    // in-flight frames to finish release, we just begin the release and
    // create a new one
    private var surfaceTextureSource: SurfaceTextureCaptureSource? = null

    private val handler: Handler

    private val observers = mutableSetOf<CaptureSourceObserver>()
    private val sinks = mutableSetOf<VideoSink>()

    override val contentHint = VideoContentHint.Text

    private val TAG = "DefaultScreenCaptureSource"

    init {
        val thread = HandlerThread("DefaultScreenCaptureSource")
        thread.start()
        handler = Handler(thread.looper)
    }

    override fun start() {
        logger.info(TAG, "Starting screen capture source")

        displayMetrics = context.resources.displayMetrics
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY) ?: throw RuntimeException("No display found.")
        display.getMetrics(displayMetrics)

        surfaceTextureSource =
            surfaceTextureCaptureSourceFactory.createSurfaceTextureCaptureSource(
                displayMetrics.widthPixels,
                displayMetrics.heightPixels,
                contentHint
            )

        surfaceTextureSource?.addVideoSink(this)
        surfaceTextureSource?.start()

        virtualDisplay = mediaProjection.createVirtualDisplay(
            TAG,
            displayMetrics.widthPixels,
            displayMetrics.heightPixels,
            displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            surfaceTextureSource?.surface,
            object : VirtualDisplay.Callback() {
                override fun onStopped() {
                    ObserverUtils.notifyObserverOnMainThread(observers) {
                        it.onCaptureStopped()
                    }
                }
            },
            handler
        )

        logger.info(TAG, "Media projection adapter activity succeeded, virtual display created")
        ObserverUtils.notifyObserverOnMainThread(observers) {
            it.onCaptureStarted()
        }
    }

    override fun stop() {
        logger.info(TAG, "Stopping screen capture source")
        val sink: VideoSink = this
        runBlocking(handler.asCoroutineDispatcher().immediate) {
            mediaProjection.stop()
            virtualDisplay?.release()
            virtualDisplay = null

            surfaceTextureSource?.removeVideoSink(sink)
            surfaceTextureSource?.stop()
            surfaceTextureSource?.release()
            surfaceTextureSource = null
        }
    }

    override fun addCaptureSourceObserver(observer: CaptureSourceObserver) {
        observers.add(observer)
    }

    override fun removeCaptureSourceObserver(observer: CaptureSourceObserver) {
        observers.remove(observer)
    }

    override fun addVideoSink(sink: VideoSink) {
        handler.post { sinks.add(sink) }
    }

    override fun removeVideoSink(sink: VideoSink) {
        runBlocking(handler.asCoroutineDispatcher().immediate) {
            sinks.remove(sink)
        }
    }

    override fun onVideoFrameReceived(frame: VideoFrame) {
        sinks.forEach { it.onVideoFrameReceived(frame) }
    }

    fun release() {
        runBlocking(handler.asCoroutineDispatcher().immediate) {
            logger.info(TAG, "Stopping handler looper")
            handler.removeCallbacksAndMessages(null)
            handler.looper.quit()
        }
    }
}
