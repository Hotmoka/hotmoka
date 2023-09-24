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
sed -i 's/@takamakaCode/645fe5273407ba4dbbcbbdd2ab08238357291de20f8aa6bccb0adf84d9de43e0/g' Tutorial.md
sed -i 's/@manifest/d622321837529cb901706b20d878a0226a6d102dde0e2f76f487a23b6a3fa581#0/g' Tutorial.md
sed -i 's/@gamete/6ae9773bd9fe77981bf17a10573ca7663e54f561b6f039febff83a7657de0c54#0/g' Tutorial.md
sed -i 's/@gasStation/d622321837529cb901706b20d878a0226a6d102dde0e2f76f487a23b6a3fa581#14/g' Tutorial.md
sed -i 's/@validators/d622321837529cb901706b20d878a0226a6d102dde0e2f76f487a23b6a3fa581#1/g' Tutorial.md
sed -i 's/@maxFaucet/10000000000000/g' Tutorial.md
sed -i 's/@chainid/marabunta/g' Tutorial.md
sed -i 's/@chainid/marabunta/g' state1_copy.fig
sed -i 's/@chainid/marabunta/g' state2_copy.fig
sed -i 's/@chainid/marabunta/g' state3_copy.fig
sed -i 's/@account1/ea3966e3e055e02695ccca9d6bd9d2eadc440182672f2c9ae2d93f8d5f13725c#0/g' Tutorial.md
sed -i 's/@short_account1/ea3966e3e05...#0/g' state2_copy.fig
sed -i 's/@short_account1/ea3966e3e05...#0/g' state3_copy.fig
sed -i 's/@36words_of_account1/ 1: game\n 2: ribbon\n 3: tourist\n 4: pair\n 5: pact\n 6: tape\n 7: piece\n 8: learn\n 9: ten\n10: pony\n11: nephew\n12: fiction\n13: phone\n14: recall\n15: moon\n16: apple\n17: then\n18: cruise\n19: industry\n20: news\n21: stove\n22: undo\n23: entry\n24: fortune\n25: dune\n26: army\n27: oil\n28: furnace\n29: situate\n30: imitate\n31: gossip\n32: vast\n33: gallery\n34: oppose\n35: novel\n36: cradle/g' Tutorial.md
sed -i "s/@publickeyaccount1/mjc9gJKZi5hWFwBYdQz1QZ9RaoQohfz3cuDOd0M9xwA=/g" Tutorial.md
sed -i "s/@short_publickeyaccount1/mjc9gJKZi5.../g" state2_copy.fig
sed -i "s/@short_publickeyaccount1/mjc9gJKZi5.../g" state3_copy.fig
sed -i 's/@account_anonymous/1fc17de19c200d669980cded0df9f2034eb50473e9cb1c5997087ca368468c2b#0/g' Tutorial.md
sed -i 's/@new_key/AHJjJGwWRcDauc6Nn5ySmi9Kcdsggd6Kpv6vGewnFhZM/g' Tutorial.md
sed -i 's/@family_address/036a66dd9e34c0ef74c536e25eb7809962fcb5cdd93499cbff1bc294a3a0b2e3/g' Tutorial.md
sed -i 's/@short_family_address/036a66dd9e.../g' state3_copy.fig
sed -i 's/@code_family_address/4f836a4c46cdec9399231a5f00349222050a880326e82d2f1f68fac08fd66e8a/g' Tutorial.md
sed -i 's/@family2_address/57d02997a22e307b8ba0014c5a598adcf96d5c963a40eacf15109a1902216e2c/g' Tutorial.md
sed -i 's/@family_exported_address/fe4f7fed3774cdb61c6f00f657d4305ec0eee65968b70916c34c1a30149110a9/g' Tutorial.md
sed -i 's/@family3_address/18de02b33609ac4d06e8ff001ccb559892f68aaed940095086f6e3dbf6751a70/g' Tutorial.md
sed -i 's/@person_object/e0abd6b48248a6729273610851bf6ed4acb26dc8c89ce1c7342e37509cab42a2#0/g' Tutorial.md
sed -i 's/@person2_object/3d75367f2275c92f491d791e3124a5629ec21129fc658c78e76a9261826f7de5#0/g' Tutorial.md
sed -i 's/@person3_object/f1d1ac4c87f3aa43ec6253a6e783680d155ce0a3d88ab13a68bafcc70a422522#0/g' Tutorial.md
sed -i 's/@gradual_ponzi_address/aa753be3cd67e5459bf2c0203a931aa2612ea23b7e43db679b55bad77cb702b1/g' Tutorial.md
sed -i 's/@gradual_ponzi_object/0ecb15b051dffeda927faa0771aa289b4a13925759a60b7395f412c9a2677646#0/g' Tutorial.md
sed -i 's/@gradual_ponzi_list/0ecb15b051dffeda927faa0771aa289b4a13925759a60b7395f412c9a2677646#1/g' Tutorial.md
sed -i 's/@gradual_ponzi_first/0ecb15b051dffeda927faa0771aa289b4a13925759a60b7395f412c9a2677646#2/g' Tutorial.md
sed -i 's/@gradual_ponzi_last/ce11a6452e5a039e0e0fc426826c1e4673d1f58bb4ff5e67ab37a779d4672fb0#0/g' Tutorial.md
sed -i 's/@account2/4d2c0008b43f68392168c4a97dcd397a48c011780794f7387262d8f2cfc82f58#0/g' Tutorial.md
sed -i 's/@account3/6bda363c355d24f8f8b59e95237dcf19e918a662fbfb71fb5a89f47d81a24aba#0/g' Tutorial.md
sed -i 's/@account4/05c7748b69e1d792c5cfcc8ded9c16ca159cb9a65eca6036fe0180473e6d0102#0/g' Tutorial.md
sed -i 's/@account5/03886c570e99a13462c0d16087b14715848ead458e566054976c39ea494de5dd#0/g' Tutorial.md
sed -i 's/@account6/702413644f554e6ea4b60f5b1eb1263716342d86812dcf0ba61c5014eda30ae9#0/g' Tutorial.md
sed -i 's/@account7/cea6e5cad68ab369fb12ecf2d9c60341bec360001c8da1bb9f6d314c0c4b2107#0/g' Tutorial.md
sed -i 's/@publickeyaccount4/amC0Jd0PPzJ2USolpy\/0BvmF7EPYqt9xn+pzuotTdv4=/g' Tutorial.md
sed -i 's/@short_publickeyaccount5/MIIDQjCCAjUGByqGSM44BAEwggIoAo.../g' Tutorial.md
sed -i 's/@tictactoe_address/9dba9423524a5a022b7f576cc5bbdd0b0b7aede9e3578cfed425b97aeca8d66b/g' Tutorial.md
sed -i 's/@tictactoe_object/3b65c76db6d4803bdaea80ddd8f28c5dcc388ca931093212753db08a3a57049e#0/g' Tutorial.md
sed -i 's/@erc20_address/a98ae8684ada190ff47f6fa83385c1d8542fabe2f2454984daadd301db7a5d6a/g' Tutorial.md
sed -i 's/@erc20_object/73b4d143ffe5fee731f54dd2bfb2ba0388a00b508734399ed03b8c4e1a1b1096#0/g' Tutorial.md
sed -i 's/@server/panarea.hotmoka.io/g' Tutorial.md
sed -i 's/@new_docker_key/En2EJywapYumNDjNZAFUMVjP26oTFXQqMtYURi4VuTxY/g' Tutorial.md
sed -i 's/@container_id1/19f0e151bbc222f722c99e6dd49abe0d23e369bf2c45680610ca8cb0ee15da9a/g' Tutorial.md
sed -i 's/@docker_takamaka_code/10added23aafb1074091735f7fd8946c05af88691ad4ac02af38a4e664122b1d/g' Tutorial.md
sed -i 's/@docker_manifest/bafdcc0414afa8599e7642e195c1705d73df755c9863ff8a99bc6192ca24964c#0/g' Tutorial.md
sed -i 's/@docker_gamete/34d9b6edffa89c17294fff821b88bd47aa3c24bef0adaec31ddcf12447b21480#0/g' Tutorial.md
sed -i 's/@docker_validators/bafdcc0414afa8599e7642e195c1705d73df755c9863ff8a99bc6192ca24964c#1/g' Tutorial.md
sed -i 's/@docker_validator0/eea5ca79f44d507622f6770fad97223111ed3e9f5f03eede7404d6a46827ce72#0/g' Tutorial.md
sed -i 's/@docker_id_validator0/C2BC1D8742CA6FF2ED86F634F01BF4B1224E9A30/g' Tutorial.md
sed -i 's/@container_id2/8bf8356be95cf76954822eae4708630d2dc33c9dba06949664f46bc3ea4e7412/g' Tutorial.md
sed -i 's/@container_id3/e2c04d1335d8ae717b49ea30a6f57c44d27dd586c79d5f3af82771be884ebee6/g' Tutorial.md
sed -i 's/@docker_new_account/dd0e4e1d86858f6e67d683f8ed93c888010cf3d9c8bc3384984fec260c7e603a#0/g' Tutorial.md
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
