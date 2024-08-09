#!/bin/sh

folder=build/dist/wasmJs/productionExecutable
rm webapp/kijijune*
gradle clean build || exit
for f in $(ls $folder); do
    echo copying $f ...
    cp $folder/$f webapp/$f
done
unzip build/distributions/*.zip -d build || exit

