#!/bin/bash
pandoc Takamaka.source --from=markdown --filter pandoc-fignos -o ../README.md
pandoc ../README.md -o Takamaka.tex --include-in-header mystylefile.tex --toc --highlight-style=kate -V geometry:a4paper -V documentclass:book -V pagestyle:headings -V papersize:a4 -V colorlinks:true
sed -i 's/\\begin{verbatim}/\\begin{myverbatim}\n\\begin{verbatim}/g' Takamaka.tex
sed -i 's/\\end{verbatim}/\\end{verbatim}\n\\end{myverbatim}/g' Takamaka.tex
sed -i 's/103 \& alicudi/$10^3$ \& alicudi/g' Takamaka.tex
sed -i 's/106 \& filicudi/$10^6$ \& filicudi/g' Takamaka.tex
sed -i 's/109 \& stromboli/$10^9$ \& stromboli/g' Takamaka.tex
sed -i 's/1012 \& vulcano/$10^{12}$ \& vulcano/g' Takamaka.tex
sed -i 's/1015 \& salina/$10^{15}$ \& salina/g' Takamaka.tex
sed -i 's/1018 \& lipari/$10^{18}$ \& lipari/g' Takamaka.tex
sed -i 's/1021 \& takamaka/$10^{21}$ \& takamaka/g' Takamaka.tex
sed -i 's/\\chapterfont{\\clearpage}//g' Takamaka.tex
sed -i 's/\\usepackage{sectsty}//g' Takamaka.tex
sed -i 's/\\chapter{Table of Contents}/\\begin{comment}\\chapter{Table of Contents}/g' Takamaka.tex
sed -i 's/\\hypertarget{introduction}/\\end{comment}\n\n\\hypertarget{introduction}/g' Takamaka.tex
pdflatex Takamaka.tex

