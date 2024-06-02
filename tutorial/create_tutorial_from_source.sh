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
sed -i 's/@hotmoka_version/1.6.0/g' Tutorial.md
sed -i 's/@takamakaCode/776a721ab24a0daaad8f128307ee090069646437be9469b879cfa8b8c6526afe/g' Tutorial.md
sed -i 's/@manifest/323eda20a41176da86f0967a86aae5154e97aae163cdbc28f8e50b36bf11f48f#0/g' Tutorial.md
sed -i 's/@gamete/04355968e61a05f6eb9c2c2c0270cc88cb821b80876d9db351ec83f21391a80b#0/g' Tutorial.md
sed -i 's/@gasStation/323eda20a41176da86f0967a86aae5154e97aae163cdbc28f8e50b36bf11f48f#14/g' Tutorial.md
sed -i 's/@validators/323eda20a41176da86f0967a86aae5154e97aae163cdbc28f8e50b36bf11f48f#1/g' Tutorial.md
sed -i 's/@maxFaucet/10000000000000/g' Tutorial.md
sed -i 's/@chainid/marabunta/g' Tutorial.md
sed -i 's/@chainid/marabunta/g' state1_copy.fig
sed -i 's/@chainid/marabunta/g' state2_copy.fig
sed -i 's/@chainid/marabunta/g' state3_copy.fig
sed -i 's/@account1/595548f802fb6c2e9eb1f3ad4f8b3c93fe59d0144df2f59e627fb7267ef1bae0#0/g' Tutorial.md
sed -i 's/@short_account1/595548f802f...#0/g' state2_copy.fig
sed -i 's/@short_account1/595548f802f...#0/g' state3_copy.fig
sed -i 's/@36words_of_account1/ 1: awkward\n 2: train\n 3: mask\n 4: inform\n 5: ridge\n 6: smoke\n 7: scissors\n 8: obvious\n 9: dumb\n10: angle\n11: segment\n12: believe\n13: next\n14: piece\n15: theme\n16: garage\n17: history\n18: insane\n19: property\n20: trap\n21: stay\n22: sheriff\n23: venture\n24: lemon\n25: flush\n26: level\n27: beach\n28: tourist\n29: fly\n30: couple\n31: yard\n32: tooth\n33: leave\n34: miss\n35: retreat\n36: hair/g' Tutorial.md
sed -i "s/@publickeyaccount1/3m\/WLUPeXeNTL\/PbnqBL3M9Pt2EzMvyFSlEPsmdLDo8=/g" Tutorial.md
sed -i "s/@short_publickeyaccount1/3m\/WLUPeXe.../g" state2_copy.fig
sed -i "s/@short_publickeyaccount1/3m\/WLUPeXe.../g" state3_copy.fig
sed -i 's/@account_anonymous/1098c9c732fc164bf4e22ad551ec2e5b531590bad4e3ee31820f011490085085#0/g' Tutorial.md
sed -i 's/@new_key/EnRbA7uAxNvDxJKbPQ5vh6PTWcewfhnunf1CfH47og8T/g' Tutorial.md
sed -i 's/@family_address/c528447126a378e816d64bf811374dceb691cc11d89589f6dca95b919adda62f/g' Tutorial.md
sed -i 's/@short_family_address/c528447126.../g' state3_copy.fig
sed -i 's/@code_family_address/7e33772fa98a58ec0e6cb80a7d76bb8410e208d77ad7e062a781782f20ed4077/g' Tutorial.md
sed -i 's/@family2_address/41a90f9525299c5df98113275ad73c7335c091446bfa5dda78394f3c7fef9ca3/g' Tutorial.md
sed -i 's/@family_exported_address/9c9866798810d966605ecdaacb8130c5d644c77ee518fe3273ac75930e107ce8/g' Tutorial.md
sed -i 's/@family3_address/54763a31c8cdf56af5f09643c9a185cedb7eb53a6eaab057fb275e7620fb709d/g' Tutorial.md
sed -i 's/@person_object/a2cbcac5034741bd711d29866cd7c6db169cee61e31817db3b40c8daf964a2d8#0/g' Tutorial.md
sed -i 's/@person2_object/121cb3b56d7fd61828b87e6e9a68bde8c14049a86d9b8156480104bc66c4f19b#0/g' Tutorial.md
sed -i 's/@person3_object/2111769886f5760d7431ab0cc2044bfe3d967ee7b67fd6917b54955a2b1db237#0/g' Tutorial.md
sed -i 's/@gradual_ponzi_address/cbae2bc748bf266d0a14061d84fe0bee3f1ff91070a47905b42e1a259e06aeef/g' Tutorial.md
sed -i 's/@gradual_ponzi_object/81443a1d24d3b7d26820c6a6b09a7d00b7ec341c46fc5db223d47393c949bf19#0/g' Tutorial.md
sed -i 's/@gradual_ponzi_list/81443a1d24d3b7d26820c6a6b09a7d00b7ec341c46fc5db223d47393c949bf19#1/g' Tutorial.md
sed -i 's/@gradual_ponzi_first/81443a1d24d3b7d26820c6a6b09a7d00b7ec341c46fc5db223d47393c949bf19#2/g' Tutorial.md
sed -i 's/@gradual_ponzi_last/36d36b397c0ae0a99058b93aadd11d0ab8edc217033f2d4f979f00bcaa9d9840#0/g' Tutorial.md
sed -i 's/@account2/af84c36211ed0b3b9e4b5a0c94a80f6d250be891f7e37dd87db20c5559e52b76#0/g' Tutorial.md
sed -i 's/@account3/61d7083cb775081f62bd3560074a898e88385833c254add0fbd50982b30ee7cf#0/g' Tutorial.md
sed -i 's/@account4/399952f7c726021aeeb8db4bdc168387525aa4c1732b47cabc29db1541532430#0/g' Tutorial.md
sed -i 's/@account5/4f3d5aa316baedc080bd75cfa0ed80042c049644451bbe21490a4f8aaddaf458#0/g' Tutorial.md
sed -i 's/@account6/5c3428e7967b71ca4af3c236960c93e45da09961fdc75b9f6a695910c7ddcf1d#0/g' Tutorial.md
sed -i 's/@account7/411813d7049a0bb8e3adfa2d3623bd03eae1d92654668a35f75d0fcf26f7d5e5#0/g' Tutorial.md
sed -i 's/@publickeyaccount4/i0nn\/zcy4AIPy6rref9rPjTq\/qVNIn8Zi7iPET9wLpE=/g' Tutorial.md
sed -i 's/@short_publickeyaccount5/MIIDRjCCAjkGByqGSM44BAEwggIsAo.../g' Tutorial.md
sed -i 's/@tictactoe_address/6bfc957d64cefee01950a9c185e7abfc83333b5632386c7af827bfa919cfe884/g' Tutorial.md
sed -i 's/@tictactoe_object/85c41cf3c548f997c5bf791d2caaec0a9d13e6f442ce651df128c4a77d9de803#0/g' Tutorial.md
sed -i 's/@erc20_address/b72ee0386d4ecfd9a319c247da4e6f7126ac1d5eb06fbffd18020ce7effa0e0a/g' Tutorial.md
sed -i 's/@erc20_object/d9d0d07bf8b534084257122e10d5ca4d095f290d2efd0a346b99de9afb64f1aa#0/g' Tutorial.md
sed -i 's/@server/ws:\/\/panarea.hotmoka.io/g' Tutorial.md
sed -i 's/@new_docker_key/9YWXKfU1mot3twaU8WD885GGTrzAK1A8v2JUohwBXkcD/g' Tutorial.md
sed -i 's/@container_id1/cf8d78e004372082466abfff9a5f58d97f2cbbaa02cfef97e906e2f3132e68cc/g' Tutorial.md
sed -i 's/@docker_takamaka_code/776a721ab24a0daaad8f128307ee090069646437be9469b879cfa8b8c6526afe/g' Tutorial.md
sed -i 's/@docker_manifest/477cb65e250c45f411e75b421de087f35e87454aae56c996d08cedecfe71b745#0/g' Tutorial.md
sed -i 's/@docker_gamete/8ee52ae1790422d4738845ff1e54c0232de55f3042e40720034e8cab909c2eae#0/g' Tutorial.md
sed -i 's/@docker_validators/477cb65e250c45f411e75b421de087f35e87454aae56c996d08cedecfe71b745#1/g' Tutorial.md
sed -i 's/@docker_validator0/e3ec768d5c4eacae8062854e84f8e848809e8fbf5b968b23fd6d7ec942bd3c57#0/g' Tutorial.md
sed -i 's/@docker_id_validator0/684CC341743E300E7A0E2718AA15788834526EE8/g' Tutorial.md
sed -i 's/@container_id2/603015fe011c516e63196566ec1ede0ebae579bdadabb5a0443ee3b370870541/g' Tutorial.md
sed -i 's/@container_id3/95d5cc7ba10b4ce6f14712786f8c3c14577d2c470b4e5e34eadeb2777df5f6be/g' Tutorial.md
sed -i 's/@docker_new_account/7b19fe903a39002b4a2404277a42350ba3dc96df041e854bbf6cc644c82fb902#0/g' Tutorial.md
sed -i 's/@docker_total_gas_new_account/2563/g' Tutorial.md
sed -i 's/@docker_reduced_balance/999999998762870/g' Tutorial.md
sed -i 's/@docker_balance_validator0/642/g' Tutorial.md
sed -i 's/@docker_staked_validator0/1923/g' Tutorial.md
sed -i 's/@docker_diff1/1237130/g' Tutorial.md
sed -i 's/@docker_diff2/2563/g' Tutorial.md
sed -i 's/@docker_diff3/2/g' Tutorial.md
sed -i 's/@docker_sum1/2565/g' Tutorial.md

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
sed -i 's/@takamaka_version/1.3.0/g' ProgrammingHotmoka.md
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
sed -i 's/@takamaka_version/1.3.0/g' ProgrammingBlueknot.md
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
