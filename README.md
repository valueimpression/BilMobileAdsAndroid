# BilMobileAdsAndroid

#### Step 1: Add the JitPack repository to your build file
- Add it in your root build.gradle at the end of repositories.
```gradle
    allprojects {
        repositories {
            ...
            maven { url 'https://jitpack.io' }
        }
    }
```
#### Step 2: In Project Explorer go to Gradle Scripts > build.gradle (Module: app) and add the following lines to the android { ... } section:
```gradle
    android {
        ...
        compileOptions 
        {
         sourceCompatibility JavaVersion.VERSION_1_8
         targetCompatibility JavaVersion.VERSION_1_8
        }
    }
```
#### Step 3: Add the dependency with latest version
```gradle
    dependencies {
        // ... other project dependencies

        // This will get the latest available version of the ValueImpression SDK.
        implementation 'org.prebid:prebid-mobile-sdk:2.1.3'
        implementation 'com.google.android.gms:play-services-ads:22.2.0'
        implementation 'com.github.valueimpression:BilMobileAdsAndroid:v2.2.3'
    }
```
## Note: Proguard 
Add this -keep directive to your apps' proguard-rules.pro:
```gradle
    -keep class com.bil.** { *; }
    -keep class org.prebid.mobile.** { *; }
    -keep class com.google.android.gms.ads.** { *; }
```
