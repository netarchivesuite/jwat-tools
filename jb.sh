#!/bin/sh
mvn -U clean install
cd target
unzip jwat-tools-*.zip
cd ..
