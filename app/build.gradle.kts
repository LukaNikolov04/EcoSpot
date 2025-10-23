
import java.util.Properties
import java.io.FileInputStream

// Kreiramo Properties objekat
val localProperties = Properties()
// Lociramo local.properties fajl u root direktorijumu projekta
val localPropertiesFile = rootProject.file("local.properties")

// Učitavamo fajl ako postoji
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}


plugins {
    id("com.android.application") // <-- Uklonjena verzija
    id("org.jetbrains.kotlin.android") // <-- Uklonjena verzija
    id("com.google.gms.google-services") // <-- Uklonjena verzija i apply false
}

android {
    namespace = "com.elfak.ecospot"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.elfak.ecospot"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Ova linija prosleđuje API ključ u AndroidManifest.xml
        manifestPlaceholders["MAPS_API_KEY"] = localProperties.getProperty("MAPS_API_KEY") ?: ""

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    composeOptions{
        kotlinCompilerExtensionVersion = "1.5.11" // Proverite i ispravite ovu verziju
    }
}

dependencies {

    // Koristimo stariji, ali stabilan BOM za Compose
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    // Uklanjamo kotlin-bom, jer BOM za compose to rešava

    // Vraćamo core-ktx na verziju kompatibilnu sa SDK 34
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    implementation("androidx.compose.material:material-icons-extended")

    // --- Naše dodate zavisnosti, usklađujemo ih ---
    implementation("com.google.firebase:firebase-auth-ktx") // Bez verzije, koristiće BOM
    implementation("com.google.firebase:firebase-firestore-ktx") // Bez verzije, koristiće BOM
    implementation("com.google.firebase:firebase-storage-ktx") // Bez verzije, koristiće BOM
    // Firebase Cloud Messaging
    implementation("com.google.firebase:firebase-messaging-ktx") // Koristimo BOM, pa ne treba verzija

    // Dodajemo Firebase BOM da upravlja verzijama
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))

    // Vraćamo navigaciju na verziju koja radi sa starim Compose-om
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Google Maps biblioteke koje smo dodali
    implementation("com.google.maps.android:maps-compose:4.3.3")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.2.0")

    // Test zavisnosti (ostaju iste)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")


}