#!/usr/bin/env bash
set -eu -o pipefail

# case insensitive
SEARCH_REGEX='api(\s*|.)server'
SED_REPLACEMENTS=('s/([Aa][Pp][Ii].?)server/\1router/g' 's/([Aa][Pp][Ii].?)Server/\1Router/g')
SEARCH_DIR=~/work/protogen

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

echo "=== Folder renames ==="
for file in $(find . -type d | egrep -v '\.git' | egrep -i "${SEARCH_REGEX}"); do
  mv_file "${file}"
done

echo && echo "=== File renames ==="
for file in $(find . -type f | egrep -v '\.git' | egrep -i "${SEARCH_REGEX}"); do
  mv_file "${file}"
done

echo && echo "=== Apply text changes ==="
for file in $(ag -i -l "${SEARCH_REGEX}"); do
  echo "${file}"
  for regex in "${SED_REPLACEMENTS[@]}"; do
    sed -i '' -E -e "${regex}" "${file}"
  done
done

echo "=== Custom Changes ==="
cd "${SEARCH_DIR}/src/main/java/com/canva/protogen/servicepluginconfig"
for file in $(ag -i -l 'return.+"Server";'); do
  echo "${file}"
  sed -i '' -E -e 's/return(.+)"Server";/return\1"Router";/g' "${file}"
done