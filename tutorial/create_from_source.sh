#!/bin/bash

# This script transforms the ProgrammingHotmoka.source documentation into the following files:
# - ../README.md: the README file of the repository, in Markdown
# - ProgrammingHotmoka.pdf: a PDF version of the documentation
# - ProgrammingHotmoka.epub: an epub version of the documentation

# generate the Markdown version
cp ProgrammingHotmoka.source ProgrammingHotmoka.md
cp pics/state1.fig state1_copy.fig
cp pics/state2.fig state2_copy.fig
cp pics/state3.fig state3_copy.fig

# place figure references. I miss Latex...
sed -i 's/@fig:receiver_payer/1/g' ProgrammingHotmoka.md
sed -i 's/@fig:mokito_start/2/g' ProgrammingHotmoka.md
sed -i 's/@fig:hotwallet_start/3/g' ProgrammingHotmoka.md
sed -i 's/@fig:state1/4/g' ProgrammingHotmoka.md
sed -i 's/@fig:mokito_menu/5/g' ProgrammingHotmoka.md
sed -i 's/@fig:mokito_manifest/6/g' ProgrammingHotmoka.md
sed -i 's/@fig:state2/7/g' ProgrammingHotmoka.md
sed -i 's/@fig:mokito_new_account/8/g' ProgrammingHotmoka.md
sed -i 's/@fig:mokito_elvis_new_account/9/g' ProgrammingHotmoka.md
sed -i 's/@fig:mokito_show_elvis/10/g' ProgrammingHotmoka.md
sed -i 's/@fig:mokito_added_elvis/11/g' ProgrammingHotmoka.md
sed -i 's/@fig:mokito_accounts_menu/12/g' ProgrammingHotmoka.md
sed -i 's/@fig:mokito_insert_passphrase/13/g' ProgrammingHotmoka.md
sed -i 's/@fig:mokito_added_the_boss/14/g' ProgrammingHotmoka.md
sed -i 's/@fig:projects/15/g' ProgrammingHotmoka.md
sed -i 's/@fig:family_jar/17/g' ProgrammingHotmoka.md
sed -i 's/@fig:family/16/g' ProgrammingHotmoka.md
sed -i 's/@fig:state3/18/g' ProgrammingHotmoka.md
sed -i 's/@fig:runs/19/g' ProgrammingHotmoka.md
sed -i 's/@fig:blockchain1/20/g' ProgrammingHotmoka.md
sed -i 's/@fig:blockchain2/21/g' ProgrammingHotmoka.md
sed -i 's/@fig:blockchain3/22/g' ProgrammingHotmoka.md
sed -i 's/@fig:contract_hierarchy/23/g' ProgrammingHotmoka.md
sed -i 's/@fig:lists_hierarchy/24/g' ProgrammingHotmoka.md
sed -i 's/@fig:arrays_hierarchy/25/g' ProgrammingHotmoka.md
sed -i 's/@fig:cross_wins/26/g' ProgrammingHotmoka.md
sed -i 's/@fig:tictactoe_draw/27/g' ProgrammingHotmoka.md
sed -i 's/@fig:tictactoe_grid/28/g' ProgrammingHotmoka.md
sed -i 's/@fig:tictactoe_linear/29/g' ProgrammingHotmoka.md
sed -i 's/@fig:byte_array_hierarchy/30/g' ProgrammingHotmoka.md
sed -i 's/@fig:map_hierarchy/31/g' ProgrammingHotmoka.md
sed -i 's/@fig:erc20_hierarchy/32/g' ProgrammingHotmoka.md
sed -i 's/@fig:erc721_hierarchy/33/g' ProgrammingHotmoka.md
sed -i 's/@fig:node_hierarchy/34/g' ProgrammingHotmoka.md
sed -i 's/@fig:hotmoka_tendermint/35/g' ProgrammingHotmoka.md
sed -i 's/@fig:inbound_rules/36/g' ProgrammingHotmoka.md

# These must be edited by hand since, for instance, they depend on accounts created in Mokito
sed -i 's/@account_mokito/701e20be588db820744df467826d67b9fe451406d7f75da6ef8aeb6805a7365f#0/g' ProgrammingHotmoka.md
sed -i 's/@36words_of_account_mokito/word #1: rail\nword #2: double\nword #3: bag\nword #4: dove\nword #5: fluid\n...\nword #34: bounce\nword #35: deposit\nword #36: hotel/g' ProgrammingHotmoka.md
sed -i 's/@tendermint_version/0.34.15/g' ProgrammingHotmoka.md

