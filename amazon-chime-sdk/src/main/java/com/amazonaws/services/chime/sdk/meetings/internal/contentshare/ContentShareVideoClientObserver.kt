/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *  SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.contentshare

import com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare.ContentShareObserver
import com.xodee.client.video.VideoClientDelegate

interface ContentShareVideoClientObserver : VideoClientDelegate {
    fun subscribeToVideoClientStateChange(observer: ContentShareObserver)

    fun unsubscribeFromVideoClientStateChange(observer: ContentShareObserver)
}
