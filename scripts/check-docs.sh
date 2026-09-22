#!/usr/bin/env bash
# The lane that works before there is any code.
#
# These two repos were forged as specs — no Gradle, no source. A CI that runs
# ./gradlew on day one is red on every push, and a gate everyone has learned
# to ignore is worse than no gate. So this script audits what is actually true
# today, and scripts/check-permissions.sh + scripts/check-nogms.sh take over
# the moment the build exists.
#
# Four checks, all hermetic, none needing a JDK or an SDK.
set -euo pipefail
cd "$(dirname "$0")/.."
fail=0

# ── 1. Relative markdown links resolve ───────────────────────────────────
# A contract that points at a file which moved is a contract nobody can
# follow. Cheap to check, and these repos are mostly documents.
echo "--- markdown links ---"
while IFS= read -r line; do
    [ -z "$line" ] && continue
    src="${line%%:*}"; target="${line#*:}"
    target="${target%%#*}"                      # strip #anchor
    [ -z "$target" ] && continue
    case "$target" in http*|mailto:*) continue ;; esac
    if [ ! -e "$(dirname "$src")/$target" ] && [ ! -e "$target" ]; then
        echo "check-docs: broken link in $src -> $target" >&2
        fail=1
    fi
done < <(grep -rnoE '\]\([^)]+\)' --include='*.md' . 2>/dev/null |
         sed -E 's/^([^:]+):[0-9]+:\]\((.*)\)$/\1:\2/')
[ "$fail" -eq 0 ] && echo "(all resolve)"

# ── 2. No AI-vendor trailers in any commit message ───────────────────────
# Estate rule, xx-apps/docs/DISPATCH-2026-09-20.md: "no AI-vendor trailers of
# any kind ... The only acceptable tag in this estate is Skippy-Agent alone."
# It was a rule agents had to remember. Now it is a rule the forge enforces.
echo "--- commit trailers ---"
if git rev-parse --git-dir >/dev/null 2>&1; then
    if BAD=$(git log --format='%H %s%n%b' |
             grep -inE 'co-authored-by|generated with|noreply@anthropic|cursoragent@|opencode\.ai' || true)
       [ -n "$BAD" ]; then
        echo "check-docs: AI-vendor trailer found in history" >&2
        printf '%s\n' "$BAD" >&2
        fail=1
    else
        echo "(clean)"
    fi
else
    echo "(not a git checkout — skipped)"
fi

# ── 3. No committed secrets ──────────────────────────────────────────────
# The estate rule is "never print the token". This is the version that
# survives someone pasting one into a doc.
echo "--- secrets ---"
if BAD=$(grep -rnE 'gh[pousr]_[A-Za-z0-9]{20,}|BEGIN (RSA|OPENSSH|PGP) PRIVATE KEY|password=[^$\"[:space:]]' \
         --include='*.md' --include='*.kts' --include='*.kt' --include='*.sh' --include='*.yml' \
         --exclude-dir=.git . 2>/dev/null | grep -v 'check-docs.sh' || true)
   [ -n "$BAD" ]; then
    echo "check-docs: possible secret committed" >&2
    printf '%s\n' "$BAD" >&2
    fail=1
else
    echo "(clean)"
fi

# ── 4. Gate honesty ──────────────────────────────────────────────────────
# The build lane self-activates on hashFiles('gradlew'), so it cannot stay
# dormant by accident. This catches the other direction: someone landing the
# Gradle scaffold and gutting the workflow in the same breath.
echo "--- gate honesty ---"
if [ -f gradlew ]; then
    for required in testDebugUnitTest check-permissions.sh check-nogms.sh; do
        if ! grep -q "$required" .github/workflows/ci.yml; then
            echo "check-docs: gradlew exists but ci.yml no longer runs $required" >&2
            fail=1
        fi
    done
    [ "$fail" -eq 0 ] && echo "(build lane armed and complete)"
else
    echo "(no gradlew yet — build lane dormant, by design)"
fi

exit "$fail"
