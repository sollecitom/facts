plugins {
    id("sollecitom.kotlin-library-conventions")
}

dependencies {
    api(libs.swissknife.test.utils)
    api(projects.factsModulesClientKotlinApi)

    implementation(libs.swissknife.kotlin.extensions)
}