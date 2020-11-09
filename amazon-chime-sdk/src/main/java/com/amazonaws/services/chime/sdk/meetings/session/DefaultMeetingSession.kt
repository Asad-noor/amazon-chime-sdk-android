/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.session

import android.content.Context
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoFacade
import com.amazonaws.services.chime.sdk.meetings.audiovideo.DefaultAudioVideoController
import com.amazonaws.services.chime.sdk.meetings.audiovideo.DefaultAudioVideoFacade
import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.activespeakerdetector.DefaultActiveSpeakerDetector
import com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare.DefaultContentShareController
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.DefaultVideoTileController
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.DefaultVideoTileFactory
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.DefaultEglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.device.DefaultDeviceController
import com.amazonaws.services.chime.sdk.meetings.internal.audio.AudioClientFactory
import com.amazonaws.services.chime.sdk.meetings.internal.audio.DefaultAudioClientController
import com.amazonaws.services.chime.sdk.meetings.internal.audio.DefaultAudioClientObserver
import com.amazonaws.services.chime.sdk.meetings.internal.metric.DefaultClientMetricsCollector
import com.amazonaws.services.chime.sdk.meetings.internal.video.DefaultVideoClientController
import com.amazonaws.services.chime.sdk.meetings.internal.video.DefaultVideoClientFactory
import com.amazonaws.services.chime.sdk.meetings.internal.video.DefaultVideoClientObserver
import com.amazonaws.services.chime.sdk.meetings.internal.video.DefaultVideoClientStateController
import com.amazonaws.services.chime.sdk.meetings.internal.video.TURNRequestParams
import com.amazonaws.services.chime.sdk.meetings.realtime.DefaultRealtimeController
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger

class DefaultMeetingSession(
    override val configuration: MeetingSessionConfiguration,
    override val logger: Logger,
    context: Context,
    eglCoreFactory: EglCoreFactory = DefaultEglCoreFactory()
) : MeetingSession {

    override val audioVideo: AudioVideoFacade

    init {
        val metricsCollector =
            DefaultClientMetricsCollector()
        val audioClientObserver =
            DefaultAudioClientObserver(
                logger,
                metricsCollector,
                configuration
            )

        val audioClient =
            AudioClientFactory.getAudioClient(context, audioClientObserver)

        audioClientObserver.audioClient = audioClient

        val audioClientController =
            DefaultAudioClientController(
                context,
                logger,
                audioClientObserver,
                audioClient
            )

        val turnRequestParams =
            TURNRequestParams(
                configuration.meetingId,
                configuration.urls.signalingURL,
                configuration.urls.turnControlURL,
                configuration.credentials.joinToken
            )

        val videoClientStateController =
            DefaultVideoClientStateController(
                logger
            )
        val videoClientObserver =
            DefaultVideoClientObserver(
                logger,
                turnRequestParams,
                metricsCollector,
                videoClientStateController,
                configuration.urls.urlRewriter
            )

        val videoClientFactory = DefaultVideoClientFactory()

        val videoClientController =
            DefaultVideoClientController(
                context,
                logger,
                videoClientStateController,
                videoClientObserver,
                configuration,
                videoClientFactory,
                eglCoreFactory
            )

        val videoTileFactory = DefaultVideoTileFactory(logger)

        val videoTileController =
            DefaultVideoTileController(
                logger,
                videoClientController,
                videoTileFactory,
                eglCoreFactory
            )
        videoClientObserver.subscribeToVideoTileChange(videoTileController)

        val deviceController =
            DefaultDeviceController(
                context,
                audioClientController,
                videoClientController
            )

        val realtimeController =
            DefaultRealtimeController(
                audioClientController,
                audioClientObserver,
                videoClientController,
                videoClientObserver
            )

        val activeSpeakerDetector = DefaultActiveSpeakerDetector(audioClientObserver)

        val audioVideoController =
            DefaultAudioVideoController(
                audioClientController,
                audioClientObserver,
                metricsCollector,
                configuration,
                videoClientController,
                videoClientObserver
            )

        val contentShareConfiguration =
            DefaultContentShareController.createContentShareMeetingSessionConfiguration(configuration)

        val contentShareTurnRequestParams =
            TURNRequestParams(
                contentShareConfiguration.meetingId,
                contentShareConfiguration.urls.signalingURL,
                contentShareConfiguration.urls.turnControlURL,
                contentShareConfiguration.credentials.joinToken
            )

        val contentShareVideoClientStateController =
            DefaultVideoClientStateController(
                logger
            )

        val contentShareObserver =
            DefaultVideoClientObserver(
                logger,
                contentShareTurnRequestParams,
                metricsCollector,
                contentShareVideoClientStateController,
                contentShareConfiguration.urls.urlRewriter
            )

        val contentShareController =
            DefaultContentShareController(
                context,
                logger,
                contentShareVideoClientStateController,
                contentShareObserver,
                contentShareConfiguration,
                videoClientFactory,
                eglCoreFactory
            )

        audioVideo = DefaultAudioVideoFacade(
            context,
            audioVideoController,
            realtimeController,
            deviceController,
            videoTileController,
            activeSpeakerDetector,
            contentShareController
        )
    }
}
