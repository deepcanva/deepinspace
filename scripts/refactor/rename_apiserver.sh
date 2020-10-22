#!/usr/bin/env bash
set -eu -o pipefail

# case insensitive
SEARCH_REGEX='api(\s*|.)server'
SED_REPLACEMENTS=('s/([Aa])([Pp])([Ii])(.?)server/\1\2\3\4router/g' 's/([Aa])([Pp])([Ii])(.?)Server/\1\2\3\4Router/g')

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

cd ~/work/protogen

echo "=== Folder renames ==="
for file in $(find . -type d | egrep -i "${SEARCH_REGEX}"); do
  mv_file "${file}"
done

echo && echo "=== File renames ==="
for file in $(find . -type f | egrep -i "${SEARCH_REGEX}"); do
  mv_file "${file}"
done

echo && echo "=== Apply text changes ==="
for file in $(ag -i -l "${SEARCH_REGEX}"); do
  echo "${file}"
  for regex in "${SED_REPLACEMENTS[@]}"; do
    sed -i '' -E -e "${regex}" "${file}"
  done
done