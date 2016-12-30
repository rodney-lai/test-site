FROM        ubuntu:16.04

MAINTAINER  Rodney Lai <rlai@irismedia.com>

ENV         ACTIVATOR_VERSION 1.3.6
ENV         SBT_VERSION 0.13.12
ENV         DEBIAN_FRONTEND noninteractive

RUN         apt-get update

RUN         apt-get upgrade -y -q

RUN         apt-get install -y unzip

RUN         apt-get install -y wget

RUN         apt-get install -y openjdk-8-jre-headless

RUN         apt-get install -y openjdk-8-jdk

RUN         apt-get install -y scala

RUN         update-ca-certificates -f

RUN         useradd -ms /bin/bash play-user

USER        play-user

RUN         mkdir /home/play-user/tmp && \
            cd /home/play-user/tmp && \
            wget http://downloads.typesafe.com/typesafe-activator/$ACTIVATOR_VERSION/typesafe-activator-$ACTIVATOR_VERSION.zip && \
            unzip typesafe-activator-$ACTIVATOR_VERSION.zip -d /home/play-user && \
            mv /home/play-user/activator-dist-$ACTIVATOR_VERSION /home/play-user/activator && \
            rm typesafe-activator-$ACTIVATOR_VERSION.zip && \
            cd /home/play-user && \
            rm -rf /home/play-user/tmp

RUN         mkdir /home/play-user/tmp && \
            cd /home/play-user/tmp && \
            wget https://dl.bintray.com/sbt/native-packages/sbt/$SBT_VERSION/sbt-$SBT_VERSION.zip && \
            unzip sbt-$SBT_VERSION.zip -d /home/play-user && \
            cd /home/play-user && \
            rm -rf /home/play-user/tmp

ADD         lib /home/play-user/root/build/lib
ADD         home/app /home/play-user/root/build/home/app
ADD         home/test /home/play-user/root/build/home/test
ADD         home/conf /home/play-user/root/build/home/conf
ADD         home/public /home/play-user/root/build/home/public
ADD         home/build.sbt /home/play-user/root/build/home/
ADD         home/README /home/play-user/root/build/home/
ADD         home/project/plugins.sbt /home/play-user/root/build/home/project/
ADD         home/project/build.properties /home/play-user/root/build/home/project/
ADD         upload/app /home/play-user/root/build/upload/app
ADD         upload/test /home/play-user/root/build/upload/test
ADD         upload/conf /home/play-user/root/build/upload/conf
ADD         upload/public /home/play-user/root/build/upload/public
ADD         upload/build.sbt /home/play-user/root/build/upload/
ADD         upload/project/plugins.sbt /home/play-user/root/build/upload/project/
ADD         upload/project/build.properties /home/play-user/root/build/upload/project/
ADD         emailer/src /home/play-user/root/build/emailer/src
ADD         emailer/build.sbt /home/play-user/root/build/emailer/
ADD         emailer/project/build.properties /home/play-user/root/build/emailer/project/
ADD         emailer/project/assembly.sbt /home/play-user/root/build/emailer/project/

RUN         cp -r /home/play-user/root/build/ /home/play-user

USER        root

RUN         rm -rf /home/play-user/root

USER        play-user

RUN         cd /home/play-user/build/home; /home/play-user/activator/activator test stage
RUN         rm /home/play-user/build/home/target/universal/stage/bin/*.bat
RUN         mv /home/play-user/build/home/target/universal/stage /home/play-user/deploy-home
RUN         cd /home/play-user/build/upload; /home/play-user/activator/activator test stage
RUN         rm /home/play-user/build/upload/target/universal/stage/bin/*.bat
RUN         mv /home/play-user/build/upload/target/universal/stage /home/play-user/deploy-upload
RUN         cd /home/play-user/build/emailer; /home/play-user/activator/activator assembly
RUN         mkdir /home/play-user/deploy-emailer
RUN         mv /home/play-user/build/emailer/target/scala-2.11/*.jar /home/play-user/deploy-emailer
RUN         rm -rf /home/play-user/build

RUN         date > /home/play-user/deploy-home/BUILD_DATE
RUN         date > /home/play-user/deploy-upload/BUILD_DATE
RUN         date > /home/play-user/deploy-emailer/BUILD_DATE
RUN         echo -n "#!/usr/bin/scala " > /home/play-user/deploy-emailer/emailer
RUN         ls /home/play-user/deploy-emailer/emailer*.jar >> /home/play-user/deploy-emailer/emailer
RUN         chmod 755 /home/play-user/deploy-emailer/emailer

WORKDIR     /home/play-user/deploy-home/bin
ENTRYPOINT  ["./rodney-test-site-home","-Dhttps.port=9443"]
EXPOSE      9000 9443
