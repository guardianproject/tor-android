#!/bin/bash

TOR_VERSION="tor-0.4.9.12"
BRANCH_SUFFIX="dev"

TOR_PROJECT_REMOTE="torproject"
GUARDIAN_PROJECT_REMOTE="origin"

GUARDIAN_PROJECT_PATCH_COMMITS=(
        "ee116cd7ea37bc2570b42324704a82d34676591d"
        "fc8695c467026af47ffcf19bb0ab1426f0232623"
        "4bb17348960ba329b3902371fae6d3b42a4cb5ab"
        "0743669764fa0ffba3f8b5e126f376825e2a904b"
        "5ae009ecd13c36317a8e2eacea66097222d77049"
        "d51cfc9982ad221f0f2f66fc2a7add55c5c1be58"
        "36d1290b22352614b039ca92ef588030bbbb57e5"
        "b7e02e08584f7dd8cc3f0c006a0a75065457921c"
        "48db35c3017ba07df31f2b9da1263230f01cc86a"
        "8950c43d7aabf4d981335f8859c8540fdaadb2ad"
        "b6f1d925388368e298439655b78608d0b47a78d7"
        "037c9d36ea72ce15052ce61a3b9e435cad42d6fa"
        "ecb70c77dffc5eea7014f22f8bc54eb6b8f47512"
        "5f6e8570a2465229e92e9d2b5a378acec8749556"
        "a3aecdf3c82c0cc689d62f9cfeb57b6be364cde0"
        "104d40112d43a91eebec330b69e31476e1b71bac"
        "436f0f0fbb69065c1bb16f742a88d7dd62b8c054"
        "16b7ed7b24ae972c49da912b6824b7d7931cb17b"
        "3c1a53efb58aa58dcc360a011a68d5aaf1116f97"
        "f13c02febaf8a69e40e51631efe7fb4a677cd326"
        "d53bcaa8abe9cd5f6b962584a5a232f7bb3c309c"
        "8a2d1c7fbee6bf4dd3acc6f8cb873b9120914b53"
        "de2bcc02eeabaf6bc5420f9031f389ffcc2997be"
        "7e9070cf347b1a7c03be3346e36f11338ee82f28"
)

cherry_pick_gp_commit() {
        echo "💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾"
        echo "💾 Guardian Project Patch: $1 💾"

        git log -n1 "$1"
        echo ""
        git cherry-pick --keep-redundant-commits "$1"
        echo "💾                                                                  💾"
        echo "💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾💾"
        printf "\n\n\n"
}

pretty() {
    echo ""
    echo "🧅 $1 🧅"
    echo ""
}

pretty "entering tor directory..."
cd tor
pwd

pretty "fetching torproject and guardianproject remote repositories"
echo "tor project"
git remote get-url $TOR_PROJECT_REMOTE
git fetch $TOR_PROJECT_REMOTE
echo ""
echo "guardian project"
git remote get-url $GUARDIAN_PROJECT_REMOTE
git fetch $GUARDIAN_PROJECT_REMOTE

pretty "building tor-android for $TOR_VERSION on $TOR_VERSION-$BRANCH_SUFFIX"
git checkout $TOR_VERSION
git checkout -b "$TOR_VERSION-$BRANCH_SUFFIX"

pretty "applying guaridan project patches"
for hash in "${GUARDIAN_PROJECT_PATCH_COMMITS[@]}"; do
    cherry_pick_gp_commit $hash
done

pretty "Done! Created branch $TOR_VERSION-$BRANCH_SUFFIX"
