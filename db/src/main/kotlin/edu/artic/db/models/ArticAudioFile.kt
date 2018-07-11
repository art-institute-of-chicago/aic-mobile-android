package edu.artic.db.models

data class ArticAudioFile(
        val title: String,
        val status: String,
        val nid: String,
        val type: String,
        val translations: List<Translation>,
        val audio_filename: String,
        val audio_file_url: String,
        val audio_filemime: String,
        val audio_filesize: String,
        val audio_transcript: String,
        val credits: String,
        val track_title: String
) {
    data class Translation(
            val language: String,
            val title: String,
            val track_title: String,
            val audio_filename: String,
            val audio_file_url: String,
            val audio_filemime: String,
            val audio_filesize: String,
            val audio_transcript: String,
            val credits: String
    )
}