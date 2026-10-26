plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hotswan.compiler)

}

android {
    namespace = "com.example.diaryapp"
    compileSdk = 36

    packaging {
        resources {
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/license.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
            excludes += "/META-INF/notice.txt"
            excludes += "/META-INF/ASL2.0"
            excludes += "/META-INF/*.kotlin_module"
        }
    }

    defaultConfig {
        applicationId = "com.example.diaryapp"
        minSdk = 26
        targetSdk = 36
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
    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

// Room 스키마를 JSON으로 내보내 버전별 변경 이력을 남긴다 (Migration 작성·검증의 기준)
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // 구글 로그인 및 인증 (최신 버전 사용)
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Credential Manager 라이브러리
    implementation("androidx.credentials:credentials:1.2.2")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.2")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // 구글 API 클라이언트 라이브러리 (캘린더 통신용)
    implementation("com.google.api-client:google-api-client-android:2.0.0") {
        exclude(group = "org.apache.httpcomponents")
    }

    // 구글 캘린더 서비스 라이브러리
    implementation("com.google.apis:google-api-services-calendar:v3-rev20220715-2.0.0")

    // 코루틴 (비동기 작업을 위해 필요)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // Hilt + Navigation Compose
    implementation(libs.hilt.navigation.compose)

    // DataStore - Preferences 기반
    implementation(libs.datastore.preferences)

    implementation(libs.kotlinx.serialization.json)
    // 비밀번호 해싱
    implementation("org.mindrot:jbcrypt:0.4")

    // Compose BOM: 버전 자동 동기화
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))

    // Compose 핵심 UI 구성요소 (relying on libs aliases)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.ui:ui-text-google-fonts")
    // 디버깅 / 미리보기용
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Lifecycle, 코루틴 등 유틸
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
}