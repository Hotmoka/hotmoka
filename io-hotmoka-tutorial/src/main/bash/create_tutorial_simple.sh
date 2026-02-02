#!/bin/bash

# This script compiles the tutorial, generating
# a book at target/pdf/tutorial.pdf
# This book misses links and references, but is quick to generate.
# The idea is that this script is used during the editing
# of the tutorial, so that one does not waste a lot of time
# for recompilation before seeing potential errors.

mkdir -p target/pdf
rm -r target/pdf/*
cp src/main/latex/*.tex target/pdf
cp -r src/main/resources/pics target/pdf/tutorial-images
cd target/pdf
pdflatex tutorial.tex
cd ../..