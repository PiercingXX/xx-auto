package android.net

import android.os.Parcel

/**
 * A concrete [Uri] for plain JVM unit tests. [Uri]'s constructor is
 * package-private, so no real Uri can be built outside android.net, and
 * [Uri.parse] is an Android stub that returns null under returnDefaultValues
 * (this project has no Robolectric or mocking dependency — it tests through
 * pure seams by design). This helper lives in the android.net package to
 * construct a non-null Uri.
 *
 * It is a faithful double, not a mock: the one method the mapper uses on an
 * artwork Uri is [toString], and a real Uri's toString is its string form. This
 * class returns exactly that, so `NowPlayingState.map` runs its real
 * `artworkUri?.toString()` path on a non-null Uri and the assertion is on the
 * mapper's real output. It sits in android.net only to satisfy the
 * package-private constructor; it shadows the framework class in the test
 * classpath, which is acceptable because the mapper never parses a Uri.
 */
class TestUri(private val value: String) : Uri() {

    override fun toString(): String = value

    override fun getScheme(): String? = null

    override fun getSchemeSpecificPart(): String? = null

    override fun getEncodedSchemeSpecificPart(): String? = null

    override fun getAuthority(): String? = null

    override fun getEncodedAuthority(): String? = null

    override fun getUserInfo(): String? = null

    override fun getEncodedUserInfo(): String? = null

    override fun getHost(): String? = null

    override fun getPort(): Int = -1

    override fun getPath(): String? = null

    override fun getEncodedPath(): String? = null

    override fun getQuery(): String? = null

    override fun getEncodedQuery(): String? = null

    override fun getFragment(): String? = null

    override fun getEncodedFragment(): String? = null

    override fun getPathSegments(): MutableList<String> = mutableListOf()

    override fun getLastPathSegment(): String? = null

    override fun getQueryParameter(name: String?): String? = null

    override fun getQueryParameterNames(): MutableSet<String> = mutableSetOf()

    override fun getQueryParameters(key: String?): MutableList<String> = mutableListOf()

    override fun isHierarchical(): Boolean = true

    override fun isOpaque(): Boolean = false

    override fun isRelative(): Boolean = false

    override fun buildUpon(): Uri.Builder = Uri.Builder()

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = Unit
}