#!/bin/bash

# This script transforms the Tutorial.source documentation into the following files:
# - ../README.md: the README file of the repository, in Markdown
# - ProgrammingHotmoka.pdf: a PDF version of the documentation for Hotmoka
# - ProgrammingHotmoka.epub: an epub version of the documentation for Hotmoka
# - ProgrammingHotmoka.mobi: a mobi version of the documentation for Hotmoka

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
sed -i 's/@hotmoka_version/1.7.0/g' Tutorial.md
sed -i 's/@takamakaCode/ae8e94c789fb66784a6cc4208c797c80cbce2721292d636b963184fc266abe42/g' Tutorial.md
sed -i 's/@manifest/f12254cd1abcf6999881a73a45075196d47828baf630bec24efbeb50150a52be#0/g' Tutorial.md
sed -i 's/@gamete/25b310a0ce87dcd6a019e5a081f38ddc9a8812dff7144273f02d06d3824752c8#0/g' Tutorial.md
sed -i 's/@gasStation/f12254cd1abcf6999881a73a45075196d47828baf630bec24efbeb50150a52be#14/g' Tutorial.md
sed -i 's/@validators/f12254cd1abcf6999881a73a45075196d47828baf630bec24efbeb50150a52be#1/g' Tutorial.md
sed -i 's/@maxFaucet/10000000000000/g' Tutorial.md
sed -i 's/@chainid/marabunta/g' Tutorial.md
sed -i 's/@chainid/marabunta/g' state1_copy.fig
sed -i 's/@chainid/marabunta/g' state2_copy.fig
sed -i 's/@chainid/marabunta/g' state3_copy.fig
sed -i 's/@account1/5f705b7dc5869ae39db3bc80b7cd073c2bb55726706749138d16a4a9d0f01766#0/g' Tutorial.md
sed -i 's/@short_account1/5f705b7dc58...#0/g' state2_copy.fig
sed -i 's/@short_account1/5f705b7dc58...#0/g' state3_copy.fig
sed -i 's/@36words_of_account1/ 1: mail\n 2: better\n 3: supreme\n 4: quit\n 5: expire\n 6: forest\n 7: traffic\n 8: bus\n 9: attract\n10: gas\n11: glow\n12: team\n13: warfare\n14: color\n15: warfare\n16: radio\n17: online\n18: brother\n19: hollow\n20: jungle\n21: arena\n22: track\n23: brother\n24: lyrics\n25: relief\n26: rice\n27: orchard\n28: soldier\n29: muffin\n30: shoot\n31: fold\n32: news\n33: special\n34: level\n35: sunset\n36: hover/g' Tutorial.md
sed -i "s/@publickeyaccount1/TFqggZYjD4hvykf4rfc84J3cWgfnmFJRWd\/8P4jH2Vs=/g" Tutorial.md
sed -i "s/@short_publickeyaccount1/TFqggZYjD4.../g" state2_copy.fig
sed -i "s/@short_publickeyaccount1/TFqggZYjD4.../g" state3_copy.fig
sed -i 's/@account_anonymous/309ed8d1058c7c7df0ebf85fb37d07e386467660dca2e568436eb9972c8e27b2#0/g' Tutorial.md
sed -i 's/@new_key/9ZWQamdq5g5b4WmGaDWDzyYTt7CYCQki9LYiVekvoe2n/g' Tutorial.md
sed -i 's/@family_address/2dfdad98c8280fa5834be3fe94db06233146275641540aa179de81c1a9afaec7/g' Tutorial.md
sed -i 's/@short_family_address/2dfdad98c8.../g' state3_copy.fig
sed -i 's/@code_family_address/7b25f645b5213e0a855ce158cedc341a688c01aa77898486ff9f36ce772809a3/g' Tutorial.md
sed -i 's/@family2_address/c4e98a66ab3b4ae598d1ffaab5aa97c93a8fb8d78282abd359658794179788f6/g' Tutorial.md
sed -i 's/@family_exported_address/3d3a8e783941ac4c8ca03fc10e7967a37b9885c9ec4e006ea48eee21fa0a02c9/g' Tutorial.md
sed -i 's/@family3_address/a9d69ce22453c912a436e990074fd3493c87667ada85b6881f5566a7efae08e7/g' Tutorial.md
sed -i 's/@person_object/24a92fcb1536caf360906a4a8f7744c1d54b7ccd1396cd1db85896dcfe39f73e#0/g' Tutorial.md
sed -i 's/@person2_object/4e168218952bbfa96a85ca69c1a738d234d95174557f257601a2a3714a3a418f#0/g' Tutorial.md
sed -i 's/@person3_object/c9b88a105236b7400805731390bab5863f44b1d0b9f7d9ae7c057e3f0e832509#0/g' Tutorial.md
sed -i 's/@gradual_ponzi_address/b8b37e6a848dd59ecd091194f234ff6409705a78bc0df774d5716d5d9c611b2e/g' Tutorial.md
sed -i 's/@gradual_ponzi_object/432d5d75ebb8bcc4f52fa2c6c5b988601b79bb6e1db72e7b591c902a638237d8#0/g' Tutorial.md
sed -i 's/@gradual_ponzi_list/432d5d75ebb8bcc4f52fa2c6c5b988601b79bb6e1db72e7b591c902a638237d8#1/g' Tutorial.md
sed -i 's/@gradual_ponzi_first/432d5d75ebb8bcc4f52fa2c6c5b988601b79bb6e1db72e7b591c902a638237d8#2/g' Tutorial.md
sed -i 's/@gradual_ponzi_last/5bdff31c6151ec5b381e8fc43e76efd382ab20d90b004597cc133b9a1619ea49#0/g' Tutorial.md
sed -i 's/@account2/12441d4a2f52e80f93e726040fbc364b75e7fedbef96887110df678794d791ea#0/g' Tutorial.md
sed -i 's/@account3/eec01b6f22911f76dbd25bda6f850e9af9e8640a4530a46c1909f48b9c7976a3#0/g' Tutorial.md
sed -i 's/@account4/e42def4faba5d93346e4d02a1608c9fb53ba7beea84ffb0064cdfe431d079867#0/g' Tutorial.md
sed -i 's/@account5/fdb930378af70c212649e4f9b888d7c275373cf5b9601b40e4802d29fa27af89#0/g' Tutorial.md
sed -i 's/@account6/b65e5976862d6786ba9a6d2cd62b332bfa7f8859e2c8b9be3bbbe5cd64af6c1e#0/g' Tutorial.md
sed -i 's/@account7/b37b9c040474fc800079230bc51409201f12f82e220b99e7b6468c5f62591ea8#0/g' Tutorial.md
sed -i 's/@publickeyaccount4/N7hQ0gpmpV46JJlQ+7zEy7r2OTn\/jErqxV9g3+tg2\/o=/g' Tutorial.md
sed -i 's/@short_publickeyaccount5/MIIDRjCCAjkGByqGSM44BAEwggIsAo.../g' Tutorial.md
sed -i 's/@tictactoe_address/85a234ecc737d83f33659bc94e916e8248274f19dfc6b5f0a29dbce7f1f947ed/g' Tutorial.md
sed -i 's/@tictactoe_object/a2bf8656d4979b0f18197f28c2aab4e7dbf58a01cf9d3ed8acf3af496e4bf99b#0/g' Tutorial.md
sed -i 's/@erc20_address/3b8157f04217918a76b48fc0f4388b4bece80ad0e542c42722a887d648cd314d/g' Tutorial.md
sed -i 's/@erc20_object/2daa4ce2dbd152bcb4917fdd1574173373c77b306bcd754309da38a138ebed17#0/g' Tutorial.md
sed -i 's/@server/ws:\/\/panarea.hotmoka.io/g' Tutorial.md
sed -i 's/@new_docker_key/AsqsX1tBMriz18xvoxvc64K4HAvxgFmwWryKcMeR64oc/g' Tutorial.md
sed -i 's/@container_id1/00a196685e5c8a8d788b892cb09a994e48016d736bfeaf89c1f339d72f7a717b/g' Tutorial.md
sed -i 's/@docker_takamaka_code/ae8e94c789fb66784a6cc4208c797c80cbce2721292d636b963184fc266abe42/g' Tutorial.md
sed -i 's/@docker_manifest/9f6d963231e8f22530496c1b38ce38b60df2c90c7d2d9176706dbe3ac8f934d1#0/g' Tutorial.md
sed -i 's/@docker_gamete/3a6eaf6df650bad0120f72c0fb962a976337ef4360407135e2e122699e40b8e7#0/g' Tutorial.md
sed -i 's/@docker_validators/9f6d963231e8f22530496c1b38ce38b60df2c90c7d2d9176706dbe3ac8f934d1#1/g' Tutorial.md
sed -i 's/@docker_validator0/7779e885c98d631e489738dfd621763959a84bc57ae9e002f9947d15a518b27c#0/g' Tutorial.md
sed -i 's/@docker_id_validator0/84694297DB4B75A1B0C810F051035A0D7A8A3D99/g' Tutorial.md
sed -i 's/@container_id2/3f59c2cdb0a57c5f3df2da7a018ccfa16f7fa48f73e65970b4bf8da60e3972c7/g' Tutorial.md
sed -i 's/@container_id3/b83bd0a5e49ed82278742f49b1490abd8a84c103fc684cb4b0222a292a6863f5/g' Tutorial.md
sed -i 's/@docker_new_account/44ca934098eb91fcc5efe1a6e67e29cf39179e94ce78cf08a71850c1add2e38f#0/g' Tutorial.md
sed -i 's/@docker_total_gas_new_account/2565/g' Tutorial.md
sed -i 's/@docker_reduced_balance/999999998762868/g' Tutorial.md
sed -i 's/@docker_balance_validator0/642/g' Tutorial.md
sed -i 's/@docker_staked_validator0/1925/g' Tutorial.md
sed -i 's/@docker_diff1/1237132/g' Tutorial.md
sed -i 's/@docker_diff2/2565/g' Tutorial.md
sed -i 's/@docker_diff3/2/g' Tutorial.md
sed -i 's/@docker_sum1/2567/g' Tutorial.md

