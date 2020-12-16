#!/usr/bin/env bash
set -eu -o pipefail

# case insensitive
SEARCH_REGEX='(?<!new )finagle.*protoserver'
FILE_REGEX='(?<!ProtoServer).java$'
SED_REPLACEMENTS=('s/Finagle[^ ]*ProtoServer.class/FinagleServer.class/g' 's/final Finagle[^ ]*ProtoServer/final FinagleServer/g')
SEARCH_DIR=~/work/canva

cd "${SEARCH_DIR}"

echo && echo "=== Apply text changes ==="
for file in $(ag -i -l "${SEARCH_REGEX}" -G "${FILE_REGEX}"); do
  echo "${file}"
  for regex in "${SED_REPLACEMENTS[@]}"; do
    sed -i '' -E -e "${regex}" "${file}"
  done
done