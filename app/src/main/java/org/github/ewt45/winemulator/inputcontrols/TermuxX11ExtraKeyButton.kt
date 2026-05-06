package org.github.ewt45.winemulator.inputcontrols

/**
 * Represents an extra key button in the TermuxX11 extra keys view.
 * This is a data class that holds information about a single button.
 */
class TermuxX11ExtraKeyButton(
    val key: String,
    val display: String,
    val popup: TermuxX11ExtraKeyButton? = null
)

/**
 * Represents the extra keys layout information.
 * Contains the matrix of buttons to display.
 */
class TermuxX11ExtraKeysInfo(
    val matrix: Array<Array<TermuxX11ExtraKeyButton>>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TermuxX11ExtraKeysInfo
        if (!matrix.contentDeepEquals(other.matrix)) return false
        return true
    }

    override fun hashCode(): Int {
        return matrix.contentDeepHashCode()
    }
}
