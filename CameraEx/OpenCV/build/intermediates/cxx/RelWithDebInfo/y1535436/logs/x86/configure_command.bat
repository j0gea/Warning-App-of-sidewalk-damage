@echo off
"C:\\CookAndroid\\SDK\\cmake\\3.18.1\\bin\\cmake.exe" ^
  "-HC:\\CookAndroid\\Project\\capston\\Warning-App-of-sidewalk-damage\\CameraEx\\OpenCV\\libcxx_helper" ^
  "-DCMAKE_SYSTEM_NAME=Android" ^
  "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON" ^
  "-DCMAKE_SYSTEM_VERSION=21" ^
  "-DANDROID_PLATFORM=android-21" ^
  "-DANDROID_ABI=x86" ^
  "-DCMAKE_ANDROID_ARCH_ABI=x86" ^
  "-DANDROID_NDK=C:\\CookAndroid\\SDK\\ndk\\23.1.7779620" ^
  "-DCMAKE_ANDROID_NDK=C:\\CookAndroid\\SDK\\ndk\\23.1.7779620" ^
  "-DCMAKE_TOOLCHAIN_FILE=C:\\CookAndroid\\SDK\\ndk\\23.1.7779620\\build\\cmake\\android.toolchain.cmake" ^
  "-DCMAKE_MAKE_PROGRAM=C:\\CookAndroid\\SDK\\cmake\\3.18.1\\bin\\ninja.exe" ^
  "-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=C:\\CookAndroid\\Project\\capston\\Warning-App-of-sidewalk-damage\\CameraEx\\OpenCV\\build\\intermediates\\cxx\\RelWithDebInfo\\y1535436\\obj\\x86" ^
  "-DCMAKE_RUNTIME_OUTPUT_DIRECTORY=C:\\CookAndroid\\Project\\capston\\Warning-App-of-sidewalk-damage\\CameraEx\\OpenCV\\build\\intermediates\\cxx\\RelWithDebInfo\\y1535436\\obj\\x86" ^
  "-DCMAKE_BUILD_TYPE=RelWithDebInfo" ^
  "-BC:\\CookAndroid\\Project\\capston\\Warning-App-of-sidewalk-damage\\CameraEx\\OpenCV\\.cxx\\RelWithDebInfo\\y1535436\\x86" ^
  -GNinja ^
  "-DANDROID_STL=c++_shared"
