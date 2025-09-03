package com.yju.data.kanji.dao

import javax.inject.Inject

class KanjiBatchInsertProcessor @Inject constructor(
    private val kanjiDatabase: KanjiDatabase
)