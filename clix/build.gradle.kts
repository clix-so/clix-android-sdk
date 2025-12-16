plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ktfmt.gradle)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.jacoco)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.jreleaser)
}

version = libs.versions.clix.get()

android {
    namespace = "so.clix"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        aarMetadata { minCompileSdk = libs.versions.minCompileSdk.get().toInt() }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("String", "VERSION", "\"${version}\"")
    }
    buildFeatures { buildConfig = true }

    buildTypes {
        debug { enableUnitTestCoverage = true }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions { jvmTarget = "1.8" }
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
    testOptions {
        animationsDisabled = true
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.systemProperty("robolectric.enabledSdks", "28")
                it.jvmArgs = listOf("-noverify")
            }
        }
    }

    testCoverage { jacocoVersion = libs.versions.jacoco.get() }
}

ktfmt { kotlinLangStyle() }

detekt {
    buildUponDefaultConfig = true
    allRules = false
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports { xml.required.set(true) }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.bundles.coroutines)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.firebase.messaging.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
}

tasks.withType<Test>().configureEach {
    extensions.configure(JacocoTaskExtension::class) {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

tasks.withType<JacocoReport>().configureEach {
    val fileFilter =
        listOf(
            "**/R.class",
            "**/R$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*Test*.*",
            "android/**/*.*",
        )

    val mainSrc = "${project.projectDir}/src/main/kotlin"

    val debugTree =
        fileTree(
            mapOf(
                "dir" to "${layout.buildDirectory}/tmp/kotlin-classes/debug",
                "excludes" to fileFilter,
            )
        )
    sourceDirectories.setFrom(files(listOf(mainSrc)))
    classDirectories.setFrom(files(listOf(debugTree)))
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "so.clix"
            artifactId = "clix-android-sdk"
            version = version

            afterEvaluate { from(components["release"]) }

            pom {
                name.set("Clix Android SDK")
                description.set("Notification and push SDK for Android")
                url.set("https://github.com/clix-so/clix-android-sdk")
                licenses {
                    license {
                        name.set("MIT License with Custom Restrictions")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("clix")
                        name.set("Clix Team")
                        email.set("team@clix.so")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/clix-so/clix-android-sdk.git")
                    developerConnection.set("scm:git:ssh://github.com/clix-so/clix-android-sdk.git")
                    url.set("https://github.com/clix-so/clix-android-sdk")
                }
            }
        }
    }

    repositories { maven(layout.buildDirectory.dir("target/staging-deploy")) }
}

jreleaser {
    gitRootSearch = true
    environment { setVariables("${rootDir}/local.properties") }
    signing {
        active = org.jreleaser.model.Active.ALWAYS
        armored = true
    }
    deploy {
        maven {
            mavenCentral {
                register("sonatype") {
                    active = org.jreleaser.model.Active.ALWAYS
                    url = "https://central.sonatype.com/api/v1/publisher"
                    verifyPom = false
                    stagingRepository(
                        layout.buildDirectory.dir("target/staging-deploy").get().asFile.path
                    )
                }
            }
        }
    }
}
