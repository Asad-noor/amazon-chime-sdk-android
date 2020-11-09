/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.utils

/**
 * The Modality type is a backwards-compatible extension of the
 * profile id (UUID string) and session token schemas (base 64 string).
 * It appends #<modality> to the string, which indicates the modality
 * of the participant.
 */
interface Modality {
    /**
     * The participant Id
     */
    fun id(): String

    /**
     * The base of the Id
     */
    fun base(): String

    /**
     * The modality of the Id
     */
    fun modality(): String

    /**
     * Check whether the current Id contains the input modality
     */
    fun hasModality(modality: String): Boolean
}
