dependencies {
    implementation(project(":common"))
    compileOnly(group = "org.spigotmc", name = "spigot", version = "1.17.1-R0.1-SNAPSHOT")
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}