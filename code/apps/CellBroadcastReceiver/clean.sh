#!/bin/sh 

rm -rf ${1}/src/*
rm -rf ${1}/res/*
rm -rf ${1}/AndroidManifest.xml

git checkout -- ${1}/src/*
git checkout -- ${1}/res/*
git checkout -- ${1}/AndroidManifest.xml
