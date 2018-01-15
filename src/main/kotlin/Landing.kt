import kotlin.browser.window

@JsName("start")
fun start() {
    Controller.loadURL(window.location.search.substring(3))
}