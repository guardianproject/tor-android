#!/bin/bash

# helper script to apply guardian commits onto torproject c-tor

cd tor
pwd

echo "Guardian Project tor fork URL:"
git remote get-url origin
git fetch origin

echo "Tor Project tor repository URL:"
git remote add torproject https://gitlab.com/torproject/tor.git
git remote get-url torproject
git fetch torproject

echo "Enter Tor Project's git tag for a tor release..."
echo "ie tor 0.4.8.22 has the git tag tor-0.4.8.22..."
read -p "Tor Project Release git tag: " TOR_TAG

echo "Checking out $TOR_TAG"
git checkout $TOR_TAG
echo "Creating branch $TOR_TAG-dev for Guardian Project commits..."
git checkout -b $TOR_TAG"-dev"

echo "Cherry picking guardian project commits..."
git cherry-pick ee116cd7ea37bc2570b42324704a82d34676591d \
	&& git cherry-pick fc8695c467026af47ffcf19bb0ab1426f0232623 \
	&& git cherry-pick 4bb17348960ba329b3902371fae6d3b42a4cb5ab \
	&& git cherry-pick 0743669764fa0ffba3f8b5e126f376825e2a904b \
	&& git cherry-pick 5ae009ecd13c36317a8e2eacea66097222d77049 \
	&& git cherry-pick d51cfc9982ad221f0f2f66fc2a7add55c5c1be58 \
	&& git cherry-pick 36d1290b22352614b039ca92ef588030bbbb57e5 \
	&& git cherry-pick b7e02e08584f7dd8cc3f0c006a0a75065457921c \
	&& git cherry-pick 48db35c3017ba07df31f2b9da1263230f01cc86a \
	&& git cherry-pick 8950c43d7aabf4d981335f8859c8540fdaadb2ad \
	&& git cherry-pick --keep-redundant-commits b6f1d925388368e298439655b78608d0b47a78d7 \
	&& git cherry-pick --keep-redundant-commits 037c9d36ea72ce15052ce61a3b9e435cad42d6fa \
	&& git cherry-pick ecb70c77dffc5eea7014f22f8bc54eb6b8f47512 \
	&& git cherry-pick 5f6e8570a2465229e92e9d2b5a378acec8749556 \
	&& git cherry-pick a3aecdf3c82c0cc689d62f9cfeb57b6be364cde0 \
	&& git cherry-pick 104d40112d43a91eebec330b69e31476e1b71bac \
	&& git cherry-pick 436f0f0fbb69065c1bb16f742a88d7dd62b8c054

echo "Merging in commits from Guardian Project's master branch from:"
git remote get-url origin
git merge origin/master

echo "DONE: Guardian Project commits have been added to $TOR_TAG-dev"

cd ..
git status
echo "commit tor and then run ./tor-droid-make.sh fetch -c to try the GP build of $TOR_TAG"
