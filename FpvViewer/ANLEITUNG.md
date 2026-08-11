# FpvViewer – Anleitung für Einsteiger

Diese App zeigt das Bild einer beliebigen USB-Capture-Card (egal welche Brille/Quelle
dranhängt) live auf deinem Android-Handy an und lässt dich unten in der Leiste die
Auflösung wählen (z.B. 1080p statt der bei vielen fertigen Apps erzwungenen 720p).

**Wichtig zu deinem DJI-Wunsch:** Dieses Projekt deckt den Weg "Brille -> HDMI ->
Capture Card -> Handy" ab. Das funktioniert mit jeder Quelle, auch DJI, weil zu dem
Zeitpunkt bereits ein normales Videosignal anliegt. Ein direkter Anschluss vom
DJI-Controller/Goggles per USB-C OHNE Capture Card ist technisch etwas komplett
anderes (proprietäres DJI-Protokoll, braucht DJI Mobile SDK) und ist NICHT Teil
dieses Projekts.

---

## Weg A (empfohlen für dich): APK ohne eigene Installation bauen lassen

Du brauchst dafür NUR einen Browser und einen kostenlosen GitHub-Account. Kein
Android Studio, keine Kommandozeile.

1. Gehe auf https://github.com und erstelle einen kostenlosen Account (falls noch
   nicht vorhanden)
2. Oben rechts auf das "+" -> "New repository" klicken
3. Einen Namen eingeben (z.B. `fpvviewer`), auf "Create repository" klicken
4. Auf der neuen Repo-Seite auf "uploading an existing file" klicken
5. Alle Dateien/Ordner aus dem entpackten `FpvViewer`-Ordner per Drag&Drop in das
   Browserfenster ziehen (den ganzen Inhalt, inkl. dem versteckten `.github`-Ordner –
   falls dein Dateimanager versteckte Ordner nicht anzeigt, kurz aktivieren)
6. Unten auf "Commit changes" klicken
7. Oben im Repo auf den Tab **"Actions"** klicken – dort startet automatisch ein
   Build ("APK bauen"). Das dauert ca. 3-5 Minuten (gelber Punkt -> grüner Haken)
8. Wenn der Haken grün ist, auf den Workflow-Lauf klicken -> ganz unten bei
   "Artifacts" erscheint **"FpvViewer-apk"** -> anklicken zum Herunterladen
   (kommt als ZIP mit der `app-debug.apk` drin)
9. Die `app-debug.apk` aufs Handy übertragen (z.B. per USB-Kabel, Google Drive,
   oder dir selbst per Mail/Messenger schicken)
10. Auf dem Handy die APK-Datei antippen -> Installation erlauben (Android fragt
    ggf. nach "Installation aus unbekannten Quellen erlauben" -> ja)

**Falls der Actions-Lauf mit rotem X fehlschlägt:** auf den fehlgeschlagenen Lauf
klicken, den roten Fehlertext kopieren und mir hier im Chat schicken – dann fixe
ich das Projekt für dich.

---

## Weg B: Selbst in Android Studio bauen

Falls du später doch lokal entwickeln willst:

1. Android Studio installieren: https://developer.android.com/studio
2. Entpackten `FpvViewer`-Ordner in Android Studio öffnen ("Open")
3. Gradle-Sync abwarten (lädt Abhängigkeiten herunter, braucht Internet)
4. Handy per USB anschließen, USB-Debugging aktivieren (Einstellungen ->
   Über das Telefon -> 7x auf Build-Nummer tippen -> Entwickleroptionen ->
   USB-Debugging an)
5. In Android Studio dein Handy auswählen, auf den grünen Play-Button klicken

**Falls der Sync fehlschlägt:** meistens liegt es an der Kamera-Bibliotheks-Zeile
in `app/build.gradle`. Auf https://github.com/jiangdongguo/AndroidUSBCamera im
README nachsehen, welche aktuelle Gradle-Zeile dort empfohlen wird, damit ersetzen.

---

## Nutzung (beide Wege)

1. USB-OTG-Adapter ins Handy, Capture Card daran anschließen
2. App "FpvViewer" öffnen
3. USB-Berechtigung erlauben
4. Unten Auflösung antippen (z.B. 1920x1080)
5. Bild erscheint

## Wenn etwas nicht klappt
- **Kein Bild:** Prüfe, ob dein Handy USB-OTG unterstützt
- **Build-Fehler (egal ob Weg A oder B):** Fehlertext hier im Chat schicken
- **Zum Vergleich/als Absicherung:** eine fertige App wie "UVC Camera" aus dem
  Play Store testen, um Hardware-Kompatibilität zu prüfen
