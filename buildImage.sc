#!/usr/bin/env amm

import ammonite.ops._
import ammonite.ops.ImplicitWd._

def wifiConfig(ssid: String, psk: String) = s"""
#cloud-config

# Set your hostname here, the manage_etc_hosts will update the hosts file entries as well
hostname: black-pearl
manage_etc_hosts: true

# You could modify this for your own user information
users:
  - name: pirate
    gecos: "Hypriot Pirate"
    sudo: ALL=(ALL) NOPASSWD:ALL
    shell: /bin/bash
    groups: users,docker,video
    plain_text_passwd: hypriot
    lock_passwd: false
    ssh_pwauth: true
    chpasswd: { expire: false }

package_upgrade: false

# # WiFi connect to HotSpot
# # - use `wpa_passphrase SSID PASSWORD` to encrypt the psk
write_files:
  - content: |
      allow-hotplug wlan0
      iface wlan0 inet dhcp
      wpa-conf /etc/wpa_supplicant/wpa_supplicant.conf
      iface default inet dhcp
    path: /etc/network/interfaces.d/wlan0
  - content: |
      ctrl_interface=DIR=/var/run/wpa_supplicant GROUP=netdev
      update_config=1
      network={
      ssid="$ssid"
      psk="$psk"
      proto=RSN
      key_mgmt=WPA-PSK
      pairwise=CCMP
      auth_alg=OPEN
      }
    path: /etc/wpa_supplicant/wpa_supplicant.conf

# These commands will be ran once on first boot only
runcmd:
  # Pickup the hostname changes
  - 'systemctl restart avahi-daemon'

  # Activate WiFi interface
  - 'ifup wlan0'
"""

@main
def main(ssid: String, psk: String, device: String) = {
  val flashexec = root/'usr/'local/'bin/'flash
  if (!exists(flashexec)) {
    val resp = requests.get("https://raw.githubusercontent.com/hypriot/flash/2.3.0/flash")
    write(flashexec, resp.contents)
    %('chmod, "+x", flashexec.toString)
  }
  write(pwd/"wifi.yaml", wifiConfig(ssid, psk))
  %('flash, "--userdata", "wifi.yaml", "--device", device, "https://github.com/hypriot/image-builder-rpi/releases/download/v1.11.0/hypriotos-rpi-v1.11.0.img.zip")
}