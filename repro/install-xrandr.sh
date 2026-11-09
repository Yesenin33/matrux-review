#!/bin/sh
# Ставит xrandr в каждый rootfs, чтобы гость мог менять размер кадра на лету.
set -u
for R in rootfs debian13-rootfs ubuntu-rootfs arch-rootfs dev-rootfs; do
  D=/root/palma/image-build/$R
  [ -d "$D" ] || { echo "$R: нет каталога"; continue; }
  if [ -x "$D/usr/bin/xrandr" ]; then echo "$R: xrandr уже есть"; continue; fi
  PM=""
  [ -x "$D/sbin/apk" ] || [ -x "$D/usr/sbin/apk" ] && PM=apk
  [ -z "$PM" ] && [ -x "$D/usr/bin/apt-get" ] && PM=apt
  [ -z "$PM" ] && [ -x "$D/usr/bin/pacman" ] && PM=pacman
  echo "== $R: менеджер пакетов = ${PM:-не найден}"
  [ -n "$PM" ] || continue
  cp -f /usr/bin/qemu-aarch64 "$D/usr/bin/qemu-aarch64-static"
  if [ "$PM" = pacman ]; then
    rm -f "$D/etc/resolv.conf"; cp -f /etc/resolv.conf "$D/etc/resolv.conf"
  elif [ ! -s "$D/etc/resolv.conf" ]; then
    cp -f /etc/resolv.conf "$D/etc/resolv.conf"
  fi
  for m in dev proc sys; do mountpoint -q "$D/$m" || mount --bind "/$m" "$D/$m"; done
  case "$PM" in
    apk)    chroot "$D" /usr/bin/qemu-aarch64-static /bin/sh -c 'apk add --no-cache xrandr 2>&1 | tail -3' ;;
    apt)    chroot "$D" /usr/bin/qemu-aarch64-static /bin/sh -c 'export DEBIAN_FRONTEND=noninteractive; apt-get update -qq 2>&1 | tail -2; apt-get install -y -qq --no-install-recommends x11-xserver-utils 2>&1 | tail -3' ;;
    pacman) chroot "$D" /usr/bin/qemu-aarch64-static /bin/sh -c 'pacman -S --noconfirm xorg-xrandr 2>&1 | tail -4' ;;
  esac
  for m in dev proc sys; do mountpoint -q "$D/$m" && umount "$D/$m" || true; done
  rm -f "$D/usr/bin/qemu-aarch64-static"
  ls -la "$D/usr/bin/xrandr" 2>/dev/null || echo "$R: xrandr НЕ появился"
done
echo "ГОТОВО-XRANDR"
