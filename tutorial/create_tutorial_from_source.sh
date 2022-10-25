#!/bin/bash

# This script transforms the Tutorial.source documentation into the following files:
# - ../README.md: the README file of the repository, in Markdown
# - ProgrammingHotmoka.pdf: a PDF version of the documentation for Hotmoka
# - ProgrammingHotmoka.epub: an epub version of the documentation for Hotmoka
# - ProgrammingHotmoka.mobi: a mobi version of the documentation for Hotmoka
# - ProgrammingBlueknot.pdf: a PDF version of the documentation for Blueknot
# - ProgrammingBlueknot.epub: an epub version of the documentation for Blueknot
# - ProgrammingBlueknot.mobi: a mobi version of the documentation for Blueknot

cp Tutorial.source Tutorial.md
cp pics/state1.fig state1_copy.fig
cp pics/state2.fig state2_copy.fig
cp pics/state3.fig state3_copy.fig

# generate the Markdown version

# place figure references. I miss Latex...
sed -i 's/@fig:receiver_payer/1/g' Tutorial.md
sed -i 's/@fig:mokito_start/2/g' Tutorial.md
sed -i 's/@fig:hotwallet_start/3/g' Tutorial.md
sed -i 's/@fig:state1/4/g' Tutorial.md
sed -i 's/@fig:mokito_menu/5/g' Tutorial.md
sed -i 's/@fig:mokito_manifest/6/g' Tutorial.md
sed -i 's/@fig:state2/7/g' Tutorial.md
sed -i 's/@fig:mokito_new_account/8/g' Tutorial.md
sed -i 's/@fig:mokito_elvis_new_account/9/g' Tutorial.md
sed -i 's/@fig:mokito_show_elvis/10/g' Tutorial.md
sed -i 's/@fig:mokito_added_elvis/11/g' Tutorial.md
sed -i 's/@fig:mokito_accounts_menu/12/g' Tutorial.md
sed -i 's/@fig:mokito_insert_passphrase/13/g' Tutorial.md
sed -i 's/@fig:mokito_added_the_boss/14/g' Tutorial.md
sed -i 's/@fig:projects/15/g' Tutorial.md
sed -i 's/@fig:family_jar/17/g' Tutorial.md
sed -i 's/@fig:family/16/g' Tutorial.md
sed -i 's/@fig:state3/18/g' Tutorial.md
sed -i 's/@fig:runs/19/g' Tutorial.md
sed -i 's/@fig:blockchain1/20/g' Tutorial.md
sed -i 's/@fig:blockchain2/21/g' Tutorial.md
sed -i 's/@fig:blockchain3/22/g' Tutorial.md
sed -i 's/@fig:contract_hierarchy/23/g' Tutorial.md
sed -i 's/@fig:lists_hierarchy/24/g' Tutorial.md
sed -i 's/@fig:arrays_hierarchy/25/g' Tutorial.md
sed -i 's/@fig:cross_wins/26/g' Tutorial.md
sed -i 's/@fig:tictactoe_draw/27/g' Tutorial.md
sed -i 's/@fig:tictactoe_grid/28/g' Tutorial.md
sed -i 's/@fig:tictactoe_linear/29/g' Tutorial.md
sed -i 's/@fig:byte_array_hierarchy/30/g' Tutorial.md
sed -i 's/@fig:map_hierarchy/31/g' Tutorial.md
sed -i 's/@fig:erc20_hierarchy/32/g' Tutorial.md
sed -i 's/@fig:erc721_hierarchy/33/g' Tutorial.md
sed -i 's/@fig:node_hierarchy/34/g' Tutorial.md
sed -i 's/@fig:hotmoka_tendermint/35/g' Tutorial.md
sed -i 's/@fig:inbound_rules/36/g' Tutorial.md
sed -i 's/@fig:entities_hierarchy/37/g' Tutorial.md

