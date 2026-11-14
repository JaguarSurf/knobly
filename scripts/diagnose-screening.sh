#!/bin/bash
#
# Why did a call ring (or not)? Checks a phone connected over adb, then watches one incoming call.
# Everything stays local: it only reads the phone through adb and prints to this terminal.
#
# Usage:
#   scripts/diagnose-screening.sh <last digits of the caller, e.g. 87654321>
#
# Then call the phone from that number while the script is watching (60 s).

set -uo pipefail

SUFFIX="${1:-}"
if ! [[ "$SUFFIX" =~ ^[0-9]{4,}$ ]]; then
    echo "Usage: $0 <at least the last 4 digits of the caller, e.g. 87654321>" >&2
    exit 2
fi

PACKAGE=app.knobly
ok() { echo "  OK    $*"; }
bad() { echo "  FAIL  $*"; }
info() { echo "  ..    $*"; }

adb get-state >/dev/null 2>&1 || { echo "No device: connect the phone first (docs/development.md)." >&2; exit 1; }

echo "1. App and permissions"
version=$(adb shell dumpsys package "$PACKAGE" 2>/dev/null | grep -m1 versionName | tr -d '\r' | sed 's/.*versionName=//')
if [ -z "$version" ]; then
    bad "Knobly is not installed"
    exit 1
fi
ok "Knobly $version installed"

holders=$(adb shell cmd role get-role-holders android.app.role.CALL_SCREENING 2>/dev/null | tr -d '\r')
if [[ "$holders" == *"$PACKAGE"* ]]; then
    ok "Knobly is the call screening app"
else
    bad "Knobly is NOT the call screening app (holder: ${holders:-none}). Android will not ask it about calls."
    info "Fix: Filters tab > 'Enable call filtering' (needed again after every reinstall)."
fi

if adb shell dumpsys package "$PACKAGE" | grep -q "READ_CONTACTS: granted=true"; then
    ok "Contacts permission granted"
else
    bad "Contacts permission NOT granted: 'Aceitar somente contatos' does nothing (it never blocks everyone)."
fi

echo
echo "2. Numbers ending in $SUFFIX still in the contacts (any account, any format)"
matches=$(adb shell content query --uri content://com.android.contacts/data/phones \
    --projection raw_contact_id:account_type:account_name:display_name:data1 2>/dev/null | tr -d '\r' |
    while IFS= read -r row; do
        number=${row##*data1=}
        digits=${number//[^0-9]/}
        [[ "$digits" == *"$SUFFIX" ]] && echo "$row"
    done)
# Also matches the number saved without the 9th digit (Knobly treats them as the same number).
if [ ${#SUFFIX} -ge 9 ]; then
    short=${SUFFIX: -8}
    [ -z "$matches" ] && info "(no exact suffix match; also tried the last 8 digits $short)"
    matches=${matches:-$(adb shell content query --uri content://com.android.contacts/data/phones \
        --projection raw_contact_id:account_type:account_name:display_name:data1 2>/dev/null | tr -d '\r' |
        while IFS= read -r row; do
            number=${row##*data1=}
            digits=${number//[^0-9]/}
            [[ "$digits" == *"$short" ]] && echo "$row"
        done)}
fi
if [ -n "$matches" ]; then
    bad "The number is still saved, so Knobly treats it as a contact:"
    while IFS= read -r match; do echo "        $match"; done <<< "$matches"
    info "account_type tells where it lives (e.g. com.google, com.whatsapp, org.telegram.messenger,"
    info "vnd.sec.contact.phone = Samsung device, sim). Delete it there too, or wait for the app/account to sync."
else
    ok "No contact has this number"
fi

echo
echo "3. Now call the phone from that number (watching for 60 s; press Ctrl+C to stop)"
adb logcat -c
timeout 60 adb logcat -v time -s KnoblyScreening:I Telecom:I 2>/dev/null | tr -d '\r' |
    grep --line-buffered -E "KnoblyScreening|SCREENING_BOUND|SCREENING_COMPLETED|CallScreeningServiceFilter@[0-9a-f]+: $PACKAGE" |
    while IFS= read -r line; do
        case "$line" in
            *"action=REJECT"*) echo "  =>    Knobly rejected the call: $line" ;;
            *"action=ALLOW rule=contacts-allow"*) echo "  =>    Knobly let it ring because it is a CONTACT (see step 2): $line" ;;
            *"action=ALLOW rule=null"*) echo "  =>    No filter matched: is 'Aceitar somente contatos' on? Permission granted? $line" ;;
            *"action="*) echo "  =>    $line" ;;
            *) echo "  ..    Android: ${line##*Telecom: }" | cut -c1-160 ;;
        esac
    done
echo
echo "No 'Knobly ...' line above? Android did not ask Knobly about the call: check step 1, and note that some"
echo "manufacturers skip screening apps for numbers they still consider contacts."
