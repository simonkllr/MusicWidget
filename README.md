# Music Player Widget (Android App & Glance Widget)

Willkommen zu diesem Projekt! Diese App ist ein eleganter, kompakter **Musik-Player**, der zwei zentrale Säulen der modernen Android-Entwicklung demonstriert:
1. Eine **vollwertige Haupt-App** mit **Jetpack Compose** und **MVVM-Architektur**.
2. Ein **interaktives Home-Screen-Widget** mit **Jetpack Compose Glance**.

> **Hinweis zur Funktionalität**: Es wird kein echts Audiosignal über die Lautsprecher abgespielt. Das Projekt dient als praxisnahes Lehrbeispiel für den synchronen Wechsel von Titel, Interpret, Fortschritt und Album-Covern sowohl in der App als auch auf dem Startbildschirm.

---

## 📂 Projektstruktur & Dateisystem

```text
com.example.musicplayerwidget/
│
├── data/
│   ├── model/
│   │   └── Song.kt               <-- Datenmodell & zentrale Cover-Ermittlung
│   └── repository/
│       ├── SongRepository.kt     <-- Statische Mock-Datenquelle (Songs 1–5)
│       └── MusicPlayerStateRepository.kt <-- Persistente Zustandssynchronisation (SharedPreferences)
│
├── viewmodel/
│   └── PlayerViewModel.kt        <-- App-Zustand & Ansteuerung der Widget-Instanzen
│
├── ui/
│   └── screens/
│       └── PlayerScreen.kt       <-- Jetpack Compose UI der Haupt-App
│
├── MusicPlayerGlanceWidget.kt    <-- Jetpack Compose Glance Widget & ActionCallbacks
└── MainActivity.kt               <-- Einstiegspunkt der App
```

---

## 🔍 Intensive Code-Erklärung (Klasse für Klasse)

### 1. Das Datenmodell (`data/model/Song.kt`)

```kotlin
data class Song(
    val id: Int,
    val title: String,
    val artist: String,
    val duration: Int,
    val coverUrl: String
) {
    val coverResId: Int
        get() = when (coverUrl) {
            "cover_1" -> R.drawable.cover_2
            "cover_2" -> R.drawable.cover_1
            "cover_3" -> R.drawable.cover_4
            "cover_4" -> R.drawable.cover_3
            "cover_5" -> R.drawable.cover_5
            else -> R.drawable.ic_launcher_foreground
        }
}
```
* **Was macht der Code?** 
  * `Song` ist eine immutable (unveränderliche) Datenklasse.
  * **Zentrale Ressource (`coverResId`)**: Statt in der App und im Widget doppelte `when`-Abfragen zu schreiben, ermittelt diese Property direkt die passende Bild-Ressource (`R.drawable.cover_X`). So zeigen App und Widget garantiert **immer dieselben Bilder** an.

---

### 2. Das Repository für den synchronen Zustand (`MusicPlayerStateRepository.kt`)

```kotlin
object MusicPlayerStateRepository {
    private const val PREFS_NAME = "music_player_shared_prefs"
    
    private val _songIndex = MutableStateFlow(0)
    val songIndex: StateFlow<Int> = _songIndex.asStateFlow()

    fun setSongIndex(context: Context, index: Int) {
        _songIndex.value = index
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit(commit = true) {
            putInt(KEY_SONG_INDEX, index)
        }
    }
}
```
* **Worum geht es hier?**
  * Haupt-App und Widget laufen in unterschiedlichen Lebenszyklen. Um den Zustand (welcher Song läuft, ist Pause gedrückt?) dauerhaft zu teilen, nutzt dieses Repository **Android SharedPreferences**.
* **Warum `commit = true`?**
  * Normalerweise speichert Android SharedPreferences mit `apply()` (asynchron im Hintergrund). Das kann dazu führen, dass das Widget sofort nach dem Klick liest, bevor der Wert auf die Festplatte geschrieben wurde. Durch `commit = true` wird der Wert **synchron sofort geschrieben** – das verhindert Hänger und Race-Conditions!

---

### 3. Das App-ViewModel (`viewmodel/PlayerViewModel.kt`)

```kotlin
class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val _currentSongIndex = mutableIntStateOf(MusicPlayerStateRepository.songIndex.value)
    val currentSongIndex: State<Int> = _currentSongIndex

    fun nextSong() {
        val next = (_currentSongIndex.intValue + 1) % songs.size
        _currentSongIndex.intValue = next
        MusicPlayerStateRepository.setSongIndex(getApplication(), next)
        triggerWidgetUpdate()
    }

    private fun triggerWidgetUpdate() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(MusicPlayerGlanceWidget::class.java)
            glanceIds.forEach { glanceId ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[MusicKeys.SONG_INDEX] = _currentSongIndex.intValue
                    }
                }
            }
            MusicPlayerGlanceWidget().updateAll(context)
        }
    }
}
```
* **Wie funktioniert die Ansteuerung der Widgets aus der App?**
  * Tippt der Nutzer in der App auf *Next*, ändert das `PlayerViewModel` seinen internen Compose-State (`_currentSongIndex`).
  * Über `GlanceAppWidgetManager` holt sich das ViewModel alle aktiven Widgets auf dem Homescreen (`glanceIds`).
  * Es schreibt mit `updateAppWidgetState(..., PreferencesGlanceStateDefinition, ...)` den neuen Index direkt in die Glance-Preferences der Widgets.
  * Schließlich erzwingt `MusicPlayerGlanceWidget().updateAll(context)` das sofortige Neuzeichnen der Widgets auf dem Startbildschirm.

