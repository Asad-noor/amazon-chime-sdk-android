/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare

import com.amazonaws.services.chime.sdk.meetings.internal.contentshare.ContentShareVideoClientController
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionConfiguration
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionCredentials
import com.amazonaws.services.chime.sdk.meetings.utils.DefaultModality
import com.amazonaws.services.chime.sdk.meetings.utils.ModalityType
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger

class DefaultContentShareController(
    private val logger: Logger,
    private val contentShareVideoClientController: ContentShareVideoClientController
) : ContentShareController {

    private val TAG = "DefaultContentShareController"

    companion object {
        fun createContentShareMeetingSessionConfiguration(
            configuration: MeetingSessionConfiguration
        ): MeetingSessionConfiguration {
            val contentModality = DefaultModality.MODALITY_SEPARATOR + ModalityType.Content
            return MeetingSessionConfiguration(
                configuration.meetingId,
                MeetingSessionCredentials(
                    configuration.credentials.attendeeId + contentModality,
                    configuration.credentials.externalUserId,
                    configuration.credentials.joinToken + contentModality
                ),
                configuration.urls
            )
        }
    }

    override fun startContentShare(source: ContentShareSource) {
        source.videoSource?.let {
            contentShareVideoClientController.startVideoSharing(it)
        }
    }

    override fun stopContentShare() {
        contentShareVideoClientController.stopVideoSharing()
    }

    override fun addContentShareObserver(observer: ContentShareObserver) {
        contentShareVideoClientController.subscribeToVideoClientStateChange(observer)
    }

    override fun removeContentShareObserver(observer: ContentShareObserver) {
        contentShareVideoClientController.unsubscribeFromVideoClientStateChange(observer)
    }
}
