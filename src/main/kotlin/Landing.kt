import kotlin.browser.window

val safeRegex = Regex("[0-9.-A-Za-z]+")

@JsName("start")
fun start() {
    val url = window.location.pathname.split("/").last()
    if (safeRegex.matchEntire(url) == null) {
        window.alert("The url may be mallicious")
    } else {
        Controller.loadURL(url)
    }
}