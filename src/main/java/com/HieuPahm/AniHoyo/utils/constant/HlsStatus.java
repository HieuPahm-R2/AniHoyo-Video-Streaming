package com.HieuPahm.AniHoyo.utils.constant;

/**
 * Lifecycle of the FFmpeg &rarr; R2 HLS pipeline for a single episode.
 *
 * <p>Transcoding used to run inside the {@code POST /api/v1/add-episode} request,
 * so a slow (or killed) FFmpeg run turned into an HTTP 500 for the admin even
 * though the episode row had already been committed. The row now carries its
 * own state and the pipeline runs on a background thread.
 */
public enum HlsStatus {
    /** Saved and queued, the worker has not picked it up yet. */
    PENDING,
    /** FFmpeg is downloading / transcoding / uploading right now. */
    PROCESSING,
    /** Every playlist and segment is in the media bucket; playback can start. */
    READY,
    /** The pipeline threw; the reason is in {@code Episode#getHlsError()}. */
    FAILED
}
