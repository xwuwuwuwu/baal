FROM mcr.microsoft.com/azure-functions/dotnet:2.0 
# ------------------------------------------------------
# --- Install required tools
COPY ./sources.list /etc/apt/sources.list

RUN apt-get update
RUN apt-get -y install python3 build-essential file vim

# ------------------------------------------------------
# --- Android NDK
ENV ANDROID_NDK /opt/android-ndk-r11c

# download
COPY ./android-ndk-r11c.tar.gz /opt/android-ndk-r11c.tar.gz
# uncompress
RUN cd /opt && tar -xvf android-ndk-r11c.tar.gz && cd ~
# Delete unused prebuild images
RUN rm -rf /opt/android-ndk-r11c.tar.gz

RUN rm -rf $ANDROID_NDK/prebuilt/android-mips* 
# Delete obsolete Android platforms
RUN rm -rf $ANDROID_NDK/platforms/android-3 
RUN rm -rf $ANDROID_NDK/platforms/android-4 
RUN rm -rf $ANDROID_NDK/platforms/android-5 
RUN rm -rf $ANDROID_NDK/platforms/android-8 
RUN rm -rf $ANDROID_NDK/platforms/android-12 
RUN rm -rf $ANDROID_NDK/platforms/android-13 
RUN rm -rf $ANDROID_NDK/platforms/android-15 
RUN rm -rf $ANDROID_NDK/platforms/android-16 
# Delete unused platform sources
RUN rm -rf $ANDROID_NDK/sources/cxx-stl/gnu-libstdc++/4.9/libs/mips* 
RUN rm -rf $ANDROID_NDK/sources/cxx-stl/llvm-libc++/libs/mips* 
# Delete LLVM STL tests
RUN rm -rf $ANDROID_NDK/sources/cxx-stl/llvm-libc++/test 
# Delete unused toolchains
RUN rm -rf $ANDROID_NDK/toolchains/mips 
RUN rm -rf $ANDROID_NDK/build/core/toolchains/mips* 
# add to PATH
ENV PATH ${ANDROID_NDK}:${PATH}

# ------------------------------------------------------
# --- Cleanup and rev num

# Cleaning
RUN apt-get clean

CMD ["/bin/bash"]
