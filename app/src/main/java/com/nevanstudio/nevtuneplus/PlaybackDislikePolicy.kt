// Copyright (c) 2026 Sachit. All rights reserved.
// Project: NevTune+
package com.nevanstudio.nevtuneplus

import com.nevanstudio.nevtuneplus.data.model.LikeStatus

internal fun shouldSkipAfterDislike(
    previousStatus: LikeStatus,
    targetVideoId: String,
    currentVideoId: String?,
): Boolean = previousStatus != LikeStatus.DISLIKE && targetVideoId == currentVideoId
