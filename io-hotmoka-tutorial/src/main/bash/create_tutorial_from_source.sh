#!/bin/bash

# This script transforms the Tutorial.md documentation into the following files:
# - target/ProgrammingHotmoka.md: the Markdown version of the tutorial
# - target/ProgrammingHotmoka.pdf: a PDF version of the tutorial
# - target/ProgrammingHotmoka.epub: an epub version of the tutorial
# - target/ProgrammingHotmoka.mobi: a mobi version of the tutorial
#
# A copy of ProgrammingHotmoka.md and of its pictures is also copied into tutorial-md

cp src/main/md/Tutorial.md target/Tutorial.md
cp -r src/main/resources/pics target/

# generate the Markdown version

# place figure references. I miss Latex...
sed -i 's/@fig:receiver_payer/1/g' target/Tutorial.md
sed -i 's/@fig:mokito_start/2/g' target/Tutorial.md
sed -i 's/@fig:hotwallet_start/3/g' target/Tutorial.md
sed -i 's/@fig:state1/4/g' target/Tutorial.md
sed -i 's/@fig:mokito_menu/5/g' target/Tutorial.md
sed -i 's/@fig:mokito_manifest/6/g' target/Tutorial.md
sed -i 's/@fig:state2/7/g' target/Tutorial.md
sed -i 's/@fig:mokito_new_account/8/g' target/Tutorial.md
sed -i 's/@fig:mokito_elvis_new_account/9/g' target/Tutorial.md
sed -i 's/@fig:mokito_show_elvis/10/g' target/Tutorial.md
sed -i 's/@fig:mokito_added_elvis/11/g' target/Tutorial.md
sed -i 's/@fig:mokito_accounts_menu/12/g' target/Tutorial.md
sed -i 's/@fig:mokito_insert_passphrase/13/g' target/Tutorial.md
sed -i 's/@fig:mokito_added_the_boss/14/g' target/Tutorial.md
sed -i 's/@fig:projects/15/g' target/Tutorial.md
sed -i 's/@fig:family_jar/17/g' target/Tutorial.md
sed -i 's/@fig:family/16/g' target/Tutorial.md
sed -i 's/@fig:state3/18/g' target/Tutorial.md
sed -i 's/@fig:runs/19/g' target/Tutorial.md
sed -i 's/@fig:blockchain1/20/g' target/Tutorial.md
sed -i 's/@fig:blockchain2/21/g' target/Tutorial.md
sed -i 's/@fig:blockchain3/22/g' target/Tutorial.md
sed -i 's/@fig:contract_hierarchy/23/g' target/Tutorial.md
sed -i 's/@fig:lists_hierarchy/24/g' target/Tutorial.md
sed -i 's/@fig:arrays_hierarchy/25/g' target/Tutorial.md
sed -i 's/@fig:cross_wins/26/g' target/Tutorial.md
sed -i 's/@fig:tictactoe_draw/27/g' target/Tutorial.md
sed -i 's/@fig:tictactoe_grid/28/g' target/Tutorial.md
sed -i 's/@fig:tictactoe_linear/29/g' target/Tutorial.md
sed -i 's/@fig:byte_array_hierarchy/30/g' target/Tutorial.md
sed -i 's/@fig:map_hierarchy/31/g' target/Tutorial.md
sed -i 's/@fig:erc20_hierarchy/32/g' target/Tutorial.md
sed -i 's/@fig:erc721_hierarchy/33/g' target/Tutorial.md
sed -i 's/@fig:node_hierarchy/34/g' target/Tutorial.md
sed -i 's/@fig:hotmoka_tendermint/35/g' target/Tutorial.md
sed -i 's/@fig:inbound_rules/36/g' target/Tutorial.md
sed -i 's/@fig:entities_hierarchy/37/g' target/Tutorial.md

source src/main/bash/replacements.sh

# we regenerate the png figures, since they might contain some string changed by the previous sed commands
fig2dev -L png -m 4 target/pics/state1.fig target/pics/state1.png
fig2dev -L png -m 4 target/pics/state2.fig target/pics/state2.png
fig2dev -L png -m 4 target/pics/state3.fig target/pics/state3.png