# These get automatically recomputed with the update script: do not edit!
sed -i 's/@version/1.0.8/g' Tutorial.md
sed -i 's/@takamakaCode/c445a8f4f684fb50a9c86818515fe181d9711d4369e23e024182a7f5d1e33d67/g' Tutorial.md
sed -i 's/@manifest/5c8925c53825ecd3334e05925089f1b8d37c77ed1ed47e35a1c25666c093f271#0/g' Tutorial.md
sed -i 's/@gamete/0c9c4b4f3b112a9232ef307040d28350d6da886391385b59c36a6097cdd0bbdb#0/g' Tutorial.md
sed -i 's/@gasStation/5c8925c53825ecd3334e05925089f1b8d37c77ed1ed47e35a1c25666c093f271#14/g' Tutorial.md
sed -i 's/@validators/5c8925c53825ecd3334e05925089f1b8d37c77ed1ed47e35a1c25666c093f271#1/g' Tutorial.md
sed -i 's/@maxFaucet/10000000000000/g' Tutorial.md
sed -i 's/@chainid/marabunta/g' Tutorial.md
sed -i 's/@chainid/marabunta/g' state1_copy.fig
sed -i 's/@chainid/marabunta/g' state2_copy.fig
sed -i 's/@chainid/marabunta/g' state3_copy.fig
sed -i 's/@account1/660d7339698a87a98820e9afb58a194ed1000d76b1263fd5182e0c387494861a#0/g' Tutorial.md
sed -i 's/@short_account1/660d7339698...#0/g' state2_copy.fig
sed -i 's/@short_account1/660d7339698...#0/g' state3_copy.fig
sed -i 's/@36words_of_account1/ 1: vocal\n 2: rack\n 3: seat\n 4: response\n 5: point\n 6: fox\n 7: arch\n 8: depend\n 9: brave\n10: ceiling\n11: dutch\n12: express\n13: general\n14: ridge\n15: toss\n16: course\n17: marble\n18: era\n19: amused\n20: spy\n21: win\n22: shallow\n23: gown\n24: surface\n25: abandon\n26: hill\n27: proof\n28: erase\n29: wool\n30: metal\n31: return\n32: mandate\n33: innocent\n34: picnic\n35: make\n36: mad/g' Tutorial.md
sed -i "s/@publickeyaccount1/DfrZqIYMzTeM9Stz2pbL7hyFX7H9JvF4E+X2tj3hoJ8=/g" Tutorial.md
sed -i "s/@short_publickeyaccount1/DfrZqIYMzT.../g" state2_copy.fig
sed -i "s/@short_publickeyaccount1/DfrZqIYMzT.../g" state3_copy.fig
sed -i 's/@account_anonymous/e58: 413WtiSNBV2vofUgxA3JRuyvQ8hYY3fM7cdEKcDQTwF9/g' Tutorial.md
sed -i 's/@new_key/413WtiSNBV2vofUgxA3JRuyvQ8hYY3fM7cdEKcDQTwF9/g' Tutorial.md
sed -i 's/@family_address/6b1ecb28e34f0ad908076d13f643a81e4a81da01e113e47745ebf64332b07716/g' Tutorial.md
sed -i 's/@short_family_address/6b1ecb28e3.../g' state3_copy.fig
sed -i 's/@code_family_address/80b202b68e1de62904cb6d6cb426ba18ffff14385c462b0b69cabdff356722d1/g' Tutorial.md
sed -i 's/@family2_address/040f39f3bb9d3e14dbcd0924e1319b095081cab40ac834ee4537b2773d8ddd5e/g' Tutorial.md
sed -i 's/@family_exported_address/67d27764bebe45d2f5b981a9aeec47369e02ccb04cfdd6d64be86787df92fb32/g' Tutorial.md
sed -i 's/@family3_address/ed2fc2cfd9a30edfdfa50ace21c8a0a9d2f9d89ebc0d0d2050d6874c468eba35/g' Tutorial.md
sed -i 's/@person_object/07c98dae2c1955a7202a5b7f3f54a4a2c9a728a5500f1f2270a66b12a4ff49a0#0/g' Tutorial.md
sed -i 's/@person2_object/1246073057f815607dcda280954beb6f11263fe67a86ba14cd852a1347b59afe#0/g' Tutorial.md
sed -i 's/@person3_object/8b36bd65eeb5283b388343cf3b7b6e9efeb9434075d0bbb13ec7eff648802bed#0/g' Tutorial.md
sed -i 's/@gradual_ponzi_address/5b497a0f9060f189469ed366efcc578dac4ffa64ba141f6a7a36064fd52a4135/g' Tutorial.md
sed -i 's/@gradual_ponzi_object/f813d2ba716f403022a8d457b8a8193ee16bed27c3727fa012ff8a03da6309ff#0/g' Tutorial.md
sed -i 's/@gradual_ponzi_list/f813d2ba716f403022a8d457b8a8193ee16bed27c3727fa012ff8a03da6309ff#1/g' Tutorial.md
sed -i 's/@gradual_ponzi_first/f813d2ba716f403022a8d457b8a8193ee16bed27c3727fa012ff8a03da6309ff#2/g' Tutorial.md
sed -i 's/@gradual_ponzi_last/4f597e2194820c6c85fcb348b1e758854cb0d0de59056d712acc676231367335#0/g' Tutorial.md
sed -i 's/@account2/d644a9434807101b819ff2d1742728cce932cf1fa1bce08e7690e39bec850525#0/g' Tutorial.md
sed -i 's/@account3/3eb9267ff6f32a7f3e23298dddf2e2d5f7cdb9b3f1293b2f467fc15eaec78761#0/g' Tutorial.md
sed -i 's/@account4/e14aee0b7dbde28991dee8df50be35f883aad91d584299f0fa3de877b7cd2b77#0/g' Tutorial.md
sed -i 's/@account5/9c1fa307a89a6ec147cbd4c81758656d55961c7f4784d5fbe0787e94e01559aa#0/g' Tutorial.md
sed -i 's/@account6/e93822181a428732643b2786b8e2eda3fd1ad77a8ac334a630b0dc5ba9393598#0/g' Tutorial.md
sed -i 's/@account7/58bbb2020708006c3bfce43ec1b22862ee1c90bdcd83477657bb882e078c8254#0/g' Tutorial.md
sed -i 's/@publickeyaccount4/3DeNCce5Q9LnFrt9aJKssTw\/vJArtS5SqEQx3BhM5ME=/g' Tutorial.md
sed -i 's/@short_publickeyaccount5/MIIDQjCCAjUGByqGSM44BAEwggIoAo.../g' Tutorial.md
sed -i 's/@tictactoe_address/98757bf5de51208bb6f39861b28294300bee0f9b7ce8e3b6b3241e74dd0ca5c3/g' Tutorial.md
sed -i 's/@tictactoe_object/ca729b6b01fe9235bc3e24d46a88cfbe2719969d380433c9b42b42bf00549796#0/g' Tutorial.md
sed -i 's/@erc20_address/4ea842ed64da3722f9d876bb6d7cfeeb9efe10bbc37539c901cc2707b7d8d24d/g' Tutorial.md
sed -i 's/@erc20_object/e975302f0a23d4c157d787424e66f356207b1643063999ed90b80f5d3be0803b#0/g' Tutorial.md
sed -i 's/@server/panarea.hotmoka.io/g' Tutorial.md
sed -i 's/@new_docker_key/BpnB6J5DKbNTN3QRBjXNoZogTeCt7szuLjWGwaw7kqFK/g' Tutorial.md
sed -i 's/@container_id1/ab3111028e239c0132a603318163b761d40d60732208701d002f8779c5ca9513/g' Tutorial.md
sed -i 's/@docker_takamaka_code/2c3b2d553c108266d4b942dc9343dd67a366f4bc49f02c667dffa5c01970409d/g' Tutorial.md
sed -i 's/@docker_manifest/6c40fc42181e9a1591fb2f09c42194d803e0f20544a47b4f92ff5d2337382c84#0/g' Tutorial.md
sed -i 's/@docker_gamete/72c5cfc83e00fe711a64a6e03f35c49aa3f6030b67f41bdcc3d2e4bbfa354240#0/g' Tutorial.md
sed -i 's/@docker_validators/6c40fc42181e9a1591fb2f09c42194d803e0f20544a47b4f92ff5d2337382c84#1/g' Tutorial.md
sed -i 's/@docker_validator0/8a145aef9418569317084ecf5b29d40263901e52e69beef4c6ca92c62104f4f6#0/g' Tutorial.md
sed -i 's/@docker_id_validator0/4D3289603EAFD0B2EB93E94DE873029912F33506/g' Tutorial.md
sed -i 's/@container_id2/c64c7771b71ae2914366aaba358c1ac343d058e89d286db948780fce308ab005/g' Tutorial.md
sed -i 's/@container_id3/bf06ed57cdb0db124cb595c7e1fdb0c6cb79871a85eddb536130044b7b579fe5/g' Tutorial.md
sed -i 's/@docker_new_account/2027c30c45155c0b1c473b44ba8985a2de154c982d4f5ec6ec79fd7e0904a49e#0/g' Tutorial.md
sed -i 's/@docker_total_gas_new_account/44599/g' Tutorial.md
sed -i 's/@docker_reduced_balance/999999998720834/g' Tutorial.md
sed -i 's/@docker_balance_validator0/0/g' Tutorial.md
sed -i 's/@docker_staked_validator0/0/g' Tutorial.md
sed -i 's/@docker_diff1/1279166/g' Tutorial.md
sed -i 's/@docker_diff2/44599/g' Tutorial.md
sed -i 's/@docker_diff3/-44599/g' Tutorial.md
sed -i 's/@docker_sum1/0/g' Tutorial.md

