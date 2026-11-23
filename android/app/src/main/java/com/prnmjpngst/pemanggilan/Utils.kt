package com.prnmjpngst.pemanggilan

fun buildClips(plate: String, gagal: Boolean): List<Any> {
    val plateClips = plate.uppercase().mapNotNull { ch ->
        when {
            ch.isDigit() -> "$ch.mp3"
            ch in 'A'..'Z' -> "${ch.lowercaseChar()}.mp3"
            else -> null
        }
    }
    val tujuan = if (gagal) "ruang-penguji.mp3" else "loket-pengambilan.mp3"
    val clips = mutableListOf<Any>("panggilan.mp3")
    clips.addAll(plateClips)
    clips.add(tujuan)
    clips.add("saya-ulangi.mp3")
    clips.addAll(plateClips)
    clips.add(tujuan)
    return clips
}