#!/bin/bash
pandoc Takamaka.md -o Takamaka.tex "-fmarkdown-implicit_figures -o" --from=markdown --include-in-header mystylefile.tex --highlight-style=kate -V geometry:a4paper
sed -i 's/\\begin{verbatim}/\\begin{myverbatim}\n\\begin{verbatim}/g' Takamaka.tex
sed -i 's/\\end{verbatim}/\\end{verbatim}\n\\end{myverbatim}/g' Takamaka.tex
pdflatex Takamaka.tex
