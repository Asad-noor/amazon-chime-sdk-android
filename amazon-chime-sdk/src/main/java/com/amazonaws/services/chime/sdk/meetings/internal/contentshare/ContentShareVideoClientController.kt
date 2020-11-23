/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *  SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.contentshare

import com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare.ContentShareObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSource

interface ContentShareVideoClientController {
    fun startVideoSharing(videoSource: VideoSource)

    fun stopVideoSharing()

    fun subscribeToVideoClientStateChange(observer: ContentShareObserver)

    fun unsubscribeFromVideoClientStateChange(observer: ContentShareObserver)
}
