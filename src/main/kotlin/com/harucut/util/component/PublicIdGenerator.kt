package com.harucut.util.component

import com.aventrix.jnanoid.jnanoid.NanoIdUtils

fun generatePublicId(): String =
    NanoIdUtils.randomNanoId(NanoIdUtils.DEFAULT_NUMBER_GENERATOR, NanoIdUtils.DEFAULT_ALPHABET, 10)