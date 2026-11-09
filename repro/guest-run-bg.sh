#!/bin/sh
# фон-запуск сессии гостя (диагностика)
sh /data/data/ru.palma.app/guest-run.sh "$@" > /data/data/ru.palma.app/bg.log 2>&1 &
echo "фоновый pid=$!"
