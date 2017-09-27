# This is produce shared lib and executable linked to older glibc(2.12) and compatible with older linux distros

# Usage:
# ln -s $PWD/.dockerignore $PWD/../.dockerignore
# docker build -t cef -f Dockerfile ..
# docker run --name cef -d cef bash
# docker cp cef:/src/cefswt/com.make.cef.linux.x86_64/linux-x86_64/cefrust_subp ./com.make.cef.linux.x86_64/linux-x86_64/
# docker cp cef:/src/cefswt/com.make.cef.linux.x86_64/linux-x86_64/libcefrustlib.so ./com.make.cef.linux.x86_64/linux-x86_64/
# docker cp cef:/src/cefswt/com.make.cef.linux.x86_64/linux-x86_64/files.properties ./com.make.cef.linux.x86_64/linux-x86_64/
# docker cp cef:/src/cefswt/com.make.cef.linux.x86_64/linux-x86_64/checksum ./com.make.cef.linux.x86_64/linux-x86_64/

FROM centos:6.6

RUN yum install -y java-1.8.0-openjdk-devel curl gcc gtk2-devel 
# for running
RUN yum install -y libXScrnSaver GConf2-devel unzip tk libvncserver xorg-x11-server-Xvfb xorg-x11-server-Xorg dejavu-sans-fonts wget
RUN wget ftp://195.220.108.108/linux/epel/6/x86_64/x11vnc-0.9.13-8.el6.x86_64.rpm \
  && rpm -i x11vnc-0.9.13-8.el6.x86_64.rpm 

RUN curl https://sh.rustup.rs -sSf | sh -s -- -y
ENV PATH=$PATH:/root/.cargo/bin
ENV JAVA_HOME=/usr/lib/jvm/java-1.8.0/

COPY cefswt/patchelf /usr/local/bin/patchelf
#COPY build_in_docker.sh /usr/local/bin/build_in_docker.sh

COPY cefswt/gradle* /src/cefswt/
COPY cefswt/gradle /src/cefswt/gradle
WORKDIR /src/cefswt

# cause gradlew to download gradle and docker to cache this step
RUN ./gradlew tasks

COPY cefswt/build.gradle /src/cefswt/
COPY cefrust/Cargo* /src/cefrust/
RUN ./gradlew getCefAndUnzip

COPY cefrust /src/cefrust
COPY cefswt /src/cefswt

RUN ./gradlew buildCefRust --stacktrace

RUN ./gradlew buildSampleE4 --stacktrace

#CMD /usr/local/bin/build_in_docker.sh

#RUN Xvfb :1 -screen 0 1024x768x24 +extension GLX +render -noreset >> xsession.log 2>&1 &
#ENV DISPLAY=:1
#RUN x11vnc -display :1 -bg -nopw -xkb -noclipboard -nosetclipboard