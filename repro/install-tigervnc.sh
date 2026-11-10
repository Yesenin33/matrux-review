#!/bin/bash
set -u
B=/root/palma/image-build
Q=/usr/bin/qemu-aarch64

prepare() {
  D="$B/$1"
  cp "$Q" "$D/usr/bin/qemu-aarch64-static"
  cp /etc/resolv.conf "$D/etc/resolv.conf"
  mount -t proc proc "$D/proc" 2>/dev/null
  mount --bind /dev "$D/dev" 2>/dev/null
  mount --bind /sys "$D/sys" 2>/dev/null
  echo "$D"
}

teardown() {
  D="$B/$1"
  umount "$D/sys" 2>/dev/null
  umount "$D/dev" 2>/dev/null
  umount "$D/proc" 2>/dev/null
  rm -f "$D/usr/bin/qemu-aarch64-static"
}

apt_install() {
  R="$1"; D=$(prepare "$R")
  echo "===== $R : apt ====="
  DEBIAN_FRONTEND=noninteractive chroot "$D" /usr/bin/qemu-aarch64-static /bin/bash -c \
    'apt-get update -qq 2>&1 | tail -2; DEBIAN_FRONTEND=noninteractive apt-get install -y -qq tigervnc-standalone-server 2>&1 | tail -6' 
  echo "rc=$?"
  teardown "$R"
  ls -la "$D/usr/bin/Xvnc" 2>/dev/null || echo "$R: Xvnc НЕТ"
}

arch_install() {
  R="$1"; D=$(prepare "$R")
  echo "===== $R : pacman ====="
  sed -i 's/^#DisableSandbox/DisableSandbox/; s/^DownloadUser.*/DownloadUser = root/; s/^CheckSpace/#CheckSpace/' "$D/etc/pacman.conf" 2>/dev/null
  grep -q '^DisableSandbox' "$D/etc/pacman.conf" || sed -i '/^\[options\]/a DisableSandbox' "$D/etc/pacman.conf"
  chroot "$D" /usr/bin/qemu-aarch64-static /bin/bash -c \
    'pacman -Sy --noconfirm tigervnc 2>&1 | tail -8'
  echo "rc=$?"
  teardown "$R"
  ls -la "$D/usr/bin/Xvnc" 2>/dev/null || echo "$R: Xvnc НЕТ"
}

if [ "$#" -gt 0 ]; then
  for r in "$@"; do
    case "$r" in
      arch*) arch_install "$r" ;;
      *)     apt_install "$r" ;;
    esac
  done
else
  apt_install debian-rootfs
  apt_install ubuntu-rootfs
  arch_install arch-rootfs
fi
echo "===== ИТОГ ====="
for r in "${@:-rootfs debian-rootfs ubuntu-rootfs arch-rootfs}"; do
  printf '%-16s Xvnc=%s\n' "$r" "$([ -L $B/$r/usr/bin/Xvnc ] && echo симлинк || { [ -f $B/$r/usr/bin/Xvnc ] && echo бинарник || echo нет; })"
done
