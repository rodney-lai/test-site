FROM        ubuntu:20.04 AS BUILD_IMAGE

MAINTAINER  Rodney Lai <rlai@irismedia.com>

ENV         ACTIVATOR_VERSION 1.3.6
ENV         SBT_VERSION 1.3.13
ENV         DEBIAN_FRONTEND noninteractive

RUN         apt-get update

RUN         apt-get upgrade -y -q

RUN         apt-get install -y wget

RUN         apt-get install -y unzip

RUN         apt-get install -y openjdk-8-jre-headless

RUN         apt-get install -y openjdk-8-jdk

RUN         apt-get install -y scala

RUN         update-ca-certificates -f

RUN         useradd -ms /bin/bash deploy-user

USER        deploy-user

RUN         mkdir /home/deploy-user/tmp && \
            cd /home/deploy-user/tmp && \
            wget http://downloads.typesafe.com/typesafe-activator/$ACTIVATOR_VERSION/typesafe-activator-$ACTIVATOR_VERSION.zip && \
            unzip typesafe-activator-$ACTIVATOR_VERSION.zip -d /home/deploy-user && \
            mv /home/deploy-user/activator-dist-$ACTIVATOR_VERSION /home/deploy-user/activator && \
            rm typesafe-activator-$ACTIVATOR_VERSION.zip && \
            cd /home/deploy-user && \
            rm -rf /home/deploy-user/tmp

RUN         mkdir /home/deploy-user/tmp && \
            cd /home/deploy-user/tmp && \
            wget https://piccolo.link/sbt-$SBT_VERSION.zip && \
            unzip sbt-$SBT_VERSION.zip -d /home/deploy-user && \
            cd /home/deploy-user && \
            rm -rf /home/deploy-user/tmp

ADD         lib /home/deploy-user/root/build/lib
ADD         home/app /home/deploy-user/root/build/home/app
ADD         home/test /home/deploy-user/root/build/home/test
ADD         home/conf /home/deploy-user/root/build/home/conf
ADD         home/public /home/deploy-user/root/build/home/public
ADD         home/build.sbt /home/deploy-user/root/build/home/
ADD         home/README /home/deploy-user/root/build/home/
ADD         home/project/plugins.sbt /home/deploy-user/root/build/home/project/
ADD         home/project/build.properties /home/deploy-user/root/build/home/project/
ADD         upload/app /home/deploy-user/root/build/upload/app
ADD         upload/test /home/deploy-user/root/build/upload/test
ADD         upload/conf /home/deploy-user/root/build/upload/conf
ADD         upload/public /home/deploy-user/root/build/upload/public
ADD         upload/build.sbt /home/deploy-user/root/build/upload/
ADD         upload/project/plugins.sbt /home/deploy-user/root/build/upload/project/
ADD         upload/project/build.properties /home/deploy-user/root/build/upload/project/
ADD         emailer/src /home/deploy-user/root/build/emailer/src
ADD         emailer/build.sbt /home/deploy-user/root/build/emailer/
ADD         emailer/project/build.properties /home/deploy-user/root/build/emailer/project/
ADD         emailer/project/assembly.sbt /home/deploy-user/root/build/emailer/project/

RUN         cp -r /home/deploy-user/root/build/ /home/deploy-user

USER        root

RUN         rm -rf /home/deploy-user/root

USER        deploy-user

RUN         cd /home/deploy-user/build/home; /home/deploy-user/activator/activator test stage
RUN         rm /home/deploy-user/build/home/target/universal/stage/bin/*.bat
RUN         mv /home/deploy-user/build/home/target/universal/stage /home/deploy-user/deploy-home
RUN         cd /home/deploy-user/build/upload; /home/deploy-user/sbt/bin/sbt test stage
RUN         rm /home/deploy-user/build/upload/target/universal/stage/bin/*.bat
RUN         mv /home/deploy-user/build/upload/target/universal/stage /home/deploy-user/deploy-upload
RUN         cd /home/deploy-user/build/emailer; /home/deploy-user/sbt/bin/sbt assembly
RUN         mkdir /home/deploy-user/deploy-emailer
RUN         mv /home/deploy-user/build/emailer/target/scala-2.13/*.jar /home/deploy-user/deploy-emailer
RUN         rm -rf /home/deploy-user/build

FROM        ubuntu:20.04

MAINTAINER  Rodney Lai <rlai@irismedia.com>

ENV         DEBIAN_FRONTEND noninteractive

RUN         apt-get update

RUN         apt-get upgrade -y -q

RUN         apt-get install -y openjdk-8-jre-headless

RUN         apt-get install -y scala

RUN         useradd -ms /bin/bash deploy-user

USER        deploy-user

COPY        --from=BUILD_IMAGE --chown=deploy-user:deploy-user /home/deploy-user/deploy-home /home/deploy-user/deploy-home
COPY        --from=BUILD_IMAGE --chown=deploy-user:deploy-user /home/deploy-user/deploy-upload /home/deploy-user/deploy-upload
COPY        --from=BUILD_IMAGE --chown=deploy-user:deploy-user /home/deploy-user/deploy-emailer /home/deploy-user/deploy-emailer

RUN         date > /home/deploy-user/deploy-home/BUILD_DATE
RUN         date > /home/deploy-user/deploy-upload/BUILD_DATE
RUN         date > /home/deploy-user/deploy-emailer/BUILD_DATE
RUN         echo -n "#!/usr/bin/scala " > /home/deploy-user/deploy-emailer/emailer
RUN         ls /home/deploy-user/deploy-emailer/emailer*.jar >> /home/deploy-user/deploy-emailer/emailer
RUN         chmod 755 /home/deploy-user/deploy-emailer/emailer

RUN         ls -la /home/deploy-user
RUN         ls -la /home/deploy-user/deploy-home
RUN         ls -la /home/deploy-user/deploy-home/bin
RUN         ls -la /home/deploy-user/deploy-upload
RUN         ls -la /home/deploy-user/deploy-upload/bin
RUN         ls -la /home/deploy-user/deploy-emailer

WORKDIR     /home/deploy-user/deploy-home/bin
ENTRYPOINT  ["./rodney-test-site-home","-Dhttps.port=9443"]
EXPOSE      9000 9443
