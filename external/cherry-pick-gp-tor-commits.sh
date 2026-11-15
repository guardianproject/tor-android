#!/bin/bash

BRANCH_SUFFIX="dev"
TOR_VERSION=""
if [[ -z "$1" ]]; then
  echo "Please provide a tor version tag to use..."
  echo "for example: ./cherry-pick-gp-tor-commits.sh tor-0.4.9.15"
  exit
else
  TOR_VERSION="$1"
  read -r -p "Type \"yes\" to patch tor-android code onto tor tag $TOR_VERSION)? " answer
  if [[ "${answer,,}" != "yes" ]]; then
    exit
  fi
fi

TOR_PROJECT_REMOTE="torproject"
GUARDIAN_PROJECT_REMOTE="guardianproject"

GUARDIAN_PROJECT_PATCH_COMMITS=(
	"745a6a4ed09cb469c5d779ec3e990612224e7636"
	"00fde384bd2b976d641dd2f6f0e56896663a8851"
	"4efcf5909edd5f2dfdda7350667389e2a8da1de9"
	"a48a60e9a2d3c9978ff5e620a22eb954626b4a70"
	"f7253344a3cab96ebd850453942f98009aa4a048"
	"5f461d1c446ba51762a5e6af989f388387c0d488"
	"a16cd21300d100ae360c81546353d1147c7b82cf"
	"598b9e82466fe3286890cc68a39e15bc09ccf4d1"
	"50a45b5b838a562ba65a491c1a1141fb0d150c21"
	"03be88df980e127ce9581bab29d68143422f72eb"
	"fb1d37feb93bcea26a968e3019f0310ee7fbbff6"
	"1093bb81a91b710b441ce6b9c18e8cb91259bfe5"
	"f7a1049dc70771c7f69a1659dfdc7ee1fad91646"
	"f5bdf05f830edeb2d764af89b4a75c6e706d28dc"
	"49fde38d1762c1cc47e0551ed6c610144f2e4db7"
	"afb6ba465667a7a67a5e9812da3ad7a1bb54c3ea"
	"4a522a3ca491c6c04d4a44cb006b92101c42efc1"
	"a92edffdf0b705e0e511af899fa3d30bc12bbdeb"
	"52707da288319118c0552d93a934ecf6ea4d5737"
	"8d01e3a4e96a19266bf6990bb1b297cfe147f20f"
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
