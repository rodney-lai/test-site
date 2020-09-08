Rodney's Test Site v0.9.0
=========================

[![Build Status](https://travis-ci.org/rodney-lai/test-site.svg?branch=master)](https://travis-ci.org/rodney-lai/test-site)

rlai [ at ] irismedia [ dot ] com

[Live Test App](https://app.rodneylai.com)

[Live Test Site](https://test.rodneylai.com)

[GitHub Repository](https://github.com/rodney-lai)

[BitBucket Repository](https://bitbucket.org/rodney-lai)

[DockerHub Repository](https://hub.docker.com/u/rodneylai/)

[Quay Docker Repository](https://quay.io/user/rodney-lai)

Features
--------

* User Accounts
* User Roles
* Page View Logging
* Admin Portal to manage users
* Developer Portal to manage web server, memcached, and mongodb
* Interactive API documentation
* Email Queue (Redis)
* Email Templates

Quick Start
-----------

By default, site is configured to run WITHOUT MongoDB and MemCached.

MongoDB is required to create and modify users.  The three default test users will still be available without MongoDB.
Test users are test-user, test-admin, and test-developer

To activate MemCached refer to the application.conf file.

To run local:

1. Clone repository
2. Change to 'home' directory in repository.
3. Run Play 'activator -DPLAY_TEST_PASSWORD=[*my_password*] run'

To run the docker container:

1. Build and deploy docker image using provided Dockerfile.  Refer to docker documentation
2. Run Docker Image  
'docker run -p 80:9000 -p 443:9443 -e "PLAY_TEST_PASSWORD=[*my_password*]" -d [*my_docker_repository*]'

Docker Image Entrypoints:

All the projects are packaged in a single docker image.
To change the project, change the working directory and
entrypoint when running the docker image.

By default the 'home' project runs.

To run the 'upload' project,
set work directory to /home/deploy-user/deploy-upload/bin
and entrypoint to ./rodney-test-site-upload

To run the 'emailer' project
set work directory to /home/deploy-user/deploy-emailer/bin
and entrypoint to ./emailer

There is a docker compose file to startup a local memcached,
redis, postgres and mongodb.

Environment Variables:

PLAY_TEST_PASSWORD - password for test user accounts (required for ALL builds)  
PLAY_APPLICATION_SECRET - play crytographic secret.  Refer to Play Framework documentation on creating an ApplicationSecret (required for Prod/Docker build)  

Environment Variables for MongoDB:

MONGO_HOST - mongo host  
MONGO_PORT - mongo port  
MONGO_USERNAME - mongo user name  
MONGO_PASSWORD - mongo user password  
MONGO_DATABASE - mongo database name  
MONGO_AUTHMECHANISM - mongo auth mechanism [ SCRAM-SHA-1 or SCRAM-SHA-256 ]  

Environment Variables for MemCached:

MEMCACHED_HOST - memcached host  
MEMCACHED_USER - memcached user  
MEMCACHED_PASSWORD - memcached password  

Environment Variables for Redis:

REDIS_HOST - redis host  
REDIS_PORT - redis port  
REDIS_PASSWORD - redis password  

Environment Variables for Emails:

EMAIL_HOST - email smtp host  
EMAIL_PORT - email smtp port  
EMAIL_USERNAME - email smtp user name  
EMAIL_PASSWORD - email smtp password  
EMAIL_FROM_EMAIL - from email address  
EMAIL_FROM_NAME - from email name  

Server Technology
-----------------

* Web Framework - [Play Framework v2.5.19](https://playframework.com/)
* Language - [Scala v2.11.12](http://scala-lang.org/)
* NoSQL Database - [MongoDB](https://www.mongodb.org/)
* Data Cache - [Memcached](http://memcached.org/)
* Data Structure Store - [Redis](https://redis.io/)

Backend Libraries
-----------------

* Authentication - [Play2-Auth](https://github.com/t2v/play2-auth)
* Authorization - [Deadbolt 2](https://github.com/schaloner/deadbolt-2)
* Password Encryption - [jBCrypt](http://www.mindrot.org/projects/jBCrypt/)
* API Documentation - [Swagger](http://swagger.io/)
* Computer Vision - [OpenCV](http://opencv.org/)

Frontend Libraries
------------------

* DOM Library - [jQuery](http://jquery.com/)
* MVC Framework - [backbone.js](http://backbonejs.org/)
* Utility Library - [underscore.js](http://underscorejs.org/)
* Data Binding for Backbone - [epoxy.js](http://epoxyjs.org/)
* Backbone Framework - [marionette.js](http://marionettejs.com/)
* Data Visualization - [plotly](https://plot.ly)
* React Framework - [React](https://facebook.github.io/react/)

Build, Deploy, Hosting
----------------------

* Build and Deploy - [Docker](https://www.docker.com/)
* Web Server Hosting - [Amazon Web Services (AWS)](http://aws.amazon.com/)
* Frontend Http Server - [Nginx](https://www.nginx.com/)
* MongoDB Hosting - [mLab](https://mlab.com/)
* MemCached/Redis Hosting - [RedisLabs](https://redislabs.com/)
* Logging - [Papertrail](https://papertrailapp.com/)
* Monitoring - [UptimeRobot](http://uptimerobot.com/)
* SSL Certificate Authority - [LetsEncrypt](https://letsencrypt.org/)

Tools
-----

* Editor - [Atom](https://atom.io/)
* Mongo GUI Client - [MongoChef](http://3t.io/)
* Redis GUI Client - [Redis Desktop Manager](https://redisdesktop.com/)

Copyright (c) 2015-2020 Rodney S.K. Lai

Permission to use, copy, modify, and/or distribute this software for any purpose with or without fee is hereby granted, provided that the above copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
