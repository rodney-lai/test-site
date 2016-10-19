Rodney's Test Site v0.8.2
=========================

rlai [ at ] irismedia [ dot ] com

[Live Test Site](http://test.rodneylai.com)

[GitHub Repository](https://github.com/rodney-lai)

Features
--------

* User Accounts
* User Roles
* Page View Logging
* Admin Portal to manage users
* Developer Portal to manage web server, memcached, and mongodb
* Interactive API documentation

Quick Start
-----------

This project uses an implementation of the swagger-play library for the Play Framework v2.5 which
has not been merged into the main project.  You will have to download and build the project locally.

https://github.com/CreditCardsCom/swagger-play

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

Environment Variables:

PLAY_TEST_PASSWORD - password for test user accounts (required for ALL builds)  
PLAY_APPLICATION_SECRET - play crytographic secret.  Refer to Play Framework documentation on creating an ApplicationSecret (required for Prod/Docker build)  

Environment Variables for MongoDB:

PLAY_MONGO_HOST - mongo host  
PLAY_MONGO_PORT - mongo port  
PLAY_MONGO_USER_NAME - mongo user name  
PLAY_MONGO_PASSWORD - mongo user password  
PLAY_MONGO_DATABASE - mongo database name  
PLAY_MONGO_AUTHMECHANISM - mongo auth mechanism [ MONGODB-CR or SCRAM-SHA-1 ]  

Server Technology
-----------------

* Web Framework - [Play Framework v2.5.9](https://playframework.com/)
* Language - [Scala v2.11.8](http://scala-lang.org/)
* NoSQL Database - [MongoDB](https://www.mongodb.org/)
* Data Cache - [Memcached](http://memcached.org/)

Backend Libraries
-----------------

* Authentication - [Play2-Auth](https://github.com/t2v/play2-auth)
* Authorization - [Deadbolt 2](https://github.com/schaloner/deadbolt-2)
* Password Encryption - [jBCrypt](http://www.mindrot.org/projects/jBCrypt/)
* API Documentation - [Swagger](http://swagger.io/)

Frontend Libraries
------------------

* DOM Library - [jQuery](http://jquery.com/)
* MVC Framework - [backbone.js](http://backbonejs.org/)
* Utility Library - [underscore.js](http://underscorejs.org/)
* Data Binding for Backbone - [epoxy.js](http://epoxyjs.org/)
* Backbone Framework - [marionette.js](http://marionettejs.com/)

Build, Deploy, Hosting
----------------------

* Build and Deploy - [Docker](https://www.docker.com/)
* Web Server Hosting - [Amazon Web Services (AWS)](http://aws.amazon.com/)
* Frontend Http Server - [Nginx](https://www.nginx.com/)
* MongoDB Hosting - [mLab](https://mlab.com/)
* MemCached Hosting - [RedisLabs](https://redislabs.com/)
* Logging - [Papertrail](https://papertrailapp.com/)
* Monitoring - [UptimeRobot](http://uptimerobot.com/)
* SSL Certificate Authority - [LetsEncrypt](https://letsencrypt.org/)

Tools
-----

* Editor - [Atom](https://atom.io/)
* Mongo GUI Client - [MongoChef](http://3t.io/)

Copyright (c) 2015-2016 Rodney S.K. Lai

Permission to use, copy, modify, and/or distribute this software for any purpose with or without fee is hereby granted, provided that the above copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
