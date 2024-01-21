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
sed -i 's/@hotmoka_version/1.4.0/g' Tutorial.md
sed -i 's/@takamakaCode/da14e01c2331e54b0c51d1daeeae0d55a572250fbf620a6b1b35392ebf2d50d0/g' Tutorial.md
sed -i 's/@manifest/b2ca50a795e571becc7f92b3366d2628c2c805d543431662bd9d65d6fb9402b0#0/g' Tutorial.md
sed -i 's/@gamete/2e3fdb2a4b83ffef3b11a3822209ab566f53b8c2c219b8fdfd6f6d3b301399b1#0/g' Tutorial.md
sed -i 's/@gasStation/b2ca50a795e571becc7f92b3366d2628c2c805d543431662bd9d65d6fb9402b0#14/g' Tutorial.md
sed -i 's/@validators/b2ca50a795e571becc7f92b3366d2628c2c805d543431662bd9d65d6fb9402b0#1/g' Tutorial.md
sed -i 's/@maxFaucet/10000000000000/g' Tutorial.md
sed -i 's/@chainid/marabunta/g' Tutorial.md
sed -i 's/@chainid/marabunta/g' state1_copy.fig
sed -i 's/@chainid/marabunta/g' state2_copy.fig
sed -i 's/@chainid/marabunta/g' state3_copy.fig
sed -i 's/@account1/72f31eb48f3d0bf9be4e37f184024c16c016e87a20b94ce8744f298094a1cae9#0/g' Tutorial.md
sed -i 's/@short_account1/72f31eb48f3...#0/g' state2_copy.fig
sed -i 's/@short_account1/72f31eb48f3...#0/g' state3_copy.fig
sed -i 's/@36words_of_account1/ 1: away\n 2: market\n 3: nurse\n 4: maple\n 5: peanut\n 6: involve\n 7: universe\n 8: captain\n 9: sword\n10: spice\n11: harsh\n12: neutral\n13: connect\n14: monster\n15: split\n16: tray\n17: armor\n18: social\n19: near\n20: husband\n21: blue\n22: abuse\n23: core\n24: submit\n25: black\n26: duck\n27: market\n28: income\n29: soldier\n30: demand\n31: device\n32: copy\n33: net\n34: atom\n35: ripple\n36: occur/g' Tutorial.md
sed -i "s/@publickeyaccount1/w0XArLvUCeIB2iXRfz8OJjf3jptdAAtmCe+\/LxV5JgU=/g" Tutorial.md
sed -i "s/@short_publickeyaccount1/w0XArLvUCe.../g" state2_copy.fig
sed -i "s/@short_publickeyaccount1/w0XArLvUCe.../g" state3_copy.fig
sed -i 's/@account_anonymous/bac7dee12c151f9d22be2df83f555d6cc447ca9615f3c9c992219666838a74fe#0/g' Tutorial.md
sed -i 's/@new_key/H1ienW1CxnJ9dD8PBSp3H8KAFnHAdbGpomttHhpcDB4A/g' Tutorial.md
sed -i 's/@family_address/43049a4a3a70318c1a3a0c008cdcde5bbf0eead71fe8ebfda331bd6b9c686421/g' Tutorial.md
sed -i 's/@short_family_address/43049a4a3a.../g' state3_copy.fig
sed -i 's/@code_family_address/5baa57997c7872e3d7472fe5ed19d7f418596b4c2171d59c6c875926a64e67bc/g' Tutorial.md
sed -i 's/@family2_address/09f366d10534c051c95382919692300baae61a649a3e14bbb381988782ba77a5/g' Tutorial.md
sed -i 's/@family_exported_address/7597fc8a1b67ee850de84fa0e2f324631793268bb25944b4babe96f1be54f3eb/g' Tutorial.md
sed -i 's/@family3_address/d3667995b7a7d3dc252ed5d8d2488496df0f2e16217445ce830eaaecd615b973/g' Tutorial.md
sed -i 's/@person_object/526ac9a1b8693bad02b76d985672502b922d80ad100b202baa4e5497d019cc9a#0/g' Tutorial.md
sed -i 's/@person2_object/f46d7c2986e4df0e4d6668ca45473cd9fad46d14c76f29832cfde05d19591981#0/g' Tutorial.md
sed -i 's/@person3_object/ab7ade5892bdd4501f0ddb82d5a67b42799017ff26d2f78fdeff83050e84610f#0/g' Tutorial.md
sed -i 's/@gradual_ponzi_address/6893b9dc9e047f464d734ddf6f941a913a77fc9f5e835882ddb7aa78f952cdba/g' Tutorial.md
sed -i 's/@gradual_ponzi_object/b5c40b793be1694fc479c6b9f4c8634f50cfdabd196f67e1a0c8557c826a6c9c#0/g' Tutorial.md
sed -i 's/@gradual_ponzi_list/b5c40b793be1694fc479c6b9f4c8634f50cfdabd196f67e1a0c8557c826a6c9c#1/g' Tutorial.md
sed -i 's/@gradual_ponzi_first/b5c40b793be1694fc479c6b9f4c8634f50cfdabd196f67e1a0c8557c826a6c9c#2/g' Tutorial.md
sed -i 's/@gradual_ponzi_last/f181265805e32ccbc22270ef27725d7470e653b225e3faca58732f7068d516a6#0/g' Tutorial.md
sed -i 's/@account2/e867762273c7517af2bb6dc89eaca1baf9e945e99a0c24cfca08c51852f3a39a#0/g' Tutorial.md
sed -i 's/@account3/b49cbb39829929ebf4d140ba9e3481b0e9e3c27d543b9fe21e85880d6d554416#0/g' Tutorial.md
sed -i 's/@account4/189f66b050e0fc004ce3f6cc5a20ae55efe06188276082cd913392e2cf4a9429#0/g' Tutorial.md
sed -i 's/@account5/cab89e8b9b3cd7d1e54e9a4c077503bb827e9655504fc256451cefa002d0d3be#0/g' Tutorial.md
sed -i 's/@account6/1b8ec78656f3c8b9bbd01e12d192dc57c1eb0f88f7062f2b8870c076d90b239b#0/g' Tutorial.md
sed -i 's/@account7/fb7a4905f851583739dc11c395d385c46f76c943cfcbb4d4eb645ef3cd297a7d#0/g' Tutorial.md
sed -i 's/@publickeyaccount4/CNRpxIjzUwRAQDrQf1IC8mFzjtvGVnw8rwHH430RV7A=/g' Tutorial.md
sed -i 's/@short_publickeyaccount5/MIIDQjCCAjUGByqGSM44BAEwggIoAo.../g' Tutorial.md
sed -i 's/@tictactoe_address/98c583fd6be52c6cacc119fa7eba1308bc85ab124fabab424f79d052b31b51ad/g' Tutorial.md
sed -i 's/@tictactoe_object/6c5a820de8fd4899457a30516944640b4922c84341cca1124c0fc00d34032048#0/g' Tutorial.md
sed -i 's/@erc20_address/9ca8ee89dad59e060706853ab30b6045cb8d7e504e732e17e7a0733a902fdaa7/g' Tutorial.md
sed -i 's/@erc20_object/260164c272ceb72a5f0233469f0e66e467230c9ce13405ab7009c6f4bcc82289#0/g' Tutorial.md
sed -i 's/@server/panarea.hotmoka.io/g' Tutorial.md
sed -i 's/@new_docker_key/CoKNucXpHVkctg3hj5rERPGaskftmbnHmMmfncY7MF3X/g' Tutorial.md
sed -i 's/@container_id1/7e92c8a4948f8eb3aaf2fc4415801502cd7d3f4ea9ca1f482a6aabb4d82db02a/g' Tutorial.md
sed -i 's/@docker_takamaka_code/da14e01c2331e54b0c51d1daeeae0d55a572250fbf620a6b1b35392ebf2d50d0/g' Tutorial.md
sed -i 's/@docker_manifest/e7fc6067b91e154e10ce410d23e6ef9d04ca25a0c20efef203d70ceccd7f09eb#0/g' Tutorial.md
sed -i 's/@docker_gamete/320972c56a080ba42bba7fc2cbebc2e14a93e2dab5f582b06a3a86cef0ce5ea8#0/g' Tutorial.md
sed -i 's/@docker_validators/e7fc6067b91e154e10ce410d23e6ef9d04ca25a0c20efef203d70ceccd7f09eb#1/g' Tutorial.md
sed -i 's/@docker_validator0/e2289271f9ee061c41ac0638dae1dc92c3126453f17365e4cffb4f8a8d60ceff#0/g' Tutorial.md
sed -i 's/@docker_id_validator0/319FA93F3EBB6F009E0F1930FC8D3FF025D727B1/g' Tutorial.md
sed -i 's/@container_id2/e30950e9a07117275c4dd6c7abf6afa002dbada9758f993f6820247980fd50b6/g' Tutorial.md
sed -i 's/@container_id3/ff634b44444c9ad32e0502ecd06969206ce5919bf4d35f4204254f563283f56c/g' Tutorial.md
sed -i 's/@docker_new_account/ff99202e9e488d33047fb34c69e5c668798957860e9fe7beae9508dad6a00493#0/g' Tutorial.md
sed -i 's/@docker_total_gas_new_account/2577/g' Tutorial.md
sed -i 's/@docker_reduced_balance/999999998762856/g' Tutorial.md
sed -i 's/@docker_balance_validator0/645/g' Tutorial.md
sed -i 's/@docker_staked_validator0/1934/g' Tutorial.md
sed -i 's/@docker_diff1/1237144/g' Tutorial.md
sed -i 's/@docker_diff2/2577/g' Tutorial.md
sed -i 's/@docker_diff3/2/g' Tutorial.md
sed -i 's/@docker_sum1/2579/g' Tutorial.md

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
sed -i 's/@takamaka_version/1.0.14/g' ProgrammingHotmoka.md
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
sed -i 's/@takamaka_version/1.0.14/g' ProgrammingBlueknot.md
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
