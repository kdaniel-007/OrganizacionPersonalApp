plugins {
    id("com.android.application")
    id("com.google.gms.google-services") // Plugin para Firebase
}

android {
    namespace = "com.example.OrganizacionPersonal"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.OrganizacionPersonal"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:deprecation")
}


dependencies {
    // Firebase BOM (Bill of Materials) - ¡IMPORTANTE! Lo añadimos para versiones estables de Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.0.0")) // Versión actual a Mayo 2025

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity:1.10.1")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")

    // Dependencias de Firebase
    implementation("com.google.firebase:firebase-storage:20.3.0")
    implementation("com.google.firebase:firebase-analytics:21.5.0")
    implementation("com.google.firebase:firebase-auth") // Usa la versión del BOM
    implementation("com.google.android.gms:play-services-auth:21.1.0")
    implementation(libs.firebase.firestore) // Google Sign-In

    // Removida la dependencia de androidsvg-aar:1.4

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    implementation("androidx.recyclerview:recyclerview:1.3.0")
}