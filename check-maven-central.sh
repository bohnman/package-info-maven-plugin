#!/bin/bash

artifactId="$(cat pom.xml | grep '<artifactId>' | head -1 | cut -d'>' -f2 | cut -d'<' -f1)"
version="$1"

if [ "$version" == "" ]; then
    version=$(perl -0777 -ne 'print $1 if /<artifactId>'"${artifactId}"'<\/artifactId>.*?<version>(.*?)<\/version>/smg' README.md)
fi

echo -n "Checking Version $version For $artifactId: "

status=$(curl -s -o /dev/null -I -w "%{http_code}" http://central.maven.org/maven2/com/github/bohnman/${artifactId}/${version}/${artifactId}-${version}.jar)


if [ "$status" == "" ]; then
    echo "Error Unknown"
    exit 1
fi

if [ "$status" == "200" ]; then
    echo "Found"
    exit 0
fi

if [ "$status" == "404" ]; then
    echo "Not Found"
    exit 1
fi

echo "ERROR $status"
exit 1