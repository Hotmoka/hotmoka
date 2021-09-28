#!/bin/bash

# generate the Markdown version
cp Hotmoka.source Hotmoka.md
cp pics/state1.fig pics/state1_copy.fig
cp pics/state2.fig pics/state2_copy.fig
cp pics/state3.fig pics/state3_copy.fig

# place figure references. I miss Latex...
sed -i 's/@fig:mokito_start/1/g' Hotmoka.md
sed -i 's/@fig:state1/2/g' Hotmoka.md
sed -i 's/@fig:mokito_menu/3/g' Hotmoka.md
sed -i 's/@fig:mokito_manifest/4/g' Hotmoka.md
sed -i 's/@fig:state2/5/g' Hotmoka.md
sed -i 's/@fig:mokito_new_account/6/g' Hotmoka.md
sed -i 's/@fig:mokito_elvis_new_account/7/g' Hotmoka.md
sed -i 's/@fig:mokito_show_elvis/8/g' Hotmoka.md
sed -i 's/@fig:mokito_added_elvis/9/g' Hotmoka.md
sed -i 's/@fig:mokito_accounts_menu/10/g' Hotmoka.md
sed -i 's/@fig:mokito_insert_passphrase/11/g' Hotmoka.md
sed -i 's/@fig:mokito_added_the_boss/12/g' Hotmoka.md
sed -i 's/@fig:projects/13/g' Hotmoka.md
sed -i 's/@fig:family_jar/15/g' Hotmoka.md
sed -i 's/@fig:family/14/g' Hotmoka.md
sed -i 's/@fig:state3/16/g' Hotmoka.md
sed -i 's/@fig:runs/17/g' Hotmoka.md
sed -i 's/@fig:blockchain1/18/g' Hotmoka.md
sed -i 's/@fig:blockchain2/19/g' Hotmoka.md
sed -i 's/@fig:blockchain3/20/g' Hotmoka.md
sed -i 's/@fig:contract_hierarchy/21/g' Hotmoka.md
sed -i 's/@fig:lists_hierarchy/22/g' Hotmoka.md
sed -i 's/@fig:arrays_hierarchy/23/g' Hotmoka.md
sed -i 's/@fig:cross_wins/24/g' Hotmoka.md
sed -i 's/@fig:tictactoe_draw/25/g' Hotmoka.md
sed -i 's/@fig:tictactoe_grid/26/g' Hotmoka.md
sed -i 's/@fig:tictactoe_linear/27/g' Hotmoka.md
sed -i 's/@fig:byte_array_hierarchy/28/g' Hotmoka.md
sed -i 's/@fig:map_hierarchy/29/g' Hotmoka.md
sed -i 's/@fig:node_hierarchy/30/g' Hotmoka.md
sed -i 's/@hotmoka_version/1.0.4/g' Hotmoka.md
sed -i 's/@takamakaCode/56e46353158a66f893460554be026e3fc15d1a215bc59606ea5fac585527ff1a/g' Hotmoka.md
sed -i 's/@chainid/chain-btmZzq/g' Hotmoka.md
sed -i 's/@chainid/chain-btmZzq/g' pics/state1_copy.fig
sed -i 's/@chainid/chain-btmZzq/g' pics/state2_copy.fig
sed -i 's/@chainid/chain-btmZzq/g' pics/state3_copy.fig
sed -i 's/@account1/8a21b72f3f499a128acf99463d7b25450d34e8f9b4a81ee0af5c9ff2dd10a23f#0/g' Hotmoka.md
sed -i 's/@account1/8a21b72f3f4...#0/g' pics/state2_copy.fig
sed -i 's/@account1/8a21b72f3f4...#0/g' pics/state3_copy.fig
sed -i 's/@publickeyaccount1/lR0zMaddnucx+Xyoj26mzfPg+1g1yzWghJ5MQv5dOWw=/g' Hotmoka.md
sed -i 's/@publickeyaccount1/lR0zMaddnucx.../g' pics/state2_copy.fig
sed -i 's/@publickeyaccount1/lR0zMaddnucx.../g' pics/state3_copy.fig
sed -i 's/@family_address/d6441356d8038851ec8c4b615dc46b1c55c72d4fd8e4906f63e345c9f4dfe64f/g' Hotmoka.md
sed -i 's/@family_address/d6441356d803.../g' pics/state3_copy.fig
sed -i 's/@code_family_address/830ff7f3c268420ee9ab5e5225012e5c27d11d069d9df4d255702174d2b49f28/g' Hotmoka.md
sed -i 's/@family2_address/926bd122361285351f0f2bc80dadc44a334e6791779c6269f807712610ac44b6/g' Hotmoka.md
sed -i 's/@family_exported_address/954c4220fbbddc31fc79f29959f3ecd6c27c2a0e3121a2d0c301ff88a5aef099/g' Hotmoka.md
sed -i 's/@person_object/5ea47fbefbae0df8cc1984fff0aaa159eb075f64a7fc27323f5f3e8fd0adc998#0/g' Hotmoka.md
sed -i 's/@person2_object/563173b4cf375d7d9724d1a0d532749c58ecb7a47bd5e1cfb9c32f4869f522a8#0/g' Hotmoka.md
sed -i 's/@person3_object/8e8306084c6093bf76ecd1073fbd41e182d337b20fc099af92760d0b8ca4659d#0/g' Hotmoka.md
sed -i 's/@account2/167fa9c769b99cfcc43dd85f9cc2d06265e2a9bfb6fadc730fbd3dce477b7412#0/g' Hotmoka.md
sed -i 's/@account3/f58a6a89872d5af53a29e5e981e1374817c5f5e3d9900de17bb13369a86d0c43#0/g' Hotmoka.md
sed -i 's/@account_mokito/067cea2b29d1a3bd0f7c82fb3b6a767e04a8dde8c70c4b9656c1f4f0c5e34cec#0/g' Hotmoka.md
sed -i 's/@server/panarea.hotmoka.io/g' Hotmoka.md

