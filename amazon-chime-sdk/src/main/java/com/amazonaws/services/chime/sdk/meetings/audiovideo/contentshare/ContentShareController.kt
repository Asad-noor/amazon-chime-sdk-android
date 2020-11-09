/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *  SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare

/**
 * [ContentShareController] exposes methods for starting and stopping content share with a source.
 */
interface ContentShareController {
    /**
     * Start sharing the content of a given [ContentShareSource].
     * Once sharing has started successfully, [ContentShareObserver.onContentShareStarted] will
     * be notified. If sharing fails or stops, [ContentShareObserver.onContentShareStopped]
     * will be notified with [ContentShareStatus] as the cause.
     * Repeatedly calling this API will stop previous content share source if applicable and
     * start the given source.
     * @param source: [ContentShareSource] - source of content to be shared
     */
    fun startContentShare(source: ContentShareSource)

    /**
     * Stop sharing the content of a [ContentShareSource] that previously started.
     * Once the sharing stops successfully, [ContentShareObserver.onContentShareStopped]
     * will be notified with status code [ContentShareStatusCode.OK].
     */
    fun stopContentShare()

    /**
     * Subscribe the given observer to content share events (sharing started and stopped).
     * @param observer: [ContentShareObserver] - observer to be notified for events
     */
    fun addContentShareObserver(observer: ContentShareObserver)

    /**
     * Unsubscribe the given observer from content share events.
     * @param observer: [ContentShareObserver] - observer has been subscribed for events
     */
    fun removeContentShareObserver(observer: ContentShareObserver)
}
