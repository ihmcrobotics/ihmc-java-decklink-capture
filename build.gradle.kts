plugins {
   id("us.ihmc.ihmc-build") version "0.22.0"
   id("us.ihmc.ihmc-ci") version "7.0"
   id("us.ihmc.ihmc-cd") version "1.14"
}

ihmc {
   group = "us.ihmc"
   version = "0.3.3"
   vcsUrl = "https://github.com/ihmcrobotics/ihmc-java-decklink-capture"
   openSource = true

   configureDependencyResolution()
   javaDirectory("main", "../../src")
   resourceDirectory("main", "../../resources")
   configurePublications()
}

app.entrypoint("Stream", "us.ihmc.javadecklink.Stream")

mainDependencies {
   api("us.ihmc:ihmc-native-library-loader:1.2.1")
   api("com.martiansoftware:jsap:2.1")
}

val captureDirectory = "/home/shadylady/IHMCJavaDeckLinkCapture"

tasks.create("deploy")
{
   dependsOn("installDist")

   doLast {
      remote.session("logger", "shadylady")
      {
         exec("mkdir -p $captureDirectory")

         exec("rm -rf $captureDirectory/bin")
         exec("rm -rf $captureDirectory/lib")

         put(file("build/install/ihmc-java-decklink-capture/bin").toString(), "$captureDirectory/bin")
         put(file("build/install/ihmc-java-decklink-capture/lib").toString(), "$captureDirectory/lib")

         exec("chmod +x $captureDirectory/bin/Stream")
      }
   }
}
