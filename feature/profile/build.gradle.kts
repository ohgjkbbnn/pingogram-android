plugins {
    alias(libs.plugins.pingogram.android.feature)
}

dependencies {
    // The system photo picker for a group's or channel's photo (Activity Result API; no permission).
    implementation(libs.androidx.activity.compose)
    // The window size class and the supporting pane layout: the shared media beside the profile on expanded windows.
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.layout)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    // The profile's music plays on an ExoPlayer owned by a MediaSessionService, so it keeps playing outside the
    // profile; the app controls it through a MediaController.
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)

    // ProfileViewModel is tested through the real TDLib repositories on FakeTdlibClient (docs/TESTING.md).
    testImplementation(projects.core.data)
    testImplementation(projects.core.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
