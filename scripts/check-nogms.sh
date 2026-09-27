#!/usr/bin/env bash
# Asserts the noGms variant carries no Google.
#
# AU1/AU12 reopened projection, and the whole safety of that decision rests on
# one claim: the DEFAULT build stays Google-free and only the opt-in `gms`
# flavor touches androidx.car.app. That claim is worth exactly as much as the
# check that enforces it — a transitive Play Services pull is invisible in a
# build file and obvious in a dependency tree.
#
# Denylist, not allowlist. xx-note/scripts/check-deps.sh pins a full allowlist
# of every resolved artifact, which is stronger and is where this should end
# up once the dependency set stabilises. It cannot be written before the app
# exists, so this starts as a denylist of precisely what the design forbids,
# and the TODO below is real work, not a hedge.
set -euo pipefail
cd "$(dirname "$0")/.."

if [ ! -f gradlew ]; then
    echo "check-nogms: no build yet — nothing to audit (by design)."
    echo "  This script arms itself the moment Phase 0 lands a Gradle scaffold."
    exit 0
fi

# Prefer the flavoured configuration; fall back to plain release before the
# flavor split lands in Phase 9.
CONFIG=noGmsReleaseRuntimeClasspath
if ! ./gradlew :app:dependencies --configuration "$CONFIG" --console=plain >/dev/null 2>&1; then
    CONFIG=releaseRuntimeClasspath
fi
echo "check-nogms: auditing :app:dependencies --configuration $CONFIG"

DEPS=$(mktemp); trap 'rm -f "$DEPS"' EXIT
./gradlew :app:dependencies --configuration "$CONFIG" --console=plain >"$DEPS"

RESOLVED=$(grep -oE '[a-zA-Z0-9._-]+:[a-zA-Z0-9._-]+:[a-zA-Z0-9._+-]+' "$DEPS" |
           cut -d: -f1-2 | sort -u)

# Anything here in the noGms classpath is a build failure, not a warning.
DENY='com.google.android.gms
com.google.firebase
com.google.android.play
com.google.mlkit
androidx.car.app
com.google.android.libraries'

bad=0
while IFS= read -r pat; do
    if HIT=$(printf '%s\n' "$RESOLVED" | grep -F "$pat" || true); [ -n "$HIT" ]; then
        echo "check-nogms: '$pat' resolved into the noGms classpath:" >&2
        printf '  %s\n' $HIT >&2
        bad=1
    fi
done <<<"$DENY"

if [ "$bad" -eq 1 ]; then
    echo >&2
    echo "  The noGms flavor is the default build and the one most installs get." >&2
    echo "  androidx.car.app belongs in the gms source set only (AU12)." >&2
    echo "  See contracts/XX-MAPS.md Part 2, 'Build flavors'." >&2
    exit 1
fi
echo "(no Google artifacts in the noGms classpath)"

# ── Second net: what actually shipped in the noGms APK ────────────────────
# A dependency tree can be fooled (a fat jar, a vendored class); the dex
# cannot. Guava (com.google.common.*) is allowed: media3 requires it, and it
# is a plain library, not Google services.
APK=$(find app/build/outputs/apk -path '*noGms*' -name '*release*.apk' -print 2>/dev/null | head -1 || true)
if [ -z "${APK:-}" ]; then
    echo "check-nogms: no noGms release APK — run ./gradlew :app:assembleNoGmsRelease first." >&2
    exit 1
fi
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -z "$SDK" ] && [ -f local.properties ]; then
    SDK=$(sed -n 's/^sdk.dir=//p' local.properties)
fi
DEXDUMP=$(ls -1 "$SDK"/build-tools/*/dexdump 2>/dev/null | sort -V | tail -1 || true)
if [ -z "${DEXDUMP:-}" ]; then
    echo "check-nogms: dexdump not found under \$ANDROID_HOME/build-tools" >&2
    exit 1
fi
echo "check-nogms: dexdump over $APK"
WORK=$(mktemp -d); trap 'rm -rf "$WORK" "$DEPS"' EXIT
unzip -q -o "$APK" 'classes*.dex' -d "$WORK"
DEX_DENY='Landroidx/car/app/
Lcom/google/android/
Lcom/google/firebase/
Lcom/google/mlkit/'
CLASSES=$(for d in "$WORK"/classes*.dex; do "$DEXDUMP" "$d" | grep -oE "Class descriptor *: *'L[^;]*;" ; done |
          sed -E "s/.*'(L[^;]*;)/\1/" | sort -u)
while IFS= read -r pat; do
    if HIT=$(printf '%s\n' "$CLASSES" | grep -F "$pat" | head -5); [ -n "$HIT" ]; then
        echo "check-nogms: noGms APK ships $pat classes:" >&2
        printf '  %s\n' $HIT >&2
        bad=1
    fi
done <<<"$DEX_DENY"
if [ "$bad" -eq 1 ]; then
    echo "  The noGms APK must carry no androidx.car.app and no Google services (AU1/AU12)." >&2
    exit 1
fi
echo "(no Google classes in the noGms APK — $(printf '%s\n' "$CLASSES" | wc -l) classes checked)"

# TODO(phase 9): once the dependency set settles, promote this to a pinned
# allowlist in the shape of xx-note/scripts/check-deps.sh. A denylist catches
# what we thought to name; an allowlist catches what we did not.
