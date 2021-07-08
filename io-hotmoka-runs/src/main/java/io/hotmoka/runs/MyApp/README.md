# HotmokaAndroidDemo
An Android sample to show the usage of the io-hotmoka-network-thin-client-kt library

### Note
In order to use the application you have to:
* launch the hotmoka initialized node by going to go to parent folder of the hotmoka project and running the cmd   
  `sh run_network_initialized_memory_empty_signature`
  
* copy the gamete node storage reference and the basicjar node storage reference and
  
* set the gamete node storage reference of the variable gamete of MainActivity.kt
  ```java 
  private val gamete = StorageReferenceModel(
      TransactionReferenceModel(
          "local",
          "" // <- hash of gamete goes here
      ),
      "0"
  )
* set the basicjar node storage reference of the variable basicjar of MainActivity.kt
  ```java
  private val basicJar = TransactionReferenceModel(
        "local",
        "" // <- hash of basicjar goes here
  )


### Android Application
The following items have been added to the application in order to make it work:
* AndroidManifest internet permission  
  `<uses-permission android:name="android.permission.INTERNET" />`
* AndroidManifest security config  
  `android:networkSecurityConfig="@xml/network_security_config"`
* dependencies of build.gradle of app  
  `implementation "com.squareup.okhttp3:okhttp:4.9.0"`   
  `implementation "com.google.code.gson:gson:2.8.6"`  
  `implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version"`  
  `implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.4.2"`
* io-hotmoka-network-thin-client-kt-1.0-SNAPSHOT.jar library inside the lib folder
  