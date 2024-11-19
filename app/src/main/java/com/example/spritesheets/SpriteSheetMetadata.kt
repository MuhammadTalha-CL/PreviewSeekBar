package com.example.spritesheets

data class SpriteSheetMetadata(
    val sprite_sheet_name: String,
    val sprite_link: String,
    val start_time: Int,
    val end_time: Int,
    val frame_width: Int,
    val frame_height: Int,
    val frames_per_row: Int,
    val frame_interval: Int,
    val total_frames: Int
)
