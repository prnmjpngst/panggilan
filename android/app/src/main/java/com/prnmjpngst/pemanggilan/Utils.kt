package com.prnmjpngst.pemanggilan

fun buildClips(plate: String, gagal: Boolean): List<String> {
    val clips = mutableListOf("panggilan.mp3")
    for (ch in plate.uppercase()) {
        when {
            ch.isDigit() -> clips.add("$ch.mp3")
            ch in 'A'..'Z' -> clips.add("${ch.lowercaseChar()}.mp3")
        }
    }
    clips.add(if (gagal) "ruang-penguji.mp3" else "loket-pengambilan.mp3")
    return clips
}