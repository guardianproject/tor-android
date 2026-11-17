#
# The provisioning is parsed from the .gitlab-ci.yml's "release" job.
#

require 'pathname'
require 'tempfile'
require 'yaml'

srvpath = Pathname.new(File.dirname(__FILE__)).realpath
configfile = YAML.load_file(File.join(srvpath, "/.gitlab-ci.yml"))
remote_url = 'https://github.com/guardianproject/tor-android.git'

# set up essential environment variables
env = Hash.new
env['CI_PROJECT_DIR'] = '/builds/guardianproject/tor-android'
env_file = Tempfile.new('env')
File.chmod(0644, env_file.path)
env.each do |k,v|
    env_file.write("export #{k}='#{v}'\n")
end
env_file.rewind

sourcepath = '/etc/profile.d/env.sh'
header = "#!/bin/bash -ex\nsource #{sourcepath}\ncd $CI_PROJECT_DIR\n"

script_file = Tempfile.new('script')
File.chmod(0755, script_file.path)
script_file.write(header)
configfile['release']['script'].flatten.each do |line|
    script_file.write(line)
    script_file.write("\n")
end
script_file.rewind

Vagrant.configure("2") do |config|
  config.vm.box = "fdroid/basebox-buster64"
  config.vm.synced_folder '.', '/vagrant', disabled: true
  config.vm.provision "file", source: env_file.path, destination: 'env.sh'
  config.vm.provision :shell, inline: <<-SHELL
    set -ex

    echo 'deb https://deb.debian.org/debian/ buster-updates main' >> /etc/apt/sources.list
    echo 'deb https://deb.debian.org/debian-security/ buster/updates main' >> /etc/apt/sources.list

    apt-get --allow-releaseinfo-change-suite update  # buster went from stable to oldstable
    apt-get -qy remove grub-pc  # updating grub requires human interaction

    mv ~vagrant/env.sh #{sourcepath}
    source #{sourcepath}
    mkdir -p $(dirname $CI_PROJECT_DIR)
    git clone #{remote_url} $CI_PROJECT_DIR
    chown -R vagrant.vagrant $(dirname $CI_PROJECT_DIR)
SHELL
  config.vm.provision "file", source: script_file.path, destination: 'script.sh'
#TODO?  config.vm.provision :shell, inline: 'sudo SUDO_FORCE_REMOVE=yes dpkg --purge sudo'
  config.vm.provision :shell, inline: '/home/vagrant/script.sh'

  # remove this or comment it out to use VirtualBox instead of libvirt
  config.vm.provider :libvirt do |libvirt|
    libvirt.memory = 1536
  end
end
