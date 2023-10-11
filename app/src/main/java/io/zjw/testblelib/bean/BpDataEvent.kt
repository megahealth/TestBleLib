package io.zjw.testblelib.bean

import io.mega.megableparse.ParsedBPBean

data class BpDataEvent(
    val parsedBPBean: ParsedBPBean,
    val duration: Int
)