# These must be edited by hand since, for instance, they depend on accounts created in Mokito
sed -i 's/@tendermint_version/0.34.15/g' target/Tutorial.md
sed -i 's/@hotmoka_repo/https:\/\/github.com\/Hotmoka\/hotmoka/g' target/Tutorial.md
sed -i 's/@takamaka_repo/https:\/\/github.com\/Hotmoka\/io-takamaka-code/g' target/Tutorial.md
sed -i 's/@fausto_email/fausto.spoto@hotmoka.io/g' target/Tutorial.md
sed -i 's/@tutorial_name/hotmoka_tutorial/g' target/Tutorial.md
sed -i 's/@account_mokito/701e20be588db820744df467826d67b9fe451406d7f75da6ef8aeb6805a7365f#0/g' target/Tutorial.md
sed -i 's/@36words_of_account_mokito/word #1: rail\nword #2: double\nword #3: bag\nword #4: dove\nword #5: fluid\n...\nword #34: bounce\nword #35: deposit\nword #36: hotel/g' target/Tutorial.md

cp target/Tutorial.md target/ProgrammingHotmoka.md

sed -i "/^\[PDFonly]:/d" target/ProgrammingHotmoka.md
sed -i "s/\[Markdownonly]://g" target/ProgrammingHotmoka.md

# generate the PDF version now
sed -i "/^\[Markdownonly]:/d" target/Tutorial.md
sed -i "s/\[PDFonly]://g" target/Tutorial.md
pandoc target/Tutorial.md -o target/ProgrammingHotmoka.tex --include-in-header src/main/latex/mystylefile_hotmoka.tex --include-after-body src/main/latex/backcover.tex --toc --highlight-style=kate -V geometry:a4paper -V documentclass:book -V pagestyle:headings -V papersize:a4 -V colorlinks:true
rm target/Tutorial.md
sed -i 's/\\begin{verbatim}/\\begin{myverbatim}\n\\begin{verbatim}/g' target/ProgrammingHotmoka.tex
sed -i 's/\\end{verbatim}/\\end{verbatim}\n\\end{myverbatim}/g' target/ProgrammingHotmoka.tex
sed -i 's/103 \& alicudi/$10^3$ \& alicudi/g' target/ProgrammingHotmoka.tex
sed -i 's/106 \& filicudi/$10^6$ \& filicudi/g' target/ProgrammingHotmoka.tex
sed -i 's/109 \& stromboli/$10^9$ \& stromboli/g' target/ProgrammingHotmoka.tex
sed -i 's/1012 \& vulcano/$10^{12}$ \& vulcano/g' target/ProgrammingHotmoka.tex
sed -i 's/1015 \& salina/$10^{15}$ \& salina/g' target/ProgrammingHotmoka.tex
sed -i 's/1018 \& lipari/$10^{18}$ \& lipari/g' target/ProgrammingHotmoka.tex
sed -i 's/1021 \& moka/$10^{21}$ \& moka/g' target/ProgrammingHotmoka.tex
sed -i 's/\\chapterfont{\\clearpage}//g' target/ProgrammingHotmoka.tex
sed -i 's/\\chapter{Table of Contents}/\\begin{comment}\\chapter{Table of Contents}/g' target/ProgrammingHotmoka.tex
sed -i 's/\\hypertarget{introduction}/\\end{comment}\n\n\\hypertarget{introduction}/g' target/ProgrammingHotmoka.tex

# place \input{cover_page.tex} after \begin{document}
sed -i 's/\\begin{document}/\\begin{document}\\input{..\/src\/main\/latex\/cover_page.tex}/g' target/ProgrammingHotmoka.tex

cd target
pdflatex ProgrammingHotmoka.tex
pdflatex ProgrammingHotmoka.tex
rm ProgrammingHotmoka.aux
rm ProgrammingHotmoka.log
rm ProgrammingHotmoka.toc
rm ProgrammingHotmoka.tex
cd ..

# generate the epub version of the document:
# we remove the first lines of the Markdown, that contain Java build information
tail -n +6 target/ProgrammingHotmoka.md > target/temp.md
cd target
pandoc -o ProgrammingHotmoka.epub ../src/main/resources/metadata-hotmoka.yaml temp.md
rm temp.md
cd ..

# generate the mobi version of the document
ebook-convert target/ProgrammingHotmoka.epub target/ProgrammingHotmoka.mobi

# copy the md version into the tutorial-md folder, where it will be available for the README.md file
rm -rf tutorial-md
mkdir tutorial-md
mkdir tutorial-md/pics
cp -r target/pics/*.png tutorial-md/pics/
cp target/ProgrammingHotmoka.md tutorial-md
