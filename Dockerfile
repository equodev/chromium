# This is produce shared lib and executable linked to older glibc(2.12) and compatible with older linux distros
FROM centos:6.6

RUN yum install -y java-1.8.0-openjdk-devel curl gcc gtk2-devel
RUN curl https://sh.rustup.rs -sSf | sh -s -- -y
ENV PATH=$PATH:/root/.cargo/bin
ENV JAVA_HOME=/usr/lib/jvm/java-1.8.0/

#COPY cefrust/Cargo* /src/cefrust/
#WORKDIR /src/cefrust
#COPY cefrust /src/cefrust
#RUN cargo check
#RUN cc --help
#RUN cargo build --bin cefrust_subp --release
#WORKDIR /src/cefrust/cefrustlib
#RUN cargo build  --release

COPY cefswt/gradle* /src/cefswt/
COPY cefswt/gradle /src/cefswt/gradle
WORKDIR /src/cefswt

# cause gradlew to download gradle and docker to cache this step
RUN ./gradlew tasks

COPY cefswt/build.gradle /src/cefswt/
COPY cefrust/Cargo* /src/cefrust/
RUN ./gradlew getCefAndUnzip

#change_order_first rust
COPY cefswt /src/cefswt
COPY cefrust /src/cefrust

#TODO move top
RUN yum install -y libXScrnSaver GConf2-devel

RUN ./gradlew buildCefRust --stacktrace

RUN ./gradlew buildSampleE4 --stacktrace

RUN yum install -y unzip xorg-x11-server-Xvfb
