#!/bin/sh
# Decrypt the secret_properties.enc file
export GPG_TTY=$(tty)
gpg --decrypt --passphrase="$SECRET_PROP_PWD" --output treetracker.keys.properties secret_properties.enc
