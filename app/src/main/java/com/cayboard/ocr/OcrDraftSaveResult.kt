package com.cayboard.ocr
internal enum class OcrDraftSaveResult(val errorMessage:String?) { ACCEPTED(null), EMPTY_TEXT("内容不能为空") }
