#!/usr/bin/env bash
set -eu -o pipefail

# case insensitive
SEARCH_REGEX='api(\s*|[^/])server'
SED_REPLACEMENTS=('s/([Aa][Pp][Ii][^/]?)server/\1router/g' 's/([Aa][Pp][Ii][^/]?)Server/\1Router/g')
SEARCH_DIR=~/work/canva

mv_file() {
  file="$1"
  out_file="$file"
  for regex in "${SED_REPLACEMENTS[@]}"; do
    out_file=$(echo "${out_file}" | sed  -E -e "${regex}")
  done

  if [[ "${file}" == "${out_file}" || "${#file}" -ne "${#out_file}" ]]; then
    echo "File rename for '${file}' => '${out_file}' does not look right"
    exit 1
  fi;

  echo "Renaming ${file} to ${out_file}"
  git mv "${file}" "${out_file}"
}

cd "${SEARCH_DIR}"
for file in $(ag -i -l "${SEARCH_REGEX}" -G '[.](java|ts)$'); do
  echo "${file}"
  for regex in "${SED_REPLACEMENTS[@]}"; do
    sed -i '' -E -e "${regex}" "${file}"
  done

  rename=$((echo "${file}" | egrep -i "${SEARCH_REGEX}") || echo "")
  if [[ -n "${rename}" ]]; then
      mv_file "${file}"
  fi;
done
