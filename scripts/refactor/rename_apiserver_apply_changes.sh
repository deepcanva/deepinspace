#!/usr/bin/env bash
set -eu -o pipefail

# case insensitive
SEARCH_REGEX='api(\s*|[^/])server'
SED_REPLACEMENTS=('s/([Aa][Pp][Ii][^/]?)server/\1router/g' 's/([Aa][Pp][Ii][^/]?)Server/\1Router/g')
SEARCH_DIR=~/work/canva
SOURCE_FILES=~/temp/renamed.txt

cd "${SEARCH_DIR}"
for file in $(cat ${SOURCE_FILES}); do
  if [[ -f "${file}" ]]; then
    echo "${file}"
    for regex in "${SED_REPLACEMENTS[@]}"; do
      sed -i '' -E -e "${regex}" "${file}"
    done

    rename=$((echo "${file}" | egrep -i "${SEARCH_REGEX}") || echo "")
    if [[ -n "${rename}" ]]; then
        echo "${file} should be renamed!"
    fi;
  else
    echo "**** ${file} not found"
  fi
done