# we regenerate the png figures, since they might contain some string changed by the previous sed commands
fig2dev -L png -m 4 state1_copy.fig pics/state1.png
fig2dev -L png -m 4 state2_copy.fig pics/state2.png
fig2dev -L png -m 4 state3_copy.fig pics/state3.png
rm *_copy.fig

# Hotmoka-specific processing now...
cp Tutorial.md ProgrammingHotmoka.md

# These must be edited by hand since, for instance, they depend on accounts created in Mokito
sed -i 's/@tendermint_version/0.34.15/g' ProgrammingHotmoka.md
sed -i 's/@takamaka_version/1.4.1/g' ProgrammingHotmoka.md
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

# place \input{cover_page.tex} after \begin{document}
sed -i 's/\\begin{document}/\\begin{document}\\input{cover_page.tex}/g' ProgrammingHotmoka.tex

#pdflatex ProgrammingHotmoka.tex

mv ProgrammingHotmoka.md ../README.md

# generate the epub version of the document
# we remove the first lines of the Markdown, that contain Java build information
tail -n +6 ../README.md > ProgrammingHotmoka.md
pandoc -o ProgrammingHotmoka.epub metadata-hotmoka.yaml ProgrammingHotmoka.md
rm ProgrammingHotmoka.md

# generate the mobi version of the document
ebook-convert ProgrammingHotmoka.epub ProgrammingHotmoka.mobi

rm Tutorial.md