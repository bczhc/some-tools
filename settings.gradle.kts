pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { setUrl("https://jitpack.io") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        @Suppress("DEPRECATION")
        jcenter {
            content {
                // https://github.com/reddit/IndicatorFastScroll/issues/45
                includeGroup("com.reddit")
            }
        }
        maven { setUrl("https://jitpack.io") }
    }
}

rootProject.name = "some-tools"
include(":app")