cp Hotmoka.md temp.md
sed -i "/^\[PDFonly]:/d" Hotmoka.md
sed -i "s/\[Markdownonly]://g" Hotmoka.md

# we regenerate the png figures, since they might contain some string changed
# by previous sed commands
fig2dev -L png -m 4 pics/state1_copy.fig pics/state1.png
fig2dev -L png -m 4 pics/state2_copy.fig pics/state2.png
fig2dev -L png -m 4 pics/state3_copy.fig pics/state3.png
rm pics/*_copy.fig

# generate the PDF version now
sed -i "/^\[Markdownonly]:/d" temp.md
sed -i "s/\[PDFonly]://g" temp.md
pandoc temp.md -o Hotmoka.tex --include-in-header mystylefile.tex --include-after-body backcover.tex --toc --highlight-style=kate -V geometry:a4paper -V documentclass:book -V pagestyle:headings -V papersize:a4 -V colorlinks:true
rm temp.md
sed -i 's/\\begin{verbatim}/\\begin{myverbatim}\n\\begin{verbatim}/g' Hotmoka.tex
sed -i 's/\\end{verbatim}/\\end{verbatim}\n\\end{myverbatim}/g' Hotmoka.tex
sed -i 's/103 \& alicudi/$10^3$ \& alicudi/g' Hotmoka.tex
sed -i 's/106 \& filicudi/$10^6$ \& filicudi/g' Hotmoka.tex
sed -i 's/109 \& stromboli/$10^9$ \& stromboli/g' Hotmoka.tex
sed -i 's/1012 \& vulcano/$10^{12}$ \& vulcano/g' Hotmoka.tex
sed -i 's/1015 \& salina/$10^{15}$ \& salina/g' Hotmoka.tex
sed -i 's/1018 \& lipari/$10^{18}$ \& lipari/g' Hotmoka.tex
sed -i 's/1021 \& moka/$10^{21}$ \& moka/g' Hotmoka.tex
sed -i 's/\\chapterfont{\\clearpage}//g' Hotmoka.tex
sed -i 's/\\usepackage{sectsty}//g' Hotmoka.tex
sed -i 's/\\chapter{Table of Contents}/\\begin{comment}\\chapter{Table of Contents}/g' Hotmoka.tex
sed -i 's/\\hypertarget{introduction}/\\end{comment}\n\n\\hypertarget{introduction}/g' Hotmoka.tex

# delete the \begin{document}
sed -i 's/\\begin{document}//g' Hotmoka.tex
# plave \begin{document} before \BgThispage
sed -i 's/\\BgThispage/\\begin{document}\n\\BgThispage/g' Hotmoka.tex

pdflatex Hotmoka.tex

mv Hotmoka.md ../README.md

