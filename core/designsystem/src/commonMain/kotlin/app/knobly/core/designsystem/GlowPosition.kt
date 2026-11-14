package app.knobly.core.designsystem

/** Where the background's soft glow sits; screens alternate it to avoid a repetitive look. */
enum class GlowPosition(
    internal val x: Float,
    internal val y: Float,
) {
    TopStart(0.15f, 0.08f),
    TopEnd(0.85f, 0.12f),
    Center(0.5f, 0.25f),
}
