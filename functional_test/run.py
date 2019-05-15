#!/usr/bin/env python
import os
import sys

if (sys.version_info < (3, 0)):
  print("Tests must be run with python3. Exiting")
  sys.exit(1)

basedir = os.path.dirname(os.path.realpath(__file__))
vedir = os.path.join(basedir, '.virtualenv')
os.system('./bootstrap.sh')

activate_virtualenv = os.path.join(vedir, 'bin', 'activate_this.py')
print('Activating virtualenv at ' + activate_virtualenv)

report_dir = os.path.join(os.path.dirname(os.path.realpath(__file__)), '../target/pytest_reports')
if not os.path.exists(report_dir):
    os.system('mkdir -p ' + report_dir)

with open(activate_virtualenv, "r") as f:
    program = compile(f.read(), activate_virtualenv, 'exec')
    exec(program, dict(__file__=activate_virtualenv))

import pytest

result = 1

result = pytest.main(list(sys.argv[1:]))

sys.exit(result)


