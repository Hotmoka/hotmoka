#!/bin/bash

# generate the Markdown version
cp Hotmoka.source Hotmoka.md
cp pics/state1.fig state1_copy.fig
cp pics/state2.fig state2_copy.fig
cp pics/state3.fig state3_copy.fig

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
sed -i 's/@fig:erc20_hierarchy/31/g' Hotmoka.md
sed -i 's/@fig:erc721_hierarchy/32/g' Hotmoka.md

# These must be edited by hand since, for instance, they depend on accounts created in Mokito
sed -i 's/@account_mokito/701e20be588db820744df467826d67b9fe451406d7f75da6ef8aeb6805a7365f#0/g' Hotmoka.md
sed -i 's/@36words_of_account_mokito/word #1: rail\nword #2: double\nword #3: bag\nword #4: dove\nword #5: fluid\n...\nword #34: bounce\nword #35: deposit\nword #36: hotel/g' Hotmoka.md
sed -i 's/@tendermint_version/0.34.15/g' Hotmoka.md

# These can be automatically recomputed with the update script
sed -i 's/@hotmoka_version/1.0.7/g' Hotmoka.md
sed -i 's/@takamakaCode/6a10bcbf10e0fb2d37c9de0b3f20f672cd464b8868219679e837481672665621/g' Hotmoka.md
sed -i 's/@manifest/e118f3d3a66a423c058f0f639e68f052d9a4cbe35f6ca32fdd3e7db65bf62f29#0/g' Hotmoka.md
sed -i 's/@gamete/97256737b825e9d35661cb0e85cd546dfff8b75d52c97dfc6a984dbf0a4423b9#0/g' Hotmoka.md
sed -i 's/@gasStation/e118f3d3a66a423c058f0f639e68f052d9a4cbe35f6ca32fdd3e7db65bf62f29#11/g' Hotmoka.md
sed -i 's/@validators/e118f3d3a66a423c058f0f639e68f052d9a4cbe35f6ca32fdd3e7db65bf62f29#2/g' Hotmoka.md
sed -i 's/@maxFaucet/10000000000000/g' Hotmoka.md
sed -i 's/@chainid/marabunta/g' Hotmoka.md
sed -i 's/@chainid/marabunta/g' state1_copy.fig
sed -i 's/@chainid/marabunta/g' state2_copy.fig
sed -i 's/@chainid/marabunta/g' state3_copy.fig
sed -i 's/@account1/0cfa98d2057f38b2a6ebe7bb60cc61a627a772649a0f2477141588f84d160104#0/g' Hotmoka.md
sed -i 's/@short_account1/0cfa98d2057...#0/g' state2_copy.fig
sed -i 's/@short_account1/0cfa98d2057...#0/g' state3_copy.fig
sed -i 's/@36words_of_account1/ 1: pact\n 2: perfect\n 3: angle\n 4: rent\n 5: royal\n 6: power\n 7: ready\n 8: wheat\n 9: label\n10: memory\n11: merry\n12: project\n13: soul\n14: erase\n15: elite\n16: quiz\n17: december\n18: news\n19: road\n20: sorry\n21: render\n22: great\n23: make\n24: shaft\n25: polar\n26: nasty\n27: charge\n28: bundle\n29: electric\n30: meat\n31: file\n32: business\n33: escape\n34: quote\n35: donate\n36: twelve/g' Hotmoka.md
sed -i "s/@publickeyaccount1/5VwO8FhETjlpE5SIeaJqhWzoUpfRFc22uz5gMHf6SEY=/g" Hotmoka.md
sed -i "s/@short_publickeyaccount1/5VwO8FhETj.../g" state2_copy.fig
sed -i "s/@short_publickeyaccount1/5VwO8FhETj.../g" state3_copy.fig
sed -i 's/@family_address/28ab4775ee3eaa5db148c0dab2171719d6c8eec4760585278a71c7540a160877/g' Hotmoka.md
sed -i 's/@short_family_address/28ab4775ee.../g' state3_copy.fig
sed -i 's/@code_family_address/e784829a9d95e74e386b1b7b104441a5189c1ef6dbf731b4569e817a606e0a0e/g' Hotmoka.md
sed -i 's/@family2_address/e11d395d0c006fe2a036da731a954a6204bcb291256e62407446055f9d0d0707/g' Hotmoka.md
sed -i 's/@family_exported_address/841d7ea54a893fc38c6b51404d46c12b490b76709d35e5c3f21d126ac9e3b672/g' Hotmoka.md
sed -i 's/@family3_address/838e872ea7ea3ce22e3ef69d816737b5583467d3105541f2bb5584dbab361fc6/g' Hotmoka.md
sed -i 's/@person_object/f8bba465aaa00dbf144921779301e35f04cff4bea00b0064d2b4b7949d90bc26#0/g' Hotmoka.md
sed -i 's/@person2_object/3ef51f53c4946b770db2cf0ef8cb905cfa1916d7b4f390e9cc921aad6aa1bc95#0/g' Hotmoka.md
sed -i 's/@person3_object/5b0f916d9be01f283d3c68dd918d829cd92bac2f70596fa11d4defb14a75ddbe#0/g' Hotmoka.md
sed -i 's/@gradual_ponzi_address/4bcc6423e92fa547a2c723f2da9c7240976fd240f137d5d21ad9f56c73c3621e/g' Hotmoka.md
sed -i 's/@gradual_ponzi_object/8797e304cfe0ac8c6b93cd54d1a7c399c5f9bba7941bfcca1d575a0f7ca4e63e#0/g' Hotmoka.md
sed -i 's/@gradual_ponzi_list/8797e304cfe0ac8c6b93cd54d1a7c399c5f9bba7941bfcca1d575a0f7ca4e63e#1/g' Hotmoka.md
sed -i 's/@gradual_ponzi_first/8797e304cfe0ac8c6b93cd54d1a7c399c5f9bba7941bfcca1d575a0f7ca4e63e#2/g' Hotmoka.md
sed -i 's/@gradual_ponzi_last/edbee9e33869025db9a9d77b5217fb6ac71a5274ef811d142a66680d4f2f6e85#0/g' Hotmoka.md
sed -i 's/@account2/74b04f2349e72f06effc2b6312838174acdcdeb8985015b3e4e8f63a1df1df70#0/g' Hotmoka.md
sed -i 's/@account3/a9643c4586e02ffe752a86d5bd5da6b6c2e05d5721471d817b062095c28349c5#0/g' Hotmoka.md
sed -i 's/@account4/f7e33e12b388d4818b88f1f1cdf32f12b478180ffa7ed715fcb86f9e5cae3d6f#0/g' Hotmoka.md
sed -i 's/@account5/7a9ef47e632771af0177ed4f40044433f575b9293b21a2cba04b00a32a443082#0/g' Hotmoka.md
sed -i 's/@account6/c201a00f5576d8d54094fb2ac8150e5aef9269d8649217e23229f194ef533113#0/g' Hotmoka.md
sed -i 's/@account7/ead226a1d53cdf15e284f0cdc4a96c9b2886886540b5d3715362f3a0af79eef8#0/g' Hotmoka.md
sed -i 's/@publickeyaccount4/w8IDvuUp+jba94uglaPRHjMvhz7hfndj6Z6awbMdi1o=/g' Hotmoka.md
sed -i 's/@short_publickeyaccount5/MIIDQjCCAjUGByqGSM44BAEwggIoAo.../g' Hotmoka.md
sed -i 's/@tictactoe_address/e25f9a68da61df1b0bc114d847d81197e0e2a95ddb50fd897a0452173280b76e/g' Hotmoka.md
sed -i 's/@tictactoe_object/cec3268802129306c8521c87d45f9b0392bfc99aecbdc77a91b248e0bf5646e9#0/g' Hotmoka.md
sed -i 's/@erc20_address/572308e3dcabe91f186fa05c44184cbbf9765cfc9ed632c98bcce2da3f255845/g' Hotmoka.md
sed -i 's/@erc20_object/6a1d2b48fe454ea9db598b01a57e322c2c95236fe9661701d10e539fd470ad6b#0/g' Hotmoka.md
sed -i 's/@server/panarea.hotmoka.io:8080/g' Hotmoka.md

cp Hotmoka.md temp.md
sed -i "/^\[PDFonly]:/d" Hotmoka.md
sed -i "s/\[Markdownonly]://g" Hotmoka.md

# we regenerate the png figures, since they might contain some string changed
# by previous sed commands
fig2dev -L png -m 4 state1_copy.fig pics/state1.png
fig2dev -L png -m 4 state2_copy.fig pics/state2.png
fig2dev -L png -m 4 state3_copy.fig pics/state3.png
rm *_copy.fig

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

