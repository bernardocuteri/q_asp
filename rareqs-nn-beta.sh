#!/bin/bash
#
# File:  run.sh
# Author:  mikolas
# Created on:  Tue May 27 16:42:13 WEST 2014
# Copyright (C) 2014, Mikolas Janota
#
if [[ $# -ne 1 ]]; then
    echo "Usage: $0 <instance>"
    exit 100;
fi
./fmla $1 -read-qcir -write-gq | ./qcir-conv.py - -prenex -write-gq | ./rareqs-nn -
