"""Build each Minecraft 26.1.x variant and immediately check its resolved game classes."""
from pathlib import Path
import argparse,json,os,shutil,subprocess,sys,zipfile
here=Path(__file__).resolve().parent;repo=here.parent
matrix=json.loads((here/'versions.json').read_text('utf8'))
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--games',nargs='+',choices=matrix,default=list(matrix))
parser.add_argument('--legacy-model',type=Path,help='Optional original legacy .ysm for cache/animation allocation checks')
options=parser.parse_args()
gradle=str(repo/('gradlew.bat' if os.name=='nt' else 'gradlew'))
for game in options.games:
    properties={**matrix[game],'minecraft_version':game,'mod_version':f'2.6.6.0+{game}-alpha.1'}
    subprocess.run([gradle,':fabric:build','-I','verification/classpath.gradle',':fabric:writeVerificationClasspath',
        '--console=plain',*[f'-P{k}={v}' for k,v in properties.items()]],cwd=repo,check=True)
    check=[sys.executable,str(here/'run_smoke.py')]
    if options.legacy_model:check+=['--legacy-model',str(options.legacy_model.resolve())]
    subprocess.run(check,cwd=repo,check=True)
    jar=repo/f'fabric/build/libs/openysm-fabric-{properties["mod_version"]}.jar'
    with zipfile.ZipFile(jar) as archive:
        meta=json.loads(archive.read('fabric.mod.json'))
        if meta['depends']['minecraft']!=game:raise AssertionError('Wrong game constraint: '+game)
        for dependency in ('fabric-api','architectury','forgeconfigapiport','cardinal-components-base','cardinal-components-entity'):
            if dependency not in meta['depends']:raise AssertionError('Missing required runtime dependency: '+dependency)
    output=repo/'fabric/build/verified';output.mkdir(exist_ok=True)
    shutil.copyfile(jar,output/jar.name)
    print('BUILT AND CHECKED '+game+': '+str(output/jar.name),flush=True)
