# R8 rules for the Knobly Android app.
# Libraries (Compose, Koin, kotlinx.serialization, AndroidX) ship their own consumer rules.
# The CallScreeningService is referenced from the manifest, so R8 keeps it automatically.