# we regenerate the png figures, since they might contain some string changed
# by previous sed commands
fig2dev -L png -m 4 state1_copy.fig pics/state1.png
fig2dev -L png -m 4 state2_copy.fig pics/state2.png
fig2dev -L png -m 4 state3_copy.fig pics/state3.png
rm *_copy.fig

# Hotmoka-specific processing now...
cp Tutorial.md ProgrammingHotmoka.md
sed -i "s/\[Hotmokaonly]://g" ProgrammingHotmoka.md

# These must be edited by hand since, for instance, they depend on accounts created in Mokito or on the configuration (hotmoka/blueknot)
sed -i 's/@tendermint_version/0.34.15/g' ProgrammingHotmoka.md
sed -i 's/@tool_repo/https:\/\/github.com\/Hotmoka\/hotmoka/g' ProgrammingHotmoka.md
sed -i 's/@tool/moka/g' ProgrammingHotmoka.md
sed -i 's/@Tool/Moka/g' ProgrammingHotmoka.md
sed -i 's/@app_repo/https:\/\/github.com\/Hotmoka\/HotmokaAndroid/g' ProgrammingHotmoka.md
sed -i 's/@app_id_play/io.hotmoka.android.mokito/g' ProgrammingHotmoka.md
sed -i 's/@app/mokito/g' ProgrammingHotmoka.md
sed -i 's/@App/Mokito/g' ProgrammingHotmoka.md
sed -i 's/@type/hotmoka/g' ProgrammingHotmoka.md
sed -i 's/@Type/Hotmoka/g' ProgrammingHotmoka.md
sed -i 's/@docker_hub_user/hotmoka/g' ProgrammingHotmoka.md
sed -i 's/@docker_user/hotmoka/g' ProgrammingHotmoka.md
sed -i 's/@hotwallet_repo/https:\/\/github.com\/Hotmoka\/hotwallet-browser/g' ProgrammingHotmoka.md
sed -i 's/@hotweb3_repo/https:\/\/github.com\/Hotmoka\/hotweb3/g' ProgrammingHotmoka.md
sed -i 's/@fausto_email/fausto.spoto@hotmoka.io/g' ProgrammingHotmoka.md
sed -i 's/@tutorial_repo/https:\/\/github.com\/Hotmoka\/hotmoka_tutorial.git/g' ProgrammingHotmoka.md
sed -i 's/@tutorial_name/hotmoka_tutorial/g' ProgrammingHotmoka.md
sed -i 's/@account_mokito/701e20be588db820744df467826d67b9fe451406d7f75da6ef8aeb6805a7365f#0/g' ProgrammingHotmoka.md
sed -i 's/@36words_of_account_mokito/word #1: rail\nword #2: double\nword #3: bag\nword #4: dove\nword #5: fluid\n...\nword #34: bounce\nword #35: deposit\nword #36: hotel/g' ProgrammingHotmoka.md

cp ProgrammingHotmoka.md temp.md
sed -i "/^\[PDFonly]:/d" ProgrammingHotmoka.md
sed -i "s/\[Markdownonly]://g" ProgrammingHotmoka.md

# generate the PDF version now
sed -i "/^\[Markdownonly]:/d" temp.md
sed -i "s/\[PDFonly]://g" temp.md
pandoc temp.md -o ProgrammingHotmoka.tex --include-in-header mystylefile_hotmoka.tex --include-after-body backcover.tex --toc --highlight-style=kate -V geometry:a4paper -V documentclass:book -V pagestyle:headings -V papersize:a4 -V colorlinks:true
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
pandoc -o ProgrammingHotmoka.epub metadata-hotmoka.yaml ProgrammingHotmoka.md
rm ProgrammingHotmoka.md

# generate the mobi version of the document
ebook-convert ProgrammingHotmoka.epub ProgrammingHotmoka.mobi

# Blueknot-specific processing now...
cp Tutorial.md ProgrammingBlueknot.md

# some paragraphs do not exist in the Blueknot version
sed -i "/^\[Hotmokaonly]:/d" ProgrammingBlueknot.md

# These must be edited by hand since, for instance, they depend on accounts created in Mokito or on the configuration (hotmoka/blueknot)
sed -i 's/@tendermint_version/0.34.15/g' ProgrammingBlueknot.md
sed -i 's/@tool_repo/https:\/\/github.com\/Vero4Chain\/blueknot/g' ProgrammingBlueknot.md
sed -i 's/@tool/blue/g' ProgrammingBlueknot.md
sed -i 's/@Tool/Blue/g' ProgrammingBlueknot.md
sed -i 's/@app_repo/https:\/\/github.com\/Vero4Chain\/BlueknotAndroid/g' ProgrammingBlueknot.md
sed -i 's/@app_id_play/it.vero4chain.android.bluewallet/g' ProgrammingBlueknot.md
sed -i 's/@app/bluewallet/g' ProgrammingBlueknot.md
sed -i 's/@App/Bluewallet/g' ProgrammingBlueknot.md
sed -i 's/@type/blueknot/g' ProgrammingBlueknot.md
sed -i 's/@Type/Blueknot/g' ProgrammingBlueknot.md
sed -i 's/@docker_hub_user/veroforchain/g' ProgrammingBlueknot.md
sed -i 's/@docker_user/blueknot/g' ProgrammingBlueknot.md
sed -i 's/@hotwallet_repo/https:\/\/github.com\/Vero4Chain\/hotwallet-browser/g' ProgrammingBlueknot.md
sed -i 's/@hotweb3_repo/https:\/\/github.com\/Vero4Chain\/hotweb3/g' ProgrammingBlueknot.md
sed -i 's/@fausto_email/fausto.spoto@vero4chain.it/g' ProgrammingBlueknot.md
sed -i 's/@tutorial_repo/https:\/\/github.com\/Vero4Chain\/blueknot_tutorial.git/g' ProgrammingBlueknot.md
sed -i 's/@tutorial_name/blueknot_tutorial/g' ProgrammingBlueknot.md
sed -i 's/@account_mokito/701e20be588db820744df467826d67b9fe451406d7f75da6ef8aeb6805a7365f#0/g' ProgrammingBlueknot.md
sed -i 's/@36words_of_account_mokito/word #1: rail\nword #2: double\nword #3: bag\nword #4: dove\nword #5: fluid\n...\nword #34: bounce\nword #35: deposit\nword #36: hotel/g' ProgrammingBlueknot.md

cp ProgrammingBlueknot.md temp.md
sed -i "/^\[PDFonly]:/d" ProgrammingBlueknot.md
sed -i "s/\[Markdownonly]://g" ProgrammingBlueknot.md

# generate the PDF version now
sed -i "/^\[Markdownonly]:/d" temp.md
sed -i "s/\[PDFonly]://g" temp.md
pandoc temp.md -o ProgrammingBlueknot.tex --include-in-header mystylefile_blueknot.tex --include-after-body backcover.tex --toc --highlight-style=kate -V geometry:a4paper -V documentclass:book -V pagestyle:headings -V papersize:a4 -V colorlinks:true
rm temp.md
sed -i 's/\\begin{verbatim}/\\begin{myverbatim}\n\\begin{verbatim}/g' ProgrammingBlueknot.tex
sed -i 's/\\end{verbatim}/\\end{verbatim}\n\\end{myverbatim}/g' ProgrammingBlueknot.tex
sed -i 's/103 \& alicudi/$10^3$ \& alicudi/g' ProgrammingBlueknot.tex
sed -i 's/106 \& filicudi/$10^6$ \& filicudi/g' ProgrammingBlueknot.tex
sed -i 's/109 \& stromboli/$10^9$ \& stromboli/g' ProgrammingBlueknot.tex
sed -i 's/1012 \& vulcano/$10^{12}$ \& vulcano/g' ProgrammingBlueknot.tex
sed -i 's/1015 \& salina/$10^{15}$ \& salina/g' ProgrammingBlueknot.tex
sed -i 's/1018 \& lipari/$10^{18}$ \& lipari/g' ProgrammingBlueknot.tex
sed -i 's/1021 \& moka/$10^{21}$ \& moka/g' ProgrammingBlueknot.tex
sed -i 's/\\chapterfont{\\clearpage}//g' ProgrammingBlueknot.tex
sed -i 's/\\chapter{Table of Contents}/\\begin{comment}\\chapter{Table of Contents}/g' ProgrammingBlueknot.tex
sed -i 's/\\hypertarget{introduction}/\\end{comment}\n\n\\hypertarget{introduction}/g' ProgrammingBlueknot.tex

# delete the \begin{document}
sed -i 's/\\begin{document}//g' ProgrammingBlueknot.tex
# place \begin{document} before \BgThispage
sed -i 's/\\BgThispage/\\begin{document}\n\\BgThispage/g' ProgrammingBlueknot.tex

pdflatex ProgrammingBlueknot.tex

# mv ProgrammingBlueknot.md ../README.md

# generate the epub version of the document
# we remove the first lines of the Markdown, that contain Java build information
tail -n +6 ../README.md > ProgrammingBlueknot.md
pandoc -o ProgrammingBlueknot.epub metadata-blueknot.yaml ProgrammingBlueknot.md
rm ProgrammingBlueknot.md

# generate the mobi version of the document
ebook-convert ProgrammingBlueknot.epub ProgrammingBlueknot.mobi

rm Tutorial.md