---

### 4. Das Compose UI der App (`ui/screens/PlayerScreen.kt`)

```kotlin
@Composable
fun PlayerScreen(viewModel: PlayerViewModel = viewModel()) {
    val currentSong = viewModel.currentSong
    val isPlaying by viewModel.isPlaying

    Column(...) {
        Image(
            painter = painterResource(id = currentSong.coverResId),
            contentDescription = "Album Cover"
        )
        Text(text = currentSong.title)
        Text(text = currentSong.artist)
        
        IconButton(onClick = { viewModel.nextSong() }) {
            Icon(imageVector = Icons.Default.SkipNext, ...)
        }
    }
}
```
* **Reaktivität in Jetpack Compose**:
  * `PlayerScreen` liest die States aus dem ViewModel über `by viewModel.isPlaying`.
  * Wenn der State sich ändert, führt Compose automatisch ein **Recomposition** (Neu-Zusammensetzen der UI) durch. Es werden nur die UI-Elemente neu gezeichnet, die sich tatsächlich verändert haben.

---

### 5. Das Home-Screen-Widget (`MusicPlayerGlanceWidget.kt`)

```kotlin
class MusicPlayerGlanceWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val currentIndex = prefs[MusicKeys.SONG_INDEX] ?: 0
            val song = SongRepository.getAllSongs()[currentIndex]

            GlanceTheme {
                Box(modifier = GlanceModifier.clickable(actionStartActivity<MainActivity>())) {
                    Row(...) {
                        Image(provider = ImageProvider(song.coverResId), ...)
                        Text(text = song.title)
                        
                        Image(
                            provider = ImageProvider(android.R.drawable.ic_media_next),
                            modifier = GlanceModifier.clickable(actionRunCallback<NextActionCallback>())
                        )
                    }
                }
            }
        }
    }
}
```
* **Besonderheiten von Glance**:
  * **`currentState<Preferences>()`**: Glance liest seinen Zustand direkt aus den Glance-Preferences. Ändert sich dieser Wert, zeichnet sich das Widget neu.
  * **Klick auf den Hintergrund (`actionStartActivity<MainActivity>()`)**: Tippt der Nutzer auf den Freiraum oder das Cover des Widgets, öffnet Android automatisch die `MainActivity`.
  * **Klick auf Buttons (`actionRunCallback<NextActionCallback>()`)**: Tippt der Nutzer auf den *Next*-Button, wird kein App-Bildschirm gestartet, sondern im Hintergrund der `NextActionCallback` ausgeführt.

---

### 6. Die Interaktions-Callbacks (`ActionCallback`)

```kotlin
class NextActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(MusicPlayerGlanceWidget::class.java)
        
        var nextIndex = 0
        glanceIds.forEach { id ->
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                val current = prefs[MusicKeys.SONG_INDEX] ?: 0
                nextIndex = (current + 1) % SongRepository.getAllSongs().size
                prefs.toMutablePreferences().apply {
                    this[MusicKeys.SONG_INDEX] = nextIndex
                }
            }
            MusicPlayerGlanceWidget().update(context, id)
        }
        MusicPlayerStateRepository.setSongIndex(context, nextIndex)
    }
}
```
* **Was passiert bei einem Klick im Widget?**
  1. Der Callback liest den aktuellen Index aus den Glance-Preferences.
  2. Er errechnet den nächsten Index.
  3. Er speichert den neuen Index mit `updateAppWidgetState` in den Glance-State aller Widget-Instanzen.
  4. Er löst das Neuzeichnen aus (`update(context, id)`).
  5. Er speichert den neuen Index auch im `MusicPlayerStateRepository`, damit die App den gleichen Stand hat, wenn sie geöffnet wird.

---

## ⚠️ Wichtige Entwickler-Stolpersteine (Gotchas)

1. **Receiver-Registrierung im Manifest (`AndroidManifest.xml`)**:
   ```xml
   <receiver android:name=".MusicPlayerGlanceReceiver" android:exported="true">
       <intent-filter>
           <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
       </intent-filter>
       <meta-data
           android:name="android.appwidget.provider"
           android:resource="@xml/music_widget_provider" />
   </receiver>
   ```
   * *Ohne diese Registrierung findet Android das Widget nicht in der Homescreen-Widget-Auswahl!*

2. **Format & Seitenverhältnis (`res/xml/music_widget_provider.xml`)**:
   ```xml
   <appwidget-provider
       android:minWidth="280dp"
       android:minHeight="100dp"
       android:resizeMode="horizontal|vertical"
       android:widgetCategory="home_screen" />
   ```
   * `minWidth="280dp"` und `minHeight="100dp"` zwingen den Android-Launcher dazu, das Widget direkt im schlanken 2:1 Mini-Player-Querformat anzulegen (anstatt als riesiges 4x4 Quadrat).

---

## 🚀 Fazit

Dieses Projekt demonstriert lückenlos die saubere Zustandssynchronisation zwischen Android-UIs und Startbildschirm-Widgets mit modernstem Kotlin- & Jetpack-Compose-Code!