# These get automatically recomputed with the update script: do not edit!
sed -i 's/@hotmoka_version/1.0.7/g' ProgrammingHotmoka.md
sed -i 's/@takamakaCode/5878d6d66699ffe19f95482c3080356008f917263c68a8a872be3c437020c9eb/g' ProgrammingHotmoka.md
sed -i 's/@manifest/4cb4ebfcff972f60c22f1bf16950ca11fca32a2d1622b67d2b7f3e63166f37c3#0/g' ProgrammingHotmoka.md
sed -i 's/@gamete//g' ProgrammingHotmoka.md
sed -i 's/@gasStation/4cb4ebfcff972f60c22f1bf16950ca11fca32a2d1622b67d2b7f3e63166f37c3#11/g' ProgrammingHotmoka.md
sed -i 's/@validators/4cb4ebfcff972f60c22f1bf16950ca11fca32a2d1622b67d2b7f3e63166f37c3#2/g' ProgrammingHotmoka.md
sed -i 's/@maxFaucet/10000000000000/g' ProgrammingHotmoka.md
sed -i 's/@chainid/marabunta/g' ProgrammingHotmoka.md
sed -i 's/@chainid/marabunta/g' state1_copy.fig
sed -i 's/@chainid/marabunta/g' state2_copy.fig
sed -i 's/@chainid/marabunta/g' state3_copy.fig
sed -i 's/@account1/0ad8f34419eafa6d095c0a20f95cfc42b6d08cbc69cff0789ade38c5772e2a4b#0/g' ProgrammingHotmoka.md
sed -i 's/@short_account1/0ad8f34419e...#0/g' state2_copy.fig
sed -i 's/@short_account1/0ad8f34419e...#0/g' state3_copy.fig
sed -i 's/@36words_of_account1/ 1: soda\n 2: way\n 3: prevent\n 4: unknown\n 5: girl\n 6: staff\n 7: fox\n 8: figure\n 9: today\n10: wash\n11: garlic\n12: key\n13: pulse\n14: keen\n15: away\n16: diary\n17: whip\n18: hair\n19: finish\n20: begin\n21: auto\n22: purpose\n23: valve\n24: fine\n25: spatial\n26: grass\n27: bounce\n28: paper\n29: always\n30: estate\n31: jump\n32: board\n33: jaguar\n34: image\n35: name\n36: rally/g' ProgrammingHotmoka.md
sed -i "s/@publickeyaccount1/iBXNHGS5zLIYrnOec+HH9FQEejKcHqXGedPto7Jxf0A=/g" ProgrammingHotmoka.md
sed -i "s/@short_publickeyaccount1/iBXNHGS5zL.../g" state2_copy.fig
sed -i "s/@short_publickeyaccount1/iBXNHGS5zL.../g" state3_copy.fig
sed -i 's/@account_anonymous/0ad8f34419eafa6d095c0a20f95cfc42b6d08cbc69cff0789ade38c5772e2a4b#0/g' ProgrammingHotmoka.md
sed -i 's/@new_key/3qtV2z2u7DfsgyXpJgxvqPsnAB959LjMEgkk1V2jiVTE/g' ProgrammingHotmoka.md
sed -i 's/@family_address/8f9a9d74e6d3acabaeb296def3a11868538df8a0298732fad2e5fc749f51763b/g' ProgrammingHotmoka.md
sed -i 's/@short_family_address/8f9a9d74e6.../g' state3_copy.fig
sed -i 's/@code_family_address/bef038c492244fb244ecd5e3c177b1317a32c5551da311f0baf2828d3d469f45/g' ProgrammingHotmoka.md
sed -i 's/@family2_address/1bc976ea627d751344b39702611f6a64787d9c5ae7c4f948fad61ebae30f1a3e/g' ProgrammingHotmoka.md
sed -i 's/@family_exported_address/89319f9714765c89c1eeccdc34806ee5490089dfb91bfcac9e389023e297a69e/g' ProgrammingHotmoka.md
sed -i 's/@family3_address/b327e54893ca9b6eb43838aa731677927340135fe6635e65ca944b56080d244c/g' ProgrammingHotmoka.md
sed -i 's/@person_object/7cb6c4c82459ef6088e924633921928da5b680eb9534879d8a53db943a191eea#0/g' ProgrammingHotmoka.md
sed -i 's/@person2_object/d683a1876a0919631ee1d098ecdbaeca6dcf3f86fd76e914cf83baa6080da7cb#0/g' ProgrammingHotmoka.md
sed -i 's/@person3_object/d1d77c34c7a781d022598445594abf83cfc113f7c091fb99d6a39ecadff2e6f0#0/g' ProgrammingHotmoka.md
sed -i 's/@gradual_ponzi_address/c1ee2e9a3cf9777f457805f5be2dacd8e21070ee9244ef4c934ba0b08037ca23/g' ProgrammingHotmoka.md
sed -i 's/@gradual_ponzi_object/e42550ba0977e8cdc4ef4605472630094fc9a2deea344a5accb624e0fb516a54#0/g' ProgrammingHotmoka.md
sed -i 's/@gradual_ponzi_list/e42550ba0977e8cdc4ef4605472630094fc9a2deea344a5accb624e0fb516a54#1/g' ProgrammingHotmoka.md
sed -i 's/@gradual_ponzi_first/e42550ba0977e8cdc4ef4605472630094fc9a2deea344a5accb624e0fb516a54#2/g' ProgrammingHotmoka.md
sed -i 's/@gradual_ponzi_last/6961debbb96369c5f6fc6c4151118462a76861e445ee61562252a22784eb48de#0/g' ProgrammingHotmoka.md
sed -i 's/@account2/cb1368fcb822b1720a997c9c24aac2f901e455fa38438ce41879de9164237cd5#0/g' ProgrammingHotmoka.md
sed -i 's/@account3/27249ea17c724f2c4703f954feaf5ebb018fbe06b355a7022bb794c754f29a8e#0/g' ProgrammingHotmoka.md
sed -i 's/@account4/08284520f79e6995c2f7c7a8418040800a00a4d006a8ed213391555411e0f6fd#0/g' ProgrammingHotmoka.md
sed -i 's/@account5/77e1bfd05ba4b62d41e1113dd14f55c407968337151485d416d98d26efc6fffc#0/g' ProgrammingHotmoka.md
sed -i 's/@account6/1011411e48ef002ceeafee0455c7114afd22e8f8c7300b7b3a9e766d43a737e4#0/g' ProgrammingHotmoka.md
sed -i 's/@account7/372a53e7e93eb6a4630c5a4816cf19bce17e2508325d22134d5b39f0f195569a#0/g' ProgrammingHotmoka.md
sed -i 's/@publickeyaccount4/YdbZ58JDpuzlxIB4oHXiYmu7lcB0q5dUwFkuP3Hqoik=/g' ProgrammingHotmoka.md
sed -i 's/@short_publickeyaccount5/MIIDQjCCAjUGByqGSM44BAEwggIoAo.../g' ProgrammingHotmoka.md
sed -i 's/@tictactoe_address/c753ad535b0a5179640c202761f0a7bddffa362010c03a88eb177b2443567702/g' ProgrammingHotmoka.md
sed -i 's/@tictactoe_object/c13cfe01f846cd1417d96014ce8085d21acb2b80b9ad1dd349525a8537d530d0#0/g' ProgrammingHotmoka.md
sed -i 's/@erc20_address/9d9b8f1a593fda7b6616220b4b74d2ac8abb5e988c1813a3a0a1d9e2ffef8546/g' ProgrammingHotmoka.md
sed -i 's/@erc20_object/ebe75c48121f18d8bbd7ad8fc35c336e5f81fab918a651e72b51a5aec677745e#0/g' ProgrammingHotmoka.md
sed -i 's/@server/panarea.hotmoka.io/g' ProgrammingHotmoka.md
sed -i 's/@new_docker_key/D344Rdcptc6DeUDz94fBst8iCLbXQnRU8MoxFH3ctN8K/g' ProgrammingHotmoka.md
sed -i 's/@container_id1/8b495e14c5b338c7b38134d762b392dfd84256ea374c82a1b0b2a97b1ecc8385/g' ProgrammingHotmoka.md
sed -i 's/@docker_takamaka_code/1afad391033c80ceeb71aa4d6fdc629f6fe19209cdbc57cb1f7a34a118759d2d/g' ProgrammingHotmoka.md
sed -i 's/@docker_manifest/df02c536585de2f9bf521ed9e46fc402682d159f74c91187af604c7ac57da26e#0/g' ProgrammingHotmoka.md
sed -i 's/@docker_gamete/3fe876e7b634979931e67f5bd70f26e158486e192c06f2c505250215257a36f2#0/g' ProgrammingHotmoka.md
sed -i 's/@docker_validators/df02c536585de2f9bf521ed9e46fc402682d159f74c91187af604c7ac57da26e#1/g' ProgrammingHotmoka.md
sed -i 's/@docker_validator0/aa44c2326e0a4886887b8b79c6cdd4297c9bba60db980536124b4f562d3754ef#0/g' ProgrammingHotmoka.md
sed -i 's/@docker_id_validator0/377BB831A95281854071C3603BAF68951DC02B66/g' ProgrammingHotmoka.md
sed -i 's/@container_id2/36d3a3e2f17946b540d1c747dd26d01e81df107f1450d2a580b381216f9de6a7/g' ProgrammingHotmoka.md
sed -i 's/@container_id3/261425a7638c904e9bd8eb87a71366d6ae22e0d203a791566d29b1c393f158e8/g' ProgrammingHotmoka.md
sed -i 's/@docker_new_account/4e5c6e0e56142fc9fca8d2d41d080aaa41ccdd66835fa5a3e9d7a7478c72b0f3#0/g' ProgrammingHotmoka.md
sed -i 's/@docker_total_gas_new_account/44596/g' ProgrammingHotmoka.md
sed -i 's/@docker_reduced_balance/999999998720837/g' ProgrammingHotmoka.md
sed -i 's/@docker_balance_validator0/11160/g' ProgrammingHotmoka.md
sed -i 's/@docker_staked_validator0/33480/g' ProgrammingHotmoka.md
sed -i 's/@docker_diff1/1279163/g' ProgrammingHotmoka.md
sed -i 's/@docker_diff2/44596/g' ProgrammingHotmoka.md
sed -i 's/@docker_diff3/44/g' ProgrammingHotmoka.md
sed -i 's/@docker_sum1/44640/g' ProgrammingHotmoka.md

