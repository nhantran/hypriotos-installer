#!/usr/bin/env amm

import ammonite.ops._
import ammonite.ops.ImplicitWd._

def customConfig(ssid: String, psk: String, hostname: String, username: String, sshPubKey: String) = s"""
#cloud-config

# Set your hostname here, the manage_etc_hosts will update the hosts file entries as well
hostname: $hostname
manage_etc_hosts: true

# You could modify this for your own user information
users:
  - name: $username
    gecos: "Hypriot Pirate"
    primary-group: users
    shell: /bin/bash
    sudo: ALL=(ALL) NOPASSWD:ALL
    groups: users,docker,video 
    ssh_import_id: None
    lock_passwd: true
    ssh_authorized_keys:
      - $sshPubKey  

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
def flashHypriotOS(ssid: String, psk: String, device: String, hostname: String, username: String, sshPubKey: String) = {
  val HypriotOSVersion = "v1.10.0"
  val flashexec = root/'usr/'local/'bin/'flash
  if (!exists(flashexec)) {
    val resp = requests.get("https://raw.githubusercontent.com/hypriot/flash/2.3.0/flash")
    write(flashexec, resp.contents)
    %('chmod, "+x", flashexec.toString)
  }
  write.over(pwd/"wifi.yaml", customConfig(ssid, psk, hostname, username, sshPubKey))
  %('flash, "--userdata", "wifi.yaml", "--device", device, s"https://github.com/hypriot/image-builder-rpi/releases/download/$HypriotOSVersion/hypriotos-rpi-$HypriotOSVersion.img.zip")
}

@main
def installKubernetes(hostname: String, username: String) = {
  %('ssh, "-i", s"${home.toString}/.ssh/id_rsa", s"$username@$hostname", "sudo apt-get update && sudo apt-get install -y apt-transport-https curl")
  %('ssh, s"$username@$hostname", "curl -s https://packages.cloud.google.com/apt/doc/apt-key.gpg | sudo apt-key add -")
  val kubeListExisted = %%('ssh, s"$username@$hostname", "[ -f /etc/apt/sources.list.d/kubernetes.list ] && echo \"Found\" || echo \"NotFound\"")
  if (kubeListExisted.out.string.trim == "NotFound") {
    %('scp, "kubernetes.list", s"$username@$hostname:")
    %('ssh, s"$username@$hostname", "sudo mv kubernetes.list /etc/apt/sources.list.d/kubernetes.list")
  }
  %('ssh, s"$username@$hostname", "sudo apt-get update && sudo apt-get install -y kubelet kubeadm kubectl")
}