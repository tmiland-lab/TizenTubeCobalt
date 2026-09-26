#!/usr/bin/python3
# Copyright 2026 Reis Can (reisxd). All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""Emits a C++ header string constant with the vendored userscript version.

Usage: generate_version_header.py <version-file> <output-header>
"""

import sys


def main():
  if len(sys.argv) != 3:
    print(f'usage: {sys.argv[0]} <version-file> <output-header>', file=sys.stderr)
    return 1

  with open(sys.argv[1], 'r', encoding='utf-8') as f:
    version = f.read().strip()

  with open(sys.argv[2], 'w', encoding='utf-8') as out:
    out.write('// Copyright 2026 Reis Can (reisxd). All Rights Reserved.\n')
    out.write('// This file is generated. Do not edit!\n')
    out.write('#ifndef COBALT_TIZENTUBE_EMBEDDED_RESOURCES_'
              'TIZENTUBE_VERSION_H_\n')
    out.write('#define COBALT_TIZENTUBE_EMBEDDED_RESOURCES_'
              'TIZENTUBE_VERSION_H_\n\n')
    out.write('namespace TizenTubeUserscript {\n')
    out.write(f'inline constexpr char kVersion[] = "{version}";\n')
    out.write('}  // namespace TizenTubeUserscript\n\n')
    out.write('#endif  // COBALT_TIZENTUBE_EMBEDDED_RESOURCES_'
              'TIZENTUBE_VERSION_H_\n')

  return 0


if __name__ == '__main__':
  sys.exit(main())
