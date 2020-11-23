/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.video

import com.xodee.client.video.VideoClient
import com.xodee.client.video.VideoClientDelegate

class DefaultVideoClientFactory : VideoClientFactory {
    override fun getVideoClient(videoClientObserver: VideoClientObserver): VideoClient {
        return VideoClient(videoClientObserver, videoClientObserver, videoClientObserver)
    }

    override fun getVideoClient(videoClientDelegate: VideoClientDelegate): VideoClient {
        return VideoClient(videoClientDelegate, null, null)
    }
}
