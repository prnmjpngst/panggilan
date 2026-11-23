package com.prnmjpngst.pemanggilan

fun buildClips(plate: String, gagal: Boolean): List<Any> {
    val clips = mutableListOf<Any>("panggilan.mp3")
    val plateClips = plate.uppercase().mapNotNull { ch ->
        when {
            ch.isDigit() -> "$ch.mp3"
            ch in 'A'..'Z' -> "${ch.lowercaseChar()}.mp3"
            else -> null
        }
    }
    clips.addAll(plateClips)
    clips.add(1000) // 1 second delay between repetitions
    clips.addAll(plateClips)
    clips.add(if (gagal) "ruang-penguji.mp3" else "loket-pengambilan.mp3")
    return clips
}