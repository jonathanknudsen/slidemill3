Raspbian
command line dd to sd card

wpa_passphrase aelous-xii 'zazzles!' >> /etc/wpa_supplicant/wpa_supplicant.conf
edit /etc/network/interfaces, change manual to static, add address, netmask, gateway

sudo apt-get update
sudo apt-get install --no-install-recommends xserver-xorg
sudo apt-get install lxde-core lxappearance
sudo apt-get install -y lightdm

sudo raspi-config # select gui with auto-login

#sudo apt-get install lxrandr
uncomment overscan disable in /boot/config.txt

sudo apt-get install -y oracle-java8-jdk # openjdk-8-jre ?
