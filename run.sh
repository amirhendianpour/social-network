#!/bin/bash
cd /opt/social/social-network
set -a
source .env
set +a
java -jar target/social-0.0.1-SNAPSHOT.jar