/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *  SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare

/**
 * [ContentShareStatusCode] indicates the reason the content share event occurred.
 */
enum class ContentShareStatusCode {
    /**
     * Everything is OK so far.
     */
    OK,

    /**
     * This can happen when the content share video connection is in an unrecoverable failed state.
     */
    VideoServiceFailed;

    val isFailed: Boolean
        get() = this != OK

    // check video client callbacks
    val isTerminal: Boolean
        get() = this == VideoServiceFailed
}
