/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.utils

class DefaultModality(private val id: String) : Modality {

    companion object {
        const val MODALITY_SEPARATOR = "#"
        const val MODALITY_CONTENT = "content"
    }

    override fun id(): String {
        return id
    }

    override fun base(): String {
        if (id.isEmpty()) {
            return ""
        }
        return id.split(MODALITY_SEPARATOR)[0]
    }

    override fun modality(): String {
        if (id.isEmpty()) {
            return ""
        }
        val components = id.split(MODALITY_SEPARATOR)
        return if (components.size == 2) components[1] else ""
    }

    override fun hasModality(modality: String): Boolean {
        return modality.isNotEmpty() && modality() == modality
    }
}