cp ProgrammingHotmoka.md temp.md
sed -i "/^\[PDFonly]:/d" ProgrammingHotmoka.md
sed -i "s/\[Markdownonly]://g" ProgrammingHotmoka.md

# we regenerate the png figures, since they might contain some string changed
# by previous sed commands
fig2dev -L png -m 4 state1_copy.fig pics/state1.png
fig2dev -L png -m 4 state2_copy.fig pics/state2.png
fig2dev -L png -m 4 state3_copy.fig pics/state3.png
rm *_copy.fig

# generate the PDF version now
sed -i "/^\[Markdownonly]:/d" temp.md
sed -i "s/\[PDFonly]://g" temp.md
pandoc temp.md -o ProgrammingHotmoka.tex --include-in-header mystylefile.tex --include-after-body backcover.tex --toc --highlight-style=kate -V geometry:a4paper -V documentclass:book -V pagestyle:headings -V papersize:a4 -V colorlinks:true
rm temp.md
sed -i 's/\\begin{verbatim}/\\begin{myverbatim}\n\\begin{verbatim}/g' ProgrammingHotmoka.tex
sed -i 's/\\end{verbatim}/\\end{verbatim}\n\\end{myverbatim}/g' ProgrammingHotmoka.tex
sed -i 's/103 \& alicudi/$10^3$ \& alicudi/g' ProgrammingHotmoka.tex
sed -i 's/106 \& filicudi/$10^6$ \& filicudi/g' ProgrammingHotmoka.tex
sed -i 's/109 \& stromboli/$10^9$ \& stromboli/g' ProgrammingHotmoka.tex
sed -i 's/1012 \& vulcano/$10^{12}$ \& vulcano/g' ProgrammingHotmoka.tex
sed -i 's/1015 \& salina/$10^{15}$ \& salina/g' ProgrammingHotmoka.tex
sed -i 's/1018 \& lipari/$10^{18}$ \& lipari/g' ProgrammingHotmoka.tex
sed -i 's/1021 \& moka/$10^{21}$ \& moka/g' ProgrammingHotmoka.tex
sed -i 's/\\chapterfont{\\clearpage}//g' ProgrammingHotmoka.tex
sed -i 's/\\chapter{Table of Contents}/\\begin{comment}\\chapter{Table of Contents}/g' ProgrammingHotmoka.tex
sed -i 's/\\hypertarget{introduction}/\\end{comment}\n\n\\hypertarget{introduction}/g' ProgrammingHotmoka.tex

# delete the \begin{document}
sed -i 's/\\begin{document}//g' ProgrammingHotmoka.tex
# place \begin{document} before \BgThispage
sed -i 's/\\BgThispage/\\begin{document}\n\\BgThispage/g' ProgrammingHotmoka.tex

pdflatex ProgrammingHotmoka.tex

mv ProgrammingHotmoka.md ../README.md

# generate the epub version of the document
# we remove the first lines of the Markdown, that contain Java build information
tail -n +6 ../README.md > ProgrammingHotmoka.md
pandoc -o ProgrammingHotmoka.epub metadata.yaml ProgrammingHotmoka.md
rm ProgrammingHotmoka.md

# generate the mobi version of the document
ebook-convert ProgrammingHotmoka.epub ProgrammingHotmoka.mobi
