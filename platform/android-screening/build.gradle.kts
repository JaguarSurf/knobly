plugins {
    alias(libs.plugins.knobly.android.library)
}

dependencies {
    api(projects.domain.rules)
    api(projects.core.phone)
    implementation(libs.androidx.activity)
    implementation(libs.koin.android)
    implementation(libs.kotlinx.coroutines.core)
}
