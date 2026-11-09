#!/bin/sh
echo "== apk update"
apk update 2>&1 | tail -2
echo "== apk add tigervnc"
apk add --no-cache tigervnc 2>&1 | tail -6
echo "== результат:"
command -v Xvnc && Xvnc -version 2>&1 | head -2 || echo НЕТ_XVNC