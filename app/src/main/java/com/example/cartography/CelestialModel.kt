package com.example.cartography

data class CleanCelestialNode(
    val id: String,
    val displayLabel: String,
    var orbitalAngleRad: Double,
    val angularVelocity: Double
)

data class SolarDeclinationResult(
    val dayOfYear: Int,
    val declinationRad: Double,
    val declinationDeg: Double,
    val axialTiltDeg: Double,
    val equationOfTimeMinutes: Double
)

data class DawnDuskBarrierState(
    val latitudeDeg: Double,
    val longitudeDeg: Double,
    val hourAngleRad: Double,
    val hourAngleDeg: Double,
    val isPolarDay: Boolean,
    val isPolarNight: Boolean,
    val terminatorNormalVector: DoubleArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DawnDuskBarrierState

        if (latitudeDeg != other.latitudeDeg) return false
        if (longitudeDeg != other.longitudeDeg) return false
        if (hourAngleRad != other.hourAngleRad) return false
        if (hourAngleDeg != other.hourAngleDeg) return false
        if (isPolarDay != other.isPolarDay) return false
        if (isPolarNight != other.isPolarNight) return false
        if (!terminatorNormalVector.contentEquals(other.terminatorNormalVector)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = latitudeDeg.hashCode()
        result = 31 * result + longitudeDeg.hashCode()
        result = 31 * result + hourAngleRad.hashCode()
        result = 31 * result + hourAngleDeg.hashCode()
        result = 31 * result + isPolarDay.hashCode()
        result = 31 * result + isPolarNight.hashCode()
        result = 31 * result + terminatorNormalVector.contentHashCode()
        return result
    }
}

data class BucketMouthNav(
    val frontSpoutRadius: Double = 160.0,
    val rearSpoutRadius: Double = 120.0,
    var siriusAntaresAngle: Double = 0.0
)

data class QuranReference(
    val surahAyah: String,
    val theme: String,
    val transliteration: String,
    val translation: String,
    val contextNote: String
)

val SCRIPTURAL_REFERENCES = listOf(
    QuranReference(
        surahAyah = "Surah Ya-Sin\n(36:40)",
        theme = "Orbital Independence & Dawn/Dusk Barrier",
        transliteration = "La ash-shamsu yanbaghi laha an tudrika al-qamara wa la al-laylu sabiqu an-nahari wa kullun fi falakin yasbahun.",
        translation = "It is not allowable for the sun to reach the moon, nor does the night overtake the day, but each, in an orbit, is swimming.",
        contextNote = "Celestial bodies move in distinct non-overlapping paths with an inviolable barrier separating the day/night hemispheres."
    ),
    QuranReference(
        surahAyah = "Surah Al-Baqarah\n(2:127)",
        theme = "Foundations of the Sanctuary (Ka'aba)",
        transliteration = "Wa idh yarfa'u Ibrahimu al-qawa'ida mina al-bayti wa Isma'ilu...",
        translation = "And [remember] when Abraham was raising the foundations of the House with Ishmael, [saying], 'Our Lord, accept [this] from us. Indeed You are the Hearing, the Knowing.'",
        contextNote = "Central cartographic origin B1-B3 anchor meridian intersection, establishing terrestrial-celestial alignment."
    ),
    QuranReference(
        surahAyah = "Surah Al-Baqarah\n(2:158)",
        theme = "Safa and Marwa Landmarks",
        transliteration = "Inna as-Safa wa al-Marwata min sha'a'iri Allahi faman hajja al-bayta aw i'tamara fala junaha 'alayhi an yattawwafa bihima...",
        translation = "Indeed, 'as-Safa' and 'al-Marwah' are among the symbols of Allah. So whoever makes Hajj to the House or performs 'Umrah - there is no blame upon him for walking between them.",
        contextNote = "Terrestrial axis markers forming the navigational baseline of celestial sighting."
    ),
    QuranReference(
        surahAyah = "Surah An-Najm\n(53:49)",
        theme = "The Lord of Sirius",
        transliteration = "Wa annahu huwa rabbu ash-Shi'ra.",
        translation = "And that it is He who is the Lord of Sirius (*As-Shi'ra*).",
        contextNote = "Primary navigational beacon star (Alpha Canis Majoris), the brightest star in Earth's night sky used for oceanic and desert transit."
    ),
    QuranReference(
        surahAyah = "Surah Yusuf\n(12:4)",
        theme = "Vision of Celestial Bodies",
        transliteration = "Idh qala Yusufu li-abihi ya abati inni ra'aytu ahada 'ashara kawkaban wa ash-shamsa wa al-qamara ra'aytuhum li sajidin.",
        translation = "[Remember] when Joseph said to his father, 'O my father, indeed I have seen eleven stars and the sun and the moon; I saw them prostrating to me.'",
        contextNote = "Prophetic astronomy observing 11 planetary bodies alongside the solar and lunar nodes."
    )
)

data class TerminalLogEntry(
    val timestamp: String,
    val message: String,
    val isSystem: Boolean = false
)
