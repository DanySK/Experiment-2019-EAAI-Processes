#!/bin/bash
for file in data/*
  do LASTLINE="`tac "${file}" | egrep -m 1 .`"
  if [ "${LASTLINE:0:1}" == "#" ]
    then git add -f "${file}"
  fi
done
git commit -m "[AUTOMATIC ${USER}@${HOSTNAME}] - Data generated up to `date --iso-8601=seconds`"
git pull --no-edit
git push
