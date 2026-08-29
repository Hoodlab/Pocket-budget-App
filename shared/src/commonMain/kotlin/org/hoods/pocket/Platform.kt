package org.hoods.pocket

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform