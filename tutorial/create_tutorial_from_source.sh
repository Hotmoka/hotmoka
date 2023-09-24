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
sed -i 's/@hotmoka_version/1.3.0/g' Tutorial.md
sed -i 's/@takamakaCode/e8f248fea98e1c2062bc427b3f90e5a5cbe564cede9583292ac37ffe55c74afa/g' Tutorial.md
sed -i 's/@manifest/ba7440e7eb6d746992509ad6c2e95b99d14b65a4f35cbd087d7e55b532848ce1#0/g' Tutorial.md
sed -i 's/@gamete/a5c78f32fe8e80140082049e736bb6ed4d7a805f14dc1da728d0a88720ec78a9#0/g' Tutorial.md
sed -i 's/@gasStation/ba7440e7eb6d746992509ad6c2e95b99d14b65a4f35cbd087d7e55b532848ce1#14/g' Tutorial.md
sed -i 's/@validators/ba7440e7eb6d746992509ad6c2e95b99d14b65a4f35cbd087d7e55b532848ce1#1/g' Tutorial.md
sed -i 's/@maxFaucet/10000000000000/g' Tutorial.md
sed -i 's/@chainid/marabunta/g' Tutorial.md
sed -i 's/@chainid/marabunta/g' state1_copy.fig
sed -i 's/@chainid/marabunta/g' state2_copy.fig
sed -i 's/@chainid/marabunta/g' state3_copy.fig
sed -i 's/@account1/3290de7bdcb50522448a426160581bf3c58cb7480569ecc3358a9800c2477ee1#0/g' Tutorial.md
sed -i 's/@short_account1/3290de7bdcb...#0/g' state2_copy.fig
sed -i 's/@short_account1/3290de7bdcb...#0/g' state3_copy.fig
sed -i 's/@36words_of_account1/ 1: ticket\n 2: next\n 3: wash\n 4: couple\n 5: code\n 6: episode\n 7: public\n 8: surprise\n 9: hundred\n10: garden\n11: stadium\n12: media\n13: cinnamon\n14: hundred\n15: urge\n16: note\n17: ahead\n18: banana\n19: card\n20: luggage\n21: bid\n22: clown\n23: hurt\n24: judge\n25: million\n26: ripple\n27: access\n28: stable\n29: slow\n30: onion\n31: clerk\n32: abandon\n33: seat\n34: desk\n35: reveal\n36: retire/g' Tutorial.md
sed -i "s/@publickeyaccount1/aBVS2tzgccs5vDUiadadMnL8OjeIYMFBpsOcSzXpi0g=/g" Tutorial.md
sed -i "s/@short_publickeyaccount1/aBVS2tzgcc.../g" state2_copy.fig
sed -i "s/@short_publickeyaccount1/aBVS2tzgcc.../g" state3_copy.fig
sed -i 's/@account_anonymous/95f78e7e129f872f1916d6ab07e069ab0d6d285b7737a0aed61f3c600220f0cf#0/g' Tutorial.md
sed -i 's/@new_key/EAsUaGohV29uuiBDZQv4Az6D4d6uuJP3FxiJ7Zj1cJUF/g' Tutorial.md
sed -i 's/@family_address/31b76020a07a1cb703ddcf9406f2a53cc70029b64c5ea2ee99de62612d26ee1c/g' Tutorial.md
sed -i 's/@short_family_address/31b76020a0.../g' state3_copy.fig
sed -i 's/@code_family_address/7c1db47acbe07fcbbc5addc9060cdd8dc010ab269b64676778eefe53376167cb/g' Tutorial.md
sed -i 's/@family2_address/865ed2ef744e65df42029661a068cbaeafe6738a5f44c44efe65848072bdf5d8/g' Tutorial.md
sed -i 's/@family_exported_address/4725f9898df4e0022cba3dd44b6d70bce5ffe8ec42675c2de4b3bed0eb07b8fb/g' Tutorial.md
sed -i 's/@family3_address/de5d9beba8f03dad39059a930242f127bc1206f10daaeebf2b8a44f614afffcb/g' Tutorial.md
sed -i 's/@person_object/4dff19ae06c7976ed0a72460055d4ba784b3de8bf19a5d7f54aed1c30300f92b#0/g' Tutorial.md
sed -i 's/@person2_object/c1588220bed63c9ed9c8447c30bbe07d4a93755bbe0fc71e8eea2d0a9f96c6c5#0/g' Tutorial.md
sed -i 's/@person3_object/cd429736c0b23c50152507748061f8588ad3aff7ba829d44f80acc72a8954159#0/g' Tutorial.md
sed -i 's/@gradual_ponzi_address/fa66f88a08ac40eaa2a71890a7a7ebfa474291f94d8cdecbbfbb1db90793b2ba/g' Tutorial.md
sed -i 's/@gradual_ponzi_object/c54dee0be6c4e8bb56e71a3ce399d65bbeebbf726b28c4edc1b1c8fd02cb5a0c#0/g' Tutorial.md
sed -i 's/@gradual_ponzi_list/c54dee0be6c4e8bb56e71a3ce399d65bbeebbf726b28c4edc1b1c8fd02cb5a0c#1/g' Tutorial.md
sed -i 's/@gradual_ponzi_first/c54dee0be6c4e8bb56e71a3ce399d65bbeebbf726b28c4edc1b1c8fd02cb5a0c#2/g' Tutorial.md
sed -i 's/@gradual_ponzi_last/6e761f296e198e3a30b2a1a09436f8ae3045ced1e8a813a56f4535ac2c98cbf1#0/g' Tutorial.md
sed -i 's/@account2/ab03328645589defeb289fd49a5fa7b6d41d1475d307565717b568e4cd4e7bec#0/g' Tutorial.md
sed -i 's/@account3/b9372abbd001a7588a0a2d0b9bcca6aee5c9fc943b0227e664c5f1f53bc2d443#0/g' Tutorial.md
sed -i 's/@account4/48866034dce23c41a5711bf431f26cc2659c7bef10732dfb18c904954dcb47b0#0/g' Tutorial.md
sed -i 's/@account5/d807984ad05c8d1485f82faeb9945873681c749e6ddae3980591f7402290a135#0/g' Tutorial.md
sed -i 's/@account6/3f7a251fa30ad03d628b54617ed65f3408d8e836533e5d7f822f6a77a7bef97d#0/g' Tutorial.md
sed -i 's/@account7/c9fe4af7456feef2d7d30458f8e4f712e73f7abe52b24b58454173b3b6d89dc8#0/g' Tutorial.md
sed -i 's/@publickeyaccount4/uWM71209Q2zanHxwvYT\/UlcSsf78mxQaozOORgSlEA8=/g' Tutorial.md
sed -i 's/@short_publickeyaccount5/MIIDQjCCAjUGByqGSM44BAEwggIoAo.../g' Tutorial.md
sed -i 's/@tictactoe_address/ddd2511e1cc35f73afec28bafe15f702dba9e079b8155516e0cd6e73108f6abc/g' Tutorial.md
sed -i 's/@tictactoe_object/095a8a8cd2e177c67e2ce1081c8ad051306ce2bc2a7c4c4218971837ff8d3081#0/g' Tutorial.md
sed -i 's/@erc20_address/8514144ad96106ee959bea3307bfd28422b7eab2414a051fe4de782f547aa815/g' Tutorial.md
sed -i 's/@erc20_object/99777292a2241da8f7e8bffa3fec2b34e8730dafd94d3c6fad1bd419aaf19635#0/g' Tutorial.md
sed -i 's/@server/panarea.hotmoka.io/g' Tutorial.md
sed -i 's/@new_docker_key/EzMBHXA6ZEcK9ymt6ivG7b3z4vg3PcSKoKdCympk4QMJ/g' Tutorial.md
sed -i 's/@container_id1/c673a5bb5e000359091613f9ce1cf9d3f1598df9fdf4bcb2c31b805d8b0af83d/g' Tutorial.md
sed -i 's/@docker_takamaka_code/36c0ddd98c768e25589ef89f46ceb1d4e519eabe6b30e3fdeb184e9e498ff415/g' Tutorial.md
sed -i 's/@docker_manifest/7784aef4403376e8067901ea4c9eaaa7a2ca3e64db7082dd53c3b55894d012e1#0/g' Tutorial.md
sed -i 's/@docker_gamete/b31606ce2dfa508f1ea54437efdd1522114579c323cfc90946aa4832fc641f6e#0/g' Tutorial.md
sed -i 's/@docker_validators/7784aef4403376e8067901ea4c9eaaa7a2ca3e64db7082dd53c3b55894d012e1#1/g' Tutorial.md
sed -i 's/@docker_validator0/f57b1eb4d702066cd50088142dfc34ee305a262691745c0932329f93a4250753#0/g' Tutorial.md
sed -i 's/@docker_id_validator0/A403E8A15718EC8C10FDA10EF9300EE4B9FE6DCF/g' Tutorial.md
sed -i 's/@container_id2/909836b22d1ddc2604f08479b75078314c719c0f7c3a6af99bf30cea4f5ca329/g' Tutorial.md
sed -i 's/@container_id3/e15bd7597d14d42c0b776ee39d986096a4e8de4a4fe750672040e6bcd1531076/g' Tutorial.md
sed -i 's/@docker_new_account/f2f9f69df86120e1460ae79d64997f517f5474c3f8486cb9ebcadeb2afc28c11#0/g' Tutorial.md
sed -i 's/@docker_total_gas_new_account/2579/g' Tutorial.md
sed -i 's/@docker_reduced_balance/999999998762854/g' Tutorial.md
sed -i 's/@docker_balance_validator0/0/g' Tutorial.md
sed -i 's/@docker_staked_validator0/0/g' Tutorial.md
sed -i 's/@docker_diff1/1237146/g' Tutorial.md
sed -i 's/@docker_diff2/2579/g' Tutorial.md
sed -i 's/@docker_diff3/-2579/g' Tutorial.md
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
sed -i 's/@takamaka_version/1.0.13/g' ProgrammingHotmoka.md
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
sed -i 's/@takamaka_version/1.0.13/g' ProgrammingBlueknot.md
sed -i 's/@tool_repo/https:\/\/github.com\/Vero4Chain\/blueknot/g' ProgrammingBlueknot.md
sed -i 's/@tool/blue/g' ProgrammingBlueknot.md
sed -i 's/@Tool/Blue/g' ProgrammingBlueknot.md
sed -i 's/@app_repo/https:\/\/github.com\/Vero4Chain\/BlueknotAndroid/g' ProgrammingBlueknot.md
sed -i 's/@app_id_play/it.vero4chain.android.bluewally/g' ProgrammingBlueknot.md
sed -i 's/@app/bluewally/g' ProgrammingBlueknot.md
sed -i 's/@App/Bluewally/g' ProgrammingBlueknot.md
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
