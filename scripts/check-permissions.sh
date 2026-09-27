#!/usr/bin/env bash
# Audits the MERGED release manifest against what design.md actually claims.
#
# xx-auto's headline claim is AU11 + the README: "No INTERNET. Nothing leaves
# the phone." That is the kind of claim that rots the first time someone adds
# a library with a transitive <uses-permission>. A merged-manifest audit is
# the only thing that catches it, because the app's own manifest will still
# look innocent.
#
# Modelled on xx-note/scripts/check-permissions.sh, which does the same job
# for the same reason.
#
# Two lists. FORBIDDEN is absolute and contradicts the design if present.
# EXPECTED is pinned and may be edited — deliberately, in the open, with a
# reason — as the app gains surfaces.
set -euo pipefail
cd "$(dirname "$0")/.."

MERGED=$(find app/build/intermediates/merged_manifests -name AndroidManifest.xml -print 2>/dev/null |
         grep -i 'nogms.*release\|release.*nogms' | head -1 || true)
[ -z "${MERGED:-}" ] && MERGED=$(find app/build/intermediates/merged_manifests/release \
         -name AndroidManifest.xml -print -quit 2>/dev/null || true)

if [ -z "${MERGED:-}" ]; then
    if [ ! -f gradlew ]; then
        echo "check-permissions: no build yet — nothing to audit (by design)."
        echo "  This script arms itself the moment Phase 0 lands a manifest."
        exit 0
    fi
    echo "check-permissions: no merged manifest found." >&2
    echo "  Run './gradlew :app:assembleNoGmsRelease' (or :app:assembleRelease) first." >&2
    exit 1
fi
echo "check-permissions: auditing $MERGED"

# Flatten first: the merger writes an element with more than one attribute
# (e.g. maxSdkVersion) across several lines, and a line-wise grep would miss
# it — including a transitive INTERNET written that way.
ACTUAL=$(tr '\n' ' ' <"$MERGED" | tr -s ' ' |
         grep -oE '<uses-permission(-sdk-23)? android:name="[^"]*"' |
         sed -E 's/<uses-permission(-sdk-23)? android:name="//; s/"$//' | sort -u)

# ── FORBIDDEN: present = the design is a lie ─────────────────────────────
# INTERNET is AU11 and the README. The location and notification-listener
# permissions are things xx-auto has explicitly declined (AU10, "Later").
echo "--- forbidden permissions ---"
FORBIDDEN='android.permission.INTERNET
android.permission.ACCESS_FINE_LOCATION
android.permission.ACCESS_COARSE_LOCATION
android.permission.BIND_NOTIFICATION_LISTENER_SERVICE'
bad=0
while IFS= read -r perm; do
    if printf '%s\n' "$ACTUAL" | grep -qx "$perm"; then
        echo "check-permissions: FORBIDDEN permission present: $perm" >&2
        bad=1
    fi
done <<<"$FORBIDDEN"
if [ "$bad" -eq 1 ]; then
    echo "  xx-auto claims 'No INTERNET. Nothing leaves the phone.' (AU11, README)." >&2
    echo "  Either the dependency that pulled this in goes, or the claim does." >&2
    exit 1
fi
echo "(none present)"

# ── EXPECTED: the pinned set from design.md "Identity and build" ─────────
# Deliberate additions beyond design.md's list, each with its reason:
# - BLUETOOTH (maxSdkVersion 30): on API 26-30 the bonded-device list and the
#   ACL_CONNECTED broadcast need it; BLUETOOTH_CONNECT only exists from 31.
# - ${applicationId}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION: androidx.core's
#   own signature permission backing ContextCompat.registerReceiver(
#   RECEIVER_NOT_EXPORTED) below API 33, as in xx-note. App-private.
# media3-common's ACCESS_NETWORK_STATE is stripped in the manifest
# (tools:node="remove"); it must not reappear here.
echo "--- expected vs actual ---"
EXPECTED=$(cat <<'PERMS'
android.permission.BLUETOOTH
android.permission.BLUETOOTH_CONNECT
android.permission.CALL_PHONE
android.permission.POST_NOTIFICATIONS
android.permission.READ_CONTACTS
android.permission.SYSTEM_ALERT_WINDOW
com.piercingxx.xxauto.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
com.piercingxx.xxlauncher.permission.THEME_SYNC
PERMS
)
if DIFF=$(diff <(printf '%s\n' "$EXPECTED") <(printf '%s\n' "$ACTUAL")); then
    echo "(identical)"
else
    echo "check-permissions: PERMISSION SET MISMATCH" >&2
    printf '%s\n' "$DIFF" >&2
    echo "  < = pinned expected   > = found in merged manifest" >&2
    echo "  Edit EXPECTED above deliberately, with a reason, or drop the permission." >&2
    exit 1
fi

# ── AU12: xx-auto's car surface is media, never navigation ───────────────
# "xx-maps owns the car screen for navigation; xx-auto owns it for media."
# An app declaring the navigation category to draw someone else's map is how
# you get two nav apps fighting for one screen.
# The car service only exists in the gms flavor, so audit that manifest too —
# checking noGms alone would pass vacuously.
echo "--- AU12: car app category ---"
GMS_MERGED=$(find app/build/intermediates/merged_manifests -name AndroidManifest.xml -print 2>/dev/null |
             grep -i 'gmsrelease' | grep -vi 'nogms' | head -1 || true)
for m in "$MERGED" ${GMS_MERGED:+"$GMS_MERGED"}; do
    if grep -q 'androidx.car.app.category.NAVIGATION' "$m"; then
        echo "check-permissions: $m declares category.NAVIGATION — AU12 violation" >&2
        echo "  Navigation on the car screen belongs to xx-maps. See contracts/XX-MAPS.md Part 2." >&2
        exit 1
    fi
    if grep -q 'androidx.car.app.NAVIGATION_TEMPLATES' "$m"; then
        echo "check-permissions: $m requests NAVIGATION_TEMPLATES — AU12 violation" >&2
        exit 1
    fi
done
if [ -z "${GMS_MERGED:-}" ]; then
    echo "check-permissions: no gms merged manifest — build :app:assembleGmsRelease to audit the car surface." >&2
    exit 1
fi
if grep -q 'androidx.car.app.CarAppService' "$MERGED"; then
    echo "check-permissions: the noGms manifest declares a CarAppService — AU1/AU12 violation" >&2
    exit 1
fi
echo "(media only, as AU12 requires; noGms has no car service)"
