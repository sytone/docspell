{ config, pkgs, ... }:

# This module sets up solr with one core. It is a bit tedious…. If you
# know a better solution, please let me know.
{

  services.solr = {
    enable = true;
  };
  # This is needed to run solr script as user solr
  users.users.solr.useDefaultShell = true;

  systemd.services.solr-init =
    let
      solrPort = toString config.services.solr.port;
      initSolr = ''
        if [ ! -f ${config.services.solr.stateDir}/docspell_core ]; then
          while ! echo "" | ${pkgs.telnet}/bin/telnet localhost ${solrPort}
          do
             echo "Waiting for SOLR become ready..."
             sleep 1.5
          done
          ${pkgs.su}/bin/su -s ${pkgs.bash}/bin/sh solr -c "${pkgs.solr}/bin/solr create_core -c docspell -p ${solrPort}";
          touch ${config.services.solr.stateDir}/docspell_core
        fi
      '';
    in
      { script = initSolr;
        after = [ "solr.target" ];
        wantedBy = [ "multi-user.target" ];
        requires = [ "solr.target" ];
        description = "Create a core at solr";
      };

}
