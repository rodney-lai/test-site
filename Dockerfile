FROM        ubuntu:15.10

MAINTAINER  Rodney Lai <rlai@irismedia.com>

ENV			ACTIVATOR_VERSION 1.3.6
ENV			DEBIAN_FRONTEND noninteractive

RUN			apt-get update

RUN			apt-get upgrade -y -q

RUN			apt-get install -y unzip

RUN			apt-get install -y wget

RUN			apt-get install -y openjdk-8-jre-headless

RUN			apt-get install -y openjdk-8-jdk

RUN			update-ca-certificates -f

RUN			useradd -ms /bin/bash play-user

USER		play-user

RUN			mkdir /home/play-user/tmp && \
			cd /home/play-user/tmp && \
			wget http://downloads.typesafe.com/typesafe-activator/$ACTIVATOR_VERSION/typesafe-activator-$ACTIVATOR_VERSION.zip && \
			unzip typesafe-activator-$ACTIVATOR_VERSION.zip -d /home/play-user && \
			mv /home/play-user/activator-dist-$ACTIVATOR_VERSION /home/play-user/activator && \
			rm typesafe-activator-$ACTIVATOR_VERSION.zip && \
			cd /home/play-user && \
			rm -rf /home/play-user/tmp


ADD			lib /home/play-user/root/build/lib
ADD			home/app /home/play-user/root/build/home/app
ADD			home/test /home/play-user/root/build/home/test
ADD			home/conf /home/play-user/root/build/home/conf
ADD			home/public /home/play-user/root/build/home/public
ADD			home/build.sbt /home/play-user/root/build/home/
ADD			home/README /home/play-user/root/build/home/
ADD			home/project/plugins.sbt /home/play-user/root/build/home/project/
ADD			home/project/build.properties /home/play-user/root/build/home/project/

RUN			cp -r /home/play-user/root/build/ /home/play-user

USER		root

RUN			rm -rf /home/play-user/root

USER		play-user

RUN			cd /home/play-user/build/home; /home/play-user/activator/activator stage
RUN			rm /home/play-user/build/home/target/universal/stage/bin/*.bat
RUN			mv /home/play-user/build/home/target/universal/stage /home/play-user/deploy
RUN			rm -rf /home/play-user/build

RUN			date > /home/play-user/deploy/BUILD_DATE

RUN			ls -la /home/play-user
RUN			ls -la /home/play-user/deploy
RUN			ls -la /home/play-user/deploy/bin

WORKDIR		/home/play-user/deploy/bin
ENTRYPOINT	["./rodney-test-site-home","-Dhttps.port=9443"]
EXPOSE		9000 9443
