plugins {
    id("sollecitom.kotlin-library-conventions")
}

dependencies {
    api(projects.factsModulesClientKotlinApi)

    implementation(libs.swissknife.kotlin.extensions)
}