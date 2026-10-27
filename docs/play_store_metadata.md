# Google Play Store Listing & Privacy Policy Metadata

## 1. App Store Details

- **App Name:** Stoic Journal - Daily Reflection & Wisdom
- **Package Name:** `com.stoicjournal`
- **Category:** Lifestyle / Productivity
- **Content Rating:** Everyone

### Short Description (Max 80 characters)
*A quiet, 100% offline space for daily reflection, security & Stoic wisdom.*

### Long Description (Dutch)
**Stoic Journal** is jouw persoonlijke, veilige en volledig offline plek voor dagelijkse reflectie en innerlijke rust, geïnspireerd door de klassieke stoïcijnse filosofie van Marcus Aurelius, Epictetus en Seneca.

**Kernfunctionaliteiten:**
- 🏛️ **Begeleide Stoïcijnse Vragen & Citaten:** Start je dag met de *Ochtend-intentie (Premeditatio Malorum)* en sluit af met *Seneca's Avondreview*.
- 🛡️ **Biometrische & PIN Beveiliging:** Bescherm je persoonlijk dagboek met vingerafdruk, gezichtsherkenning of een 4-cijferige PIN-code. Schakel het Privacy Scherm in om screenshots en app-swapper previews te blokkeren.
- 🌅 **Slimme Herinneringen & Direct Reageren:** Ontvang dagelijkse herinneringen en typ je gedachten direct vanuit de notificatiebalk met Direct Reply.
- 🦁 **De 4 Kardinaaldeugden Tracker:** Houd bij welke stoïcijnse deugd (*Wijsheid, Moed, Rechtvaardigheid, Matigheid*) je hebt beoefend en bekijk je voortgang in het statistiekenscherm.
- ⚖️ **Cirkel van Invloed (Dichotomy of Control):** Verdeel je zorgen in 'Binnen mijn controle' en 'Buiten mijn controle' om stress om te zetten in kalmte.
- 🗣️ **Voice Journaling:** Spreek je gedachten in via spraakherkenning wanneer je niet wilt typen.
- 💾 **100% Offline & Lokale Backup:** Je data blijft uitsluitend lokaal op jouw apparaat. Exporteer en importeer eenvoudig je volledige dagboek via JSON.

---

## 2. Privacybeleid (Privacy Policy)

**Privacy & Gegevensverwerking:**
*Stoic Journal* is ontworpen met privacy als hoogste prioriteit:
1. **Lokale Gegevensopslag:** Alle dagboeknotities, voorkeuren en statistieken worden uitsluitend lokaal opgeslagen op het apparaat van de gebruiker (via Android Room en Jetpack DataStore). Er vindt geen synchronisatie met externe servers of cloudbronnen plaats.
2. **Biometrische Gegevens:** De app gebruikt de officiële Android `androidx.biometric` API. Biometrische gegevens worden verwerkt door de veilige hardware van het Android-besturingssysteem en worden nooit door de app gelezen of opgeslagen.
3. **Microfoonpermissie (`RECORD_AUDIO` / SpeechRecognizer):** De spraak-naar-tekst functie maakt gebruik van het standaardsysteem voor spraakherkenning van het Android-apparaat. Audiofragmenten worden niet opgeslagen of verzonden door de app.
4. **Geen Tracking of Analytics:** De app bevat geen derde-partij SDK's voor advertenties, analytics of tracking.

---

## 3. Release Artifacts

- **Android App Bundle (.aab):** `app/build/outputs/bundle/release/app-release.aab`
- **Release Signed APK (.apk):** `app/build/outputs/apk/release/app-release.apk`
- **Signing Config:** RSA 2048-bit Keystore (`stoic-journal-release.keystore`)
