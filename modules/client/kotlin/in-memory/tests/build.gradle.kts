plugins {
    id("sollecitom.kotlin-library-conventions")
}

dependencies {
    testImplementation(projects.factsModulesClientKotlinInMemoryImplementation)
    testImplementation(projects.factsModulesClientKotlinTestSpecification)
}