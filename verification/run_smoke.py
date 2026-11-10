"""Run focused checks against the compiled port and its Minecraft dependencies."""
from pathlib import Path
import argparse
import os
import shutil
import subprocess

here = Path(__file__).resolve().parent
repo = here.parent
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--classpath-file', type=Path,
                    default=repo / 'fabric/build/verification/classpath.txt')
parser.add_argument('--java-home', type=Path,
                    default=Path(os.environ['JAVA_HOME']) if os.environ.get('JAVA_HOME') else None)
parser.add_argument('--legacy-model', type=Path,
                    help='Optional legacy v1/v2 .ysm fixture for metadata-cache roundtrip checks')
options = parser.parse_args()
if not options.classpath_file.is_file():
    parser.error('Generate the classpath first using verification/classpath.gradle; see README.md')
classpath = options.classpath_file.read_text('utf8').strip()
output = repo / 'fabric/build/verification'
classes = output / 'classes'
classes.mkdir(parents=True, exist_ok=True)

def executable(name):
    if options.java_home:
        return str(options.java_home / 'bin' / (name + ('.exe' if os.name == 'nt' else '')))
    result = shutil.which(name)
    if not result:
        parser.error('Java 25 JDK is required: set JAVA_HOME or --java-home')
    return result

def argfile(name, arguments):
    target = output / name
    # Java argfiles require quoted paths; normalize Windows separators first.
    target.write_text('\n'.join('"' + str(a).replace('\\', '/').replace('"', '\\"') + '"'
                                for a in arguments), encoding='utf8')
    return '@' + str(target)

sources = [here / 'WireBoundsSmoke.java', here / 'RenderSmoke.java', here / 'OptimizationSmoke.java', here / 'ModelTransferSmoke.java', here / 'ModelCacheSmoke.java', here / 'ModelBackendSmoke.java', here / 'MixinTargetsSmoke.java']
if (here / 'LegacyInputSmoke.java').is_file(): sources.append(here / 'LegacyInputSmoke.java')
subprocess.run([executable('javac'), argfile('javac.args',
    ['-proc:none', '-cp', classpath, '-d', classes, *sources])], cwd=repo, check=True)
tests = [('WireBoundsSmoke', []), ('RenderSmoke', []), ('ModelTransferSmoke', []), ('ModelCacheSmoke', []), ('ModelBackendSmoke', []), ('MixinTargetsSmoke', [])]
if (here / 'LegacyInputSmoke.java').is_file(): tests.append(('LegacyInputSmoke', []))
if options.legacy_model:
    tests.append(('OptimizationSmoke', [output / 'cache-fixtures', options.legacy_model.resolve()]))
else:
    print('SKIP OptimizationSmoke: supply --legacy-model for the optional cache/numeric checks', flush=True)
for test, arguments in tests:
    print('RUN ' + test, flush=True)
    subprocess.run([executable('java'), argfile(test + '.args',
        ['--enable-native-access=ALL-UNNAMED', '--sun-misc-unsafe-memory-access=allow',
         '-Djava.awt.headless=true', '-Dstdout.encoding=UTF-8',
         '-cp', str(classes) + os.pathsep + classpath, test, *arguments])], cwd=repo, check=True)
